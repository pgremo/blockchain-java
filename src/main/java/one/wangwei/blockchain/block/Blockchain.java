package one.wangwei.blockchain.block;

import one.wangwei.blockchain.store.RocksDBUtils;
import one.wangwei.blockchain.transaction.TXOutput;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.Bytes;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import java.util.*;

/**
 * <p> 区块链 </p>
 *
 * @author wangwei
 * @date 2018/02/02
 */
public class Blockchain implements Iterable<Block> {
    @SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Blockchain.class);
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
            lastBlockHash = genesisBlock.getHash();
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
                log.error("ERROR: Fail to mine block ! Invalid transaction ! tx=" + tx);
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
        RocksDBUtils.getInstance().putLastBlockHash(block.getHash());
        RocksDBUtils.getInstance().putBlock(block);
        this.lastBlockHash = block.getHash();
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
            return !currentBlockHash.equals(Bytes.ZERO_HASH);
        }

        /**
         * 返回区块
         *
         * @return
         */
        public Block next() {
            var currentBlock = RocksDBUtils.getInstance().getBlock(currentBlockHash);
            if (currentBlock != null) {
                currentBlockHash = currentBlock.getPrevBlockHash();
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
            for (var transaction : block.getTransactions()) {
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
            for (var transaction : block.getTransactions()) {
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
    private Transaction findTransaction(byte[] txId) {
        for (var block : this) {
            for (var tx : block.getTransactions()) {
                if (Arrays.equals(tx.getTxId(), txId)) {
                    return tx;
                }
            }
        }
        throw new RuntimeException("ERROR: Can not found tx by txId ! ");
    }

    /**
     * 进行交易签名
     *
     * @param tx         交易数据
     * @param privateKey 私钥
     */
    public void signTransaction(Transaction tx, BCECPrivateKey privateKey) throws Exception {
        // 先来找到这笔新的交易中，交易输入所引用的前面的多笔交易的数据
        var prevTxMap = new HashMap<String, Transaction>();
        for (var txInput : tx.getInputs()) {
            var prevTx = this.findTransaction(txInput.getTxId());
            prevTxMap.put(Bytes.byteArrayToHex(txInput.getTxId()), prevTx);
        }
        tx.sign(privateKey, prevTxMap);
    }

    /**
     * 交易签名验证
     *
     * @param tx
     */
    private boolean verifyTransactions(Transaction tx) {
        if (tx.isCoinbase()) {
            return true;
        }
        var prevTx = new HashMap<String, Transaction>();
        for (var txInput : tx.getInputs()) {
            var transaction = this.findTransaction(txInput.getTxId());
            prevTx.put(Bytes.byteArrayToHex(txInput.getTxId()), transaction);
        }
        try {
            return tx.verify(prevTx);
        } catch (Exception e) {
            log.error("Fail to verify transaction ! transaction invalid ! ", e);
            throw new RuntimeException("Fail to verify transaction ! transaction invalid ! ", e);
        }
    }

    @SuppressWarnings("all")
    public String getLastBlockHash() {
        return this.lastBlockHash;
    }

    @SuppressWarnings("all")
    public void setLastBlockHash(final String lastBlockHash) {
        this.lastBlockHash = lastBlockHash;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Blockchain)) return false;
        final Blockchain other = (Blockchain) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$lastBlockHash = this.getLastBlockHash();
        final Object other$lastBlockHash = other.getLastBlockHash();
        if (this$lastBlockHash == null ? other$lastBlockHash != null : !this$lastBlockHash.equals(other$lastBlockHash)) return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof Blockchain;
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $lastBlockHash = this.getLastBlockHash();
        result = result * PRIME + ($lastBlockHash == null ? 43 : $lastBlockHash.hashCode());
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "Blockchain(lastBlockHash=" + this.getLastBlockHash() + ")";
    }

    @SuppressWarnings("all")
    public Blockchain(final String lastBlockHash) {
        this.lastBlockHash = lastBlockHash;
    }

    @SuppressWarnings("all")
    public Blockchain() {
    }
}
