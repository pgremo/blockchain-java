package one.wangwei.blockchain.util;

import org.bouncycastle.util.Arrays;

public final class BtcAddressUtils {

    public static byte[] doubleHash(byte[] data) {
        return Hashes.sha256(Hashes.sha256(data));
    }

    public static byte[] ripeMD160Hash(byte[] pubKey) {
        return Hashes.ripemd160(Hashes.sha256(pubKey));
    }

    public static byte[] checksum(byte[] payload) {
        return Arrays.copyOfRange(doubleHash(payload), 0, 4);
    }

}
