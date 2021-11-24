package one.wangwei.blockchain.block;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.wangwei.blockchain.store.RocksDBUtils;
import one.wangwei.blockchain.transaction.TXOutput;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.ByteUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * <p> 区块链 </p>
 *
 * @author wangwei
 * @date 2018/02/02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class Blockchain implements Iterable<Block> {

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
        if (StringUtils.isBlank(lastBlockHash)) {
            // 创建 coinBase 交易
            var genesisCoinbaseData = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";
            var coinbaseTX = Transaction.newCoinbaseTX(address, genesisCoinbaseData);
            var genesisBlock = Block.newGenesisBlock(coinbaseTX);
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
     */
    public Block mineBlock(Transaction[] transactions) {
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
        this.addBlock(block);
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
            return !currentBlockHash.equals(ByteUtils.ZERO_HASH);
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
    public Map<String, TXOutput[]> findAllUTXOs() {
        var allSpentTXOs = this.getAllSpentTXOs();
        var allUTXOs = Maps.<String, TXOutput[]>newHashMap();
        // 再次遍历所有区块中的交易输出
        for (var block: this) {
            for (var transaction : block.getTransactions()) {

                var txId = Hex.encodeHexString(transaction.getTxId());

                var spentOutIndexArray = allSpentTXOs.get(txId);
                var txOutputs = transaction.getOutputs();
                for (var outIndex = 0; outIndex < txOutputs.length; outIndex++) {
                    if (spentOutIndexArray != null && ArrayUtils.contains(spentOutIndexArray, outIndex)) {
                        continue;
                    }
                    var UTXOArray = allUTXOs.get(txId);
                    if (UTXOArray == null) {
                        UTXOArray = new TXOutput[]{txOutputs[outIndex]};
                    } else {
                        UTXOArray = ArrayUtils.add(UTXOArray, txOutputs[outIndex]);
                    }
                    allUTXOs.put(txId, UTXOArray);
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
    private Map<String, int[]> getAllSpentTXOs() {
        // 定义TxId ——> spentOutIndex[]，存储交易ID与已被花费的交易输出数组索引值
        var spentTXOs = Maps.<String, int[]>newHashMap();
        for (var block: this) {

            for (var transaction : block.getTransactions()) {
                // 如果是 coinbase 交易，直接跳过，因为它不存在引用前一个区块的交易输出
                if (transaction.isCoinbase()) {
                    continue;
                }
                for (var txInput : transaction.getInputs()) {
                    var inTxId = Hex.encodeHexString(txInput.getTxId());
                    var spentOutIndexArray = spentTXOs.get(inTxId);
                    if (spentOutIndexArray == null) {
                        spentOutIndexArray = new int[]{txInput.getTxOutputIndex()};
                    } else {
                        spentOutIndexArray = ArrayUtils.add(spentOutIndexArray, txInput.getTxOutputIndex());
                    }
                    spentTXOs.put(inTxId, spentOutIndexArray);
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
        for (var block: this) {
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
        var prevTxMap = Maps.<String, Transaction>newHashMap();
        for (var txInput : tx.getInputs()) {
            var prevTx = this.findTransaction(txInput.getTxId());
            prevTxMap.put(Hex.encodeHexString(txInput.getTxId()), prevTx);
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
        var prevTx = Maps.<String, Transaction>newHashMap();
        for (var txInput : tx.getInputs()) {
            var transaction = this.findTransaction(txInput.getTxId());
            prevTx.put(Hex.encodeHexString(txInput.getTxId()), transaction);
        }
        try {
            return tx.verify(prevTx);
        } catch (Exception e) {
            log.error("Fail to verify transaction ! transaction invalid ! ", e);
            throw new RuntimeException("Fail to verify transaction ! transaction invalid ! ", e);
        }
    }
}