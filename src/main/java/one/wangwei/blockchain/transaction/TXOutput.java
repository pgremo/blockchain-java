package one.wangwei.blockchain.transaction;

import one.wangwei.blockchain.util.Base58Check;
import java.util.Arrays;

/**
 * 交易输出
 *
 * @author wangwei
 * @date 2017/03/04
 */
public class TXOutput {
    /**
     * 数值
     */
    private int value;
    /**
     * 公钥Hash
     */
    private byte[] pubKeyHash;

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
     * @param pubKeyHash
     * @return
     */
    public boolean isLockedWithKey(byte[] pubKeyHash) {
        return Arrays.equals(this.getPubKeyHash(), pubKeyHash);
    }

    /**
     * 数值
     */
    @SuppressWarnings("all")
    public int getValue() {
        return this.value;
    }

    /**
     * 公钥Hash
     */
    @SuppressWarnings("all")
    public byte[] getPubKeyHash() {
        return this.pubKeyHash;
    }

    /**
     * 数值
     */
    @SuppressWarnings("all")
    public void setValue(final int value) {
        this.value = value;
    }

    /**
     * 公钥Hash
     */
    @SuppressWarnings("all")
    public void setPubKeyHash(final byte[] pubKeyHash) {
        this.pubKeyHash = pubKeyHash;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof TXOutput)) return false;
        final TXOutput other = (TXOutput) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getValue() != other.getValue()) return false;
        if (!Arrays.equals(this.getPubKeyHash(), other.getPubKeyHash())) return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof TXOutput;
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getValue();
        result = result * PRIME + Arrays.hashCode(this.getPubKeyHash());
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "TXOutput(value=" + this.getValue() + ", pubKeyHash=" + Arrays.toString(this.getPubKeyHash()) + ")";
    }

    @SuppressWarnings("all")
    public TXOutput(final int value, final byte[] pubKeyHash) {
        this.value = value;
        this.pubKeyHash = pubKeyHash;
    }

    @SuppressWarnings("all")
    public TXOutput() {
    }
}
