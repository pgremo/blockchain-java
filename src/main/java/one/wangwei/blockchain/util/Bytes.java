package one.wangwei.blockchain.util;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class Bytes {

    private static final byte[] hexChar = "0123456789ABCDEF".getBytes(UTF_8);

    public static String byteArrayToHex(byte[] value) {
        if (value == null) return "";
        final byte[] data = new byte[value.length * 2];
        for (int j = 0; j < value.length; j++) {
            final int v = value[j] & 0xFF;
            data[j * 2] = hexChar[v >>> 4];
            data[j * 2 + 1] = hexChar[v & 0x0F];
        }
        return new String(data, UTF_8);
    }
}
