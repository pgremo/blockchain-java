package one.wangwei.blockchain.block;

import one.wangwei.blockchain.store.RocksDBUtils;
import one.wangwei.blockchain.transaction.TXInput;
import one.wangwei.blockchain.transaction.TXOutput;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.Bytes;

import java.security.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * <p> 区块链 </p>
 *
 * @author wangwei
 * @date 2018/02/02
 */
public class Blockchain implements Iterable<Block> {
    @SuppressWarnings("all")
    private static final Logger logger = Logger.getLogger(Blockchain.class.getName());
    private String lastBlockHash;

    /**
     * 从 DB 中恢复区块链数据
     *
     * @return
     */
    public static Blockchain initBlockchainFromDB() {
        var lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        if (lastBlockHash == null) {
            throw new RuntimeException("ERROR: Fail to init blockchain from db. ");
        }
        return new Blockchain(lastBlockHash);
    }

    /**
     * <p> 创建区块链 </p>
     *
     * @param address 钱包地址
     * @return
     */
    public static Blockchain createBlockchain(String address) {
        var lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        if (lastBlockHash.isBlank()) {
            // 创建 coinBase 交易
            var genesisCoinbaseData = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";
            var coinbaseTX = Transaction.newCoinbaseTX(address, genesisCoinbaseData);
            var genesisBlock = Block.newGenesisBlock(coinbaseTX).orElseThrow();
            lastBlockHash = genesisBlock.hash();
            RocksDBUtils.getInstance().putBlock(genesisBlock);
            RocksDBUtils.getInstance().putLastBlockHash(lastBlockHash);
        }
        return new Blockchain(lastBlockHash);
    }

    /**
     * 打包交易，进行挖矿
     *
     * @param transactions
     * @return
     */
    public Optional<Block> mineBlock(Transaction[] transactions) {
        // 挖矿前，先验证交易记录
        for (var tx : transactions) {
            if (!this.verifyTransactions(tx)) {
                logger.severe(() -> "ERROR: Fail to mine block ! Invalid transaction ! tx=%s".formatted(tx));
                throw new RuntimeException("ERROR: Fail to mine block ! Invalid transaction ! ");
            }
        }
        var lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        if (lastBlockHash == null) {
            throw new RuntimeException("ERROR: Fail to get last block hash ! ");
        }
        var block = Block.newBlock(lastBlockHash, transactions);
        block.ifPresent(this::addBlock);
        return block;
    }

    /**
     * <p> 添加区块  </p>
     *
     * @param block
     */
    private void addBlock(Block block) {
        System.out.println(block);
        RocksDBUtils.getInstance().putLastBlockHash(block.hash());
        RocksDBUtils.getInstance().putBlock(block);
        this.lastBlockHash = block.hash();
    }


    /**
     * 区块链迭代器
     */
    public static class BlockchainIterator implements Iterator<Block> {
        private String currentBlockHash;

        private BlockchainIterator(String currentBlockHash) {
            this.currentBlockHash = currentBlockHash;
        }

        /**
         * 是否有下一个区块
         *
         * @return
         */
        public boolean hasNext() {
            return !currentBlockHash.equals(Block.ZERO_HASH);
        }

        /**
         * 返回区块
         *
         * @return
         */
        public Block next() {
            var currentBlock = RocksDBUtils.getInstance().getBlock(currentBlockHash);
            if (currentBlock != null) {
                currentBlockHash = currentBlock.previousHash();
                return currentBlock;
            }
            throw new NoSuchElementException();
        }
    }

    /**
     * 获取区块链迭代器
     *
     * @return
     */
    public Iterator<Block> iterator() {
        return new BlockchainIterator(lastBlockHash);
    }

    /**
     * 查找所有的 unspent transaction outputs
     *
     * @return
     */
    public HashMap<String, List<TXOutput>> findAllUTXOs() {
        var allSpentTXOs = this.getAllSpentTXOs();
        var allUTXOs = new HashMap<String, List<TXOutput>>();
        // 再次遍历所有区块中的交易输出
        for (var block : this) {
            for (var transaction : block.transactions()) {
                var txId = Bytes.byteArrayToHex(transaction.getTxId());
                var spentOutIndexArray = allSpentTXOs.get(txId);
                var txOutputs = transaction.getOutputs();
                for (var outIndex = 0; outIndex < txOutputs.length; outIndex++) {
                    if (spentOutIndexArray != null && spentOutIndexArray.contains(outIndex)) {
                        continue;
                    }
                    allUTXOs
                            .computeIfAbsent(txId, x -> new LinkedList<>())
                            .add(txOutputs[outIndex]);
                }
            }
        }
        return allUTXOs;
    }

    /**
     * 从交易输入中查询区块链中所有已被花费了的交易输出
     *
     * @return 交易ID以及对应的交易输出下标地址
     */
    private Map<String, List<Integer>> getAllSpentTXOs() {
        // 定义TxId ——> spentOutIndex[]，存储交易ID与已被花费的交易输出数组索引值
        var spentTXOs = new HashMap<String, List<Integer>>();
        for (var block : this) {
            for (var transaction : block.transactions()) {
                // 如果是 coinbase 交易，直接跳过，因为它不存在引用前一个区块的交易输出
                if (transaction.isCoinbase()) {
                    continue;
                }
                for (var txInput : transaction.getInputs()) {
                    var inTxId = Bytes.byteArrayToHex(txInput.getTxId());
                    var spentOutIndexArray = spentTXOs.computeIfAbsent(inTxId, x -> new LinkedList<>());
                    spentOutIndexArray.add(txInput.getTxOutputIndex());
                }
            }
        }
        return spentTXOs;
    }

    /**
     * 依据交易ID查询交易信息
     *
     * @param txId 交易ID
     * @return
     */
    private Optional<Transaction> findTransaction(byte[] txId) {
        return StreamSupport.stream(this.spliterator(), false)
                .flatMap(x -> Arrays.stream(x.transactions()))
                .filter(x -> Arrays.equals(x.getTxId(), txId))
                .findFirst();
    }

    /**
     * 进行交易签名
     *
     * @param tx         交易数据
     * @param privateKey 私钥
     */
    public void signTransaction(Transaction tx, PrivateKey privateKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException {
        var prevTx = Arrays.stream(tx.getInputs())
                .map(TXInput::getTxId)
                .collect(toMap(identity(), this::findTransaction));
        tx.sign(privateKey, prevTx);
    }

    /**
     * 交易签名验证
     *
     * @param tx
     */
    public boolean verifyTransactions(Transaction tx) {
        if (tx.isCoinbase()) return true;
        var prevTx = Arrays.stream(tx.getInputs())
                .map(TXInput::getTxId)
                .collect(toMap(identity(), this::findTransaction));
        return tx.verify(prevTx);
    }

    public String getLastBlockHash() {
        return this.lastBlockHash;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof final Blockchain other)) return false;
        if (!other.canEqual(this)) return false;
        final Object this$lastBlockHash = this.getLastBlockHash();
        final Object other$lastBlockHash = other.getLastBlockHash();
        return Objects.equals(this$lastBlockHash, other$lastBlockHash);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Blockchain;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $lastBlockHash = this.getLastBlockHash();
        result = result * PRIME + ($lastBlockHash == null ? 43 : $lastBlockHash.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Blockchain(lastBlockHash=" + this.getLastBlockHash() + ")";
    }

    public Blockchain(final String lastBlockHash) {
        this.lastBlockHash = lastBlockHash;
    }
}
