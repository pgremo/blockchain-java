package one.wangwei.blockchain.transaction;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.store.RocksDBUtils;
import one.wangwei.blockchain.util.Bytes;
import one.wangwei.blockchain.util.SerializeUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 未被花费的交易输出池
 *
 * @author wangwei
 * @date 2018/03/31
 */
public class UTXOSet {
    @SuppressWarnings("all")
    private static final Logger logger = Logger.getLogger(UTXOSet.class.getName());
    @SuppressWarnings("all")
    private final Object $lock = new Object[0];
    private Blockchain blockchain;

    /**
     * 寻找能够花费的交易
     *
     * @param pubKeyHash 钱包公钥Hash
     * @param amount     花费金额
     */
    public SpendableOutputResult findSpendableOutputs(byte[] pubKeyHash, int amount) {
        var unspentOuts = new HashMap<String, List<Integer>>();
        var accumulated = 0;
        var chainstateBucket = RocksDBUtils.getInstance().getChainstateBucket();
        for (var entry : chainstateBucket.entrySet()) {
            var txId = entry.getKey();
            var txOutputs = (TXOutput[]) SerializeUtils.deserialize(entry.getValue());
            for (var outId = 0; outId < txOutputs.length; outId++) {
                var txOutput = txOutputs[outId];
                if (txOutput.isLockedWithKey(pubKeyHash) && accumulated < amount) {
                    accumulated += txOutput.getValue();
                    var outIds = unspentOuts.computeIfAbsent(txId, x -> new LinkedList<>());
                    outIds.add(outId);
                    if (accumulated >= amount) {
                        break;
                    }
                }
            }
        }
        return new SpendableOutputResult(accumulated, unspentOuts);
    }

    /**
     * 查找钱包地址对应的所有UTXO
     *
     * @param pubKeyHash 钱包公钥Hash
     * @return
     */
    public List<TXOutput> findUTXOs(byte[] pubKeyHash) {
        var utxos = new LinkedList<TXOutput>();
        var chainstateBucket = RocksDBUtils.getInstance().getChainstateBucket();
        if (chainstateBucket.isEmpty()) {
            return utxos;
        }
        for (var value : chainstateBucket.values()) {
            var txOutputs = (TXOutput[]) SerializeUtils.deserialize(value);
            for (var txOutput : txOutputs) {
                if (txOutput.isLockedWithKey(pubKeyHash)) {
                    utxos.add(txOutput);
                }
            }
        }
        return utxos;
    }

    /**
     * 重建 UTXO 池索引
     */
    public void reIndex() {
        synchronized (this.$lock) {
            logger.info("Start to reIndex UTXO set !");
            RocksDBUtils.getInstance().cleanChainStateBucket();
            var allUTXOs = blockchain.findAllUTXOs();
            for (var entry : allUTXOs.entrySet()) {
                RocksDBUtils.getInstance().putUTXOs(entry.getKey(), entry.getValue().toArray(TXOutput[]::new));
            }
            logger.info("ReIndex UTXO set finished ! ");
        }
    }

    /**
     * 更新UTXO池
     * <p>
     * 当一个新的区块产生时，需要去做两件事情：
     * 1）从UTXO池中移除花费掉了的交易输出；
     * 2）保存新的未花费交易输出；
     *
     * @param tipBlock 最新的区块
     */
    public void update(Block tipBlock) {
        synchronized (this.$lock) {
            if (tipBlock == null) {
                logger.severe("Fail to update UTXO set ! tipBlock is null !");
                throw new RuntimeException("Fail to update UTXO set ! ");
            }
            for (var transaction : tipBlock.getTransactions()) {
                // 根据交易输入排查出剩余未被使用的交易输出
                if (!transaction.isCoinbase()) {
                    for (var txInput : transaction.getInputs()) {
                        // 余下未被使用的交易输出
                        var remainderUTXOs = new LinkedList<TXOutput>();
                        var txId = Bytes.byteArrayToHex(txInput.getTxId());
                        var txOutputs = RocksDBUtils.getInstance().getUTXOs(txId);
                        if (txOutputs == null) {
                            continue;
                        }
                        for (var outIndex = 0; outIndex < txOutputs.length; outIndex++) {
                            if (outIndex != txInput.getTxOutputIndex()) {
                                remainderUTXOs.add(txOutputs[outIndex]);
                            }
                        }
                        // 没有剩余则删除，否则更新
                        if (remainderUTXOs.isEmpty()) {
                            RocksDBUtils.getInstance().deleteUTXOs(txId);
                        } else {
                            RocksDBUtils.getInstance().putUTXOs(txId, remainderUTXOs.toArray(TXOutput[]::new));
                        }
                    }
                }
                // 新的交易输出保存到DB中
                var txOutputs = transaction.getOutputs();
                var txId = Bytes.byteArrayToHex(transaction.getTxId());
                RocksDBUtils.getInstance().putUTXOs(txId, txOutputs);
            }
        }
    }

    @SuppressWarnings("all")
    public UTXOSet() {
    }

    @SuppressWarnings("all")
    public UTXOSet(final Blockchain blockchain) {
        this.blockchain = blockchain;
    }
}
