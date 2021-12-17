package one.wangwei.blockchain.block;

import one.wangwei.blockchain.util.Bytes;

import java.util.Arrays;

public record BlockId(byte[] value) {
    @Override
    public String toString() {
        return "BlockId[" +
                "value=" + Bytes.byteArrayToHex(value) +
                ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockId blockId = (BlockId) o;
        return Arrays.equals(value, blockId.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
