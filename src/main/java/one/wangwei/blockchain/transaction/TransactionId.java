package one.wangwei.blockchain.transaction;

import one.wangwei.blockchain.util.Bytes;

import java.util.Arrays;

public record TransactionId(byte[] value) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionId that = (TransactionId) o;
        return Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return Bytes.byteArrayToHex(value);
    }
}
