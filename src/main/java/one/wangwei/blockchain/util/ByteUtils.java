package one.wangwei.blockchain.util;

import org.apache.commons.codec.binary.Hex;

import java.nio.ByteBuffer;

/**
 * 字节数组工具类
 *
 * @author wangwei
 * @date 2018/02/05
 */
public class ByteUtils {

    public static final byte[] EMPTY_BYTES = new byte[32];

    public static final String ZERO_HASH = Hex.encodeHexString(EMPTY_BYTES);

    /**
     * long 类型转 byte[]
     *
     * @param val
     * @return
     */
    public static byte[] toBytes(long val) {
        return ByteBuffer.allocate(Long.BYTES).putLong(val).array();
    }

}
