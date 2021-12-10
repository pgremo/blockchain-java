package one.wangwei.blockchain.transaction;

import one.wangwei.blockchain.util.Bytes;
import one.wangwei.blockchain.util.Hashes;
import one.wangwei.blockchain.util.Numbers;

import java.util.Arrays;

/**
 * 交易输入
 *
 * @author wangwei
 * @date 2017/03/04
 */
public class TXInput {
    /**
     * 交易Id的hash值
     */
    private final byte[] txId;
    /**
     * 交易输出索引
     */
    private final int txOutputIndex;
    /**
     * 签名
     */
    private byte[] signature;
    /**
     * 公钥
     */
    private byte[] pubKey;

    /**
     * 交易Id的hash值
     */
    public byte[] getTxId() {
        return this.txId;
    }

    /**
     * 交易输出索引
     */
    public int getTxOutputIndex() {
        return this.txOutputIndex;
    }

    /**
     * 签名
     */
    public byte[] getSignature() {
        return this.signature;
    }

    /**
     * 公钥
     */
    public byte[] getPubKey() {
        return this.pubKey;
    }

    /**
     * 签名
     */
    public void setSignature(final byte[] signature) {
        this.signature = signature;
    }

    /**
     * 公钥
     */
    public void setPubKey(final byte[] pubKey) {
        this.pubKey = pubKey;
    }

    public byte[] hash() {
        return Hashes.sha256(txId, Numbers.toBytes(txOutputIndex), signature, pubKey);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof TXInput)) return false;
        final TXInput other = (TXInput) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getTxOutputIndex() != other.getTxOutputIndex()) return false;
        if (!Arrays.equals(this.getTxId(), other.getTxId())) return false;
        if (!Arrays.equals(this.getSignature(), other.getSignature())) return false;
        if (!Arrays.equals(this.getPubKey(), other.getPubKey())) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof TXInput;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getTxOutputIndex();
        result = result * PRIME + Arrays.hashCode(this.getTxId());
        result = result * PRIME + Arrays.hashCode(this.getSignature());
        result = result * PRIME + Arrays.hashCode(this.getPubKey());
        return result;
    }

    @Override
    public String toString() {
        return "TXInput[txId=" + Arrays.toString(this.getTxId()) + ", txOutputIndex=" + this.getTxOutputIndex() + ", signature=" + Arrays.toString(this.getSignature()) + ", pubKey=" + Bytes.byteArrayToHex(this.getPubKey()) + "]";
    }

    public TXInput(final byte[] txId, final int txOutputIndex, final byte[] signature, final byte[] pubKey) {
        this.txId = txId;
        this.txOutputIndex = txOutputIndex;
        this.signature = signature;
        this.pubKey = pubKey;
    }
}
