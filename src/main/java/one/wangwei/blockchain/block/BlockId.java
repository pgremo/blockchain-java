package one.wangwei.blockchain.block;

import java.util.Arrays;
import java.util.HexFormat;

public record BlockId(byte[] value) {
    public static final BlockId Null = new BlockId(new byte[32]);

    @Override
    public String toString() {
        return HexFormat.of().formatHex(value);
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
