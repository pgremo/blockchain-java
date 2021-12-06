package one.wangwei.blockchain.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Hashes {
    public static byte[] sha256(byte[] first, byte[]... rest) {
        try {
            var digest = MessageDigest.getInstance("SHA256");
            digest.update(first);
            for (var bytes : rest) digest.update(bytes);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] ripemd160(byte[] first, byte[]... rest) {
        try {
            var digest = MessageDigest.getInstance("RIPEMD160");
            digest.update(first);
            for (var bytes : rest) digest.update(bytes);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
