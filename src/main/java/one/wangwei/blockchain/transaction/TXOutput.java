package one.wangwei.blockchain.transaction;

import one.wangwei.blockchain.util.Base58Check;
import one.wangwei.blockchain.util.Bytes;
import one.wangwei.blockchain.util.Hashes;
import one.wangwei.blockchain.util.Numbers;

import java.util.Arrays;

/**
 * 交易输出
 *
 * @author wangwei
 * @date 2017/03/04
 */
public record TXOutput(int value, byte[] pubKeyHash) {

    public byte[] hash() {
        return Hashes.sha256(Numbers.toBytes(value), pubKeyHash);
    }

    /**
     * 创建交易输出
     *
     * @param value
     * @param address
     * @return
     */
    public static TXOutput newTXOutput(int value, String address) {
        // 反向转化为 byte 数组
        var versionedPayload = Base58Check.base58ToBytes(address);
        var pubKeyHash = Arrays.copyOfRange(versionedPayload, 1, versionedPayload.length);
        return new TXOutput(value, pubKeyHash);
    }

    /**
     * 检查交易输出是否能够使用指定的公钥
     *
     * @param target
     * @return
     */
    public boolean isLockedWithKey(byte[] target) {
        return Arrays.equals(pubKeyHash, target);
    }

    @Override
    public String toString() {
        return "TXOutput[" +
                "value=" + value +
                ", pubKeyHash=" + Bytes.byteArrayToHex(pubKeyHash) +
                ']';
    }
}
