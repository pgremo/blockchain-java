package one.wangwei.blockchain.util;

import java.util.Optional;

import static java.lang.Character.digit;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 字节数组工具类
 *
 * @author wangwei
 * @date 2018/02/05
 */
public final class Bytes {

    public static final byte[] EMPTY_BYTES = new byte[32];

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(UTF_8);

    public static String byteArrayToHex(byte[] value) {
        if (value == null) return "";
        final byte[] data = new byte[value.length * 2];
        for (int j = 0; j < value.length; j++) {
            final int v = value[j] & 0xFF;
            data[j * 2] = HEX_ARRAY[v >>> 4];
            data[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(data, UTF_8);
    }

    public static Optional<byte[]> hexToByteArray(String value) {
        if ((value.length() % 2) != 0) return Optional.empty();
        final int length = value.length();
        final byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((digit(value.charAt(i), 16) << 4) + digit(value.charAt(i + 1), 16));
        }
        return Optional.of(data);
    }
}
