package one.wangwei.blockchain.transaction;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.store.RocksDBUtils;
import one.wangwei.blockchain.util.Bytes;
import one.wangwei.blockchain.util.SerializeUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

/**
 * 未被花费的交易输出池
 *
 * @author wangwei
 * @date 2018/03/31
 */
public class UTXOSet {
    private static final Logger logger = Logger.getLogger(UTXOSet.class.getName());
    private final Object $lock = new Object[0];
    private final Blockchain blockchain;

    /**
     * 寻找能够花费的交易
     *
     * @param pubKeyHash 钱包公钥Hash
     * @param amount     花费金额
     */
    public SpendableOutputResult findSpendableOutputs(byte[] pubKeyHash, int amount) {
        var unspentOuts = new HashMap<byte[], List<Integer>>();
        var accumulated = 0;
        var chainstateBucket = RocksDBUtils.getInstance().getChainstateBucket();
        for (var entry : chainstateBucket.entrySet()) {
            var txId = entry.getKey();
            var txOutputs = (TXOutput[]) SerializeUtils.deserialize(entry.getValue());
            for (var outId = 0; outId < txOutputs.length; outId++) {
                var txOutput = txOutputs[outId];
                if (txOutput.isLockedWithKey(pubKeyHash) && accumulated < amount) {
                    accumulated += txOutput.value();
                    unspentOuts.computeIfAbsent(txId, x -> new LinkedList<>()).add(outId);

                    if (accumulated >= amount) break;
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
        return RocksDBUtils.getInstance().getChainstateBucket().values().stream()
                .flatMap(x -> Arrays.stream((TXOutput[])SerializeUtils.deserialize(x)))
                .filter(x -> x.isLockedWithKey(pubKeyHash))
                .collect(toList());
    }

    /**
     * 重建 UTXO 池索引
     */
    public void reIndex() {
        synchronized (this.$lock) {
            logger.info("Start to reIndex UTXO set !");
            RocksDBUtils.getInstance().cleanChainStateBucket();
            blockchain.findAllUTXOs().forEach((key, value) -> {
                Bytes.hexToByteArray(key).ifPresent(x -> {
                    RocksDBUtils.getInstance().putUTXOs(x, value.toArray(TXOutput[]::new));
                });
            });
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
            for (var transaction : tipBlock.transactions()) {
                // 根据交易输入排查出剩余未被使用的交易输出
                if (transaction.isCoinbase()) continue;

                for (var txInput : transaction.getInputs()) {
                    // 余下未被使用的交易输出
                    var remainderUTXOs = new LinkedList<TXOutput>();
                    var txOutputs = RocksDBUtils.getInstance().getUTXOs(txInput.getTxId());

                    if (txOutputs == null) continue;

                    for (var outIndex = 0; outIndex < txOutputs.length; outIndex++) {
                        if (outIndex != txInput.getTxOutputIndex()) {
                            remainderUTXOs.add(txOutputs[outIndex]);
                        }
                    }
                    // 没有剩余则删除，否则更新
                    if (remainderUTXOs.isEmpty()) {
                        RocksDBUtils.getInstance().deleteUTXOs(txInput.getTxId());
                    } else {
                        RocksDBUtils.getInstance().putUTXOs(txInput.getTxId(), remainderUTXOs.toArray(TXOutput[]::new));
                    }
                }
                // 新的交易输出保存到DB中
                var txOutputs = transaction.getOutputs();
                RocksDBUtils.getInstance().putUTXOs(transaction.getTxId(), txOutputs);
            }
        }
    }

    public UTXOSet(final Blockchain blockchain) {
        this.blockchain = blockchain;
    }
}
