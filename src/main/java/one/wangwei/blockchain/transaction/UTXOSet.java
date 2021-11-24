package one.wangwei.blockchain.transaction;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.store.RocksDBUtils;
import one.wangwei.blockchain.util.SerializeUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Map;

/**
 * 未被花费的交易输出池
 *
 * @author wangwei
 * @date 2018/03/31
 */
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class UTXOSet {

    private Blockchain blockchain;

    /**
     * 寻找能够花费的交易
     *
     * @param pubKeyHash 钱包公钥Hash
     * @param amount     花费金额
     */
    public SpendableOutputResult findSpendableOutputs(byte[] pubKeyHash, int amount) {
        var unspentOuts = Maps.<String, int[]>newHashMap();
        var accumulated = 0;
        var chainstateBucket = RocksDBUtils.getInstance().getChainstateBucket();
        for (var entry : chainstateBucket.entrySet()) {
            var txId = entry.getKey();
            var txOutputs = (TXOutput[]) SerializeUtils.deserialize(entry.getValue());

            for (var outId = 0; outId < txOutputs.length; outId++) {
                var txOutput = txOutputs[outId];
                if (txOutput.isLockedWithKey(pubKeyHash) && accumulated < amount) {
                    accumulated += txOutput.getValue();

                    var outIds = unspentOuts.get(txId);
                    if (outIds == null) {
                        outIds = new int[]{outId};
                    } else {
                        outIds = ArrayUtils.add(outIds, outId);
                    }
                    unspentOuts.put(txId, outIds);
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
    public TXOutput[] findUTXOs(byte[] pubKeyHash) {
        var utxos = new TXOutput[0];
        var chainstateBucket = RocksDBUtils.getInstance().getChainstateBucket();
        if (chainstateBucket.isEmpty()) {
            return utxos;
        }
        for (var value : chainstateBucket.values()) {
            var txOutputs = (TXOutput[]) SerializeUtils.deserialize(value);
            for (var txOutput : txOutputs) {
                if (txOutput.isLockedWithKey(pubKeyHash)) {
                    utxos = ArrayUtils.add(utxos, txOutput);
                }
            }
        }
        return utxos;
    }


    /**
     * 重建 UTXO 池索引
     */
    @Synchronized
    public void reIndex() {
        log.info("Start to reIndex UTXO set !");
        RocksDBUtils.getInstance().cleanChainStateBucket();
        var allUTXOs = blockchain.findAllUTXOs();
        for (var entry : allUTXOs.entrySet()) {
            RocksDBUtils.getInstance().putUTXOs(entry.getKey(), entry.getValue());
        }
        log.info("ReIndex UTXO set finished ! ");
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
    @Synchronized
    public void update(Block tipBlock) {
        if (tipBlock == null) {
            log.error("Fail to update UTXO set ! tipBlock is null !");
            throw new RuntimeException("Fail to update UTXO set ! ");
        }
        for (var transaction : tipBlock.getTransactions()) {

            // 根据交易输入排查出剩余未被使用的交易输出
            if (!transaction.isCoinbase()) {
                for (var txInput : transaction.getInputs()) {
                    // 余下未被使用的交易输出
                    var remainderUTXOs = new TXOutput[0];
                    var txId = Hex.encodeHexString(txInput.getTxId());
                    var txOutputs = RocksDBUtils.getInstance().getUTXOs(txId);

                    if (txOutputs == null) {
                        continue;
                    }

                    for (var outIndex = 0; outIndex < txOutputs.length; outIndex++) {
                        if (outIndex != txInput.getTxOutputIndex()) {
                            remainderUTXOs = ArrayUtils.add(remainderUTXOs, txOutputs[outIndex]);
                        }
                    }

                    // 没有剩余则删除，否则更新
                    if (remainderUTXOs.length == 0) {
                        RocksDBUtils.getInstance().deleteUTXOs(txId);
                    } else {
                        RocksDBUtils.getInstance().putUTXOs(txId, remainderUTXOs);
                    }
                }
            }

            // 新的交易输出保存到DB中
            var txOutputs = transaction.getOutputs();
            var txId = Hex.encodeHexString(transaction.getTxId());
            RocksDBUtils.getInstance().putUTXOs(txId, txOutputs);
        }

    }


}
