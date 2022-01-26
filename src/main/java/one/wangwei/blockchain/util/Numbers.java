package one.wangwei.blockchain.util;

import java.nio.ByteBuffer;

public final class Numbers {

    /**
     * long 类型转 byte[]
     *
     * @param val
     * @return
     */
    public static byte[] toBytes(long val) {
        return ByteBuffer.allocate(Long.BYTES).putLong(val).array();
    }

    public static byte[] toBytes(int val) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(val).array();
    }
}
