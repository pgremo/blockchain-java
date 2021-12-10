package one.wangwei.blockchain.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public final class Hashes {
    public static byte[] sha256(byte[] first, byte[]... rest) {
        return digest("SHA256", "SUN", first, rest);
    }

    public static byte[] ripemd160(byte[] first, byte[]... rest) {
        return digest("RIPEMD160", "BC", first, rest);
    }

    private static byte[] digest(String algorithm, String provider, byte[] first, byte[]... rest) {
        try {
            var digest = MessageDigest.getInstance(algorithm, provider);
            digest.update(first == null ? new byte[0] : first);
            for (var bytes : rest) digest.update(bytes == null ? new byte[0] : bytes);
            return digest.digest();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }
}
