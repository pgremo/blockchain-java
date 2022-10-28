package one.wangwei.blockchain.transaction;

import one.wangwei.blockchain.util.Hashes;
import one.wangwei.blockchain.util.Numbers;

import java.util.Arrays;
import java.util.HexFormat;

public class Input {
    private final Transaction.Id txId;
    private final int outputIndex;
    private byte[] signature;
    private byte[] pubKey;

    public Transaction.Id getTxId() {
        return txId;
    }

    public int getOutputIndex() {
        return outputIndex;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] getPubKey() {
        return pubKey;
    }

    public void setSignature(final byte[] signature) {
        this.signature = signature;
    }

    public void setPubKey(final byte[] pubKey) {
        this.pubKey = pubKey;
    }

    public byte[] hash() {
        return Hashes.sha256(txId.value(), Numbers.toBytes(outputIndex), signature, pubKey);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof final Input other)) return false;
        if (!other.canEqual(this)) return false;
        if (this.getOutputIndex() != other.getOutputIndex()) return false;
        if (!this.getTxId().equals(other.getTxId())) return false;
        if (!Arrays.equals(this.getSignature(), other.getSignature())) return false;
        return Arrays.equals(this.getPubKey(), other.getPubKey());
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Input;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + getOutputIndex();
        result = result * PRIME + getTxId().hashCode();
        result = result * PRIME + Arrays.hashCode(getSignature());
        result = result * PRIME + Arrays.hashCode(getPubKey());
        return result;
    }

    @Override
    public String toString() {
        return "TXInput[txId=%s, txOutputIndex=%d, signature=%s, pubKey=%s]".formatted(
                txId,
                outputIndex,
                signature == null ? "" : HexFormat.of().formatHex(signature),
                pubKey == null ? "" : HexFormat.of().formatHex(pubKey)
        );
    }

    public Input(final Transaction.Id txId, final int outputIndex, final byte[] signature, final byte[] pubKey) {
        this.txId = txId;
        this.outputIndex = outputIndex;
        this.signature = signature;
        this.pubKey = pubKey;
    }
}
