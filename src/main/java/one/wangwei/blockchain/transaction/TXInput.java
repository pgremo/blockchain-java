package one.wangwei.blockchain.transaction;

import one.wangwei.blockchain.util.BtcAddressUtils;
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
    private byte[] txId;
    /**
     * 交易输出索引
     */
    private int txOutputIndex;
    /**
     * 签名
     */
    private byte[] signature;
    /**
     * 公钥
     */
    private byte[] pubKey;

    /**
     * 检查公钥hash是否用于交易输入
     *
     * @param pubKeyHash
     * @return
     */
    public boolean usesKey(byte[] pubKeyHash) {
        var lockingHash = BtcAddressUtils.ripeMD160Hash(this.getPubKey());
        return Arrays.equals(lockingHash, pubKeyHash);
    }

    /**
     * 交易Id的hash值
     */
    @SuppressWarnings("all")
    public byte[] getTxId() {
        return this.txId;
    }

    /**
     * 交易输出索引
     */
    @SuppressWarnings("all")
    public int getTxOutputIndex() {
        return this.txOutputIndex;
    }

    /**
     * 签名
     */
    @SuppressWarnings("all")
    public byte[] getSignature() {
        return this.signature;
    }

    /**
     * 公钥
     */
    @SuppressWarnings("all")
    public byte[] getPubKey() {
        return this.pubKey;
    }

    /**
     * 交易Id的hash值
     */
    @SuppressWarnings("all")
    public void setTxId(final byte[] txId) {
        this.txId = txId;
    }

    /**
     * 交易输出索引
     */
    @SuppressWarnings("all")
    public void setTxOutputIndex(final int txOutputIndex) {
        this.txOutputIndex = txOutputIndex;
    }

    /**
     * 签名
     */
    @SuppressWarnings("all")
    public void setSignature(final byte[] signature) {
        this.signature = signature;
    }

    /**
     * 公钥
     */
    @SuppressWarnings("all")
    public void setPubKey(final byte[] pubKey) {
        this.pubKey = pubKey;
    }

    @Override
    @SuppressWarnings("all")
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

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof TXInput;
    }

    @Override
    @SuppressWarnings("all")
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
    @SuppressWarnings("all")
    public String toString() {
        return "TXInput(txId=" + Arrays.toString(this.getTxId()) + ", txOutputIndex=" + this.getTxOutputIndex() + ", signature=" + Arrays.toString(this.getSignature()) + ", pubKey=" + Arrays.toString(this.getPubKey()) + ")";
    }

    @SuppressWarnings("all")
    public TXInput(final byte[] txId, final int txOutputIndex, final byte[] signature, final byte[] pubKey) {
        this.txId = txId;
        this.txOutputIndex = txOutputIndex;
        this.signature = signature;
        this.pubKey = pubKey;
    }

    @SuppressWarnings("all")
    public TXInput() {
    }
}
