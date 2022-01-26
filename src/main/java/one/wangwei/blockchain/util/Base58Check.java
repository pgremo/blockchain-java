package one.wangwei.blockchain.util;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

public final class Base58Check {

    public static String encodeChecked(byte[] data) {
        return encode(addCheck(data));
    }

    public static String encode(byte[] data) {
        // Convert to base-58 string
        var sb = new StringBuilder();
        var num = new BigInteger(1, data);
        while (num.signum() != 0) {
            var quotrem = num.divideAndRemainder(ALPHABET_SIZE);
            sb.append(ALPHABET.charAt(quotrem[1].intValue()));
            num = quotrem[0];
        }

        // Add '1' characters for leading 0-value bytes
        for (var i = 0; i < data.length && data[i] == 0; i++) {
            sb.append(ALPHABET.charAt(0));
        }
        return sb.reverse().toString();
    }


    private static byte[] addCheck(byte[] data) {
        try {
            var hash = Arrays.copyOf(BtcAddressUtils.doubleHash(data), 4);
            var buf = new ByteArrayOutputStream();
            buf.write(data);
            buf.write(hash);
            return buf.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }


    public static byte[] decodeChecked(String s) {
        return valueOf(s).orElseThrow(() -> new IllegalArgumentException("Checksum mismatch"));
    }

    public static Optional<byte[]> valueOf(String s) {
        var decoded = decode(s);
        var data = Arrays.copyOf(decoded, decoded.length - 4);
        var check = Arrays.copyOfRange(decoded, decoded.length - 4, decoded.length);
        var hash = Arrays.copyOf(BtcAddressUtils.doubleHash(data), 4);
        return Arrays.equals(hash, check) ? Optional.of(data) : Optional.empty();
    }


    static byte[] decode(String s) {
        // Parse base-58 string
        var num = BigInteger.ZERO;
        for (var i = 0; i < s.length(); i++) {
            num = num.multiply(ALPHABET_SIZE);
            var digit = ALPHABET.indexOf(s.charAt(i));
            if (digit == -1) {
                throw new IllegalArgumentException("Invalid character for Base58Check");
            }
            num = num.add(BigInteger.valueOf(digit));
        }
        // Strip possible leading zero due to mandatory sign bit
        var b = num.toByteArray();
        if (b[0] == 0) {
            b = Arrays.copyOfRange(b, 1, b.length);
        }
        try {
            // Convert leading '1' characters to leading 0-value bytes
            var buf = new ByteArrayOutputStream();
            for (var i = 0; i < s.length() && s.charAt(i) == ALPHABET.charAt(0); i++) {
                buf.write(0);
            }
            buf.write(b);
            return buf.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }


    /*---- Class constants ----*/

    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger ALPHABET_SIZE = BigInteger.valueOf(ALPHABET.length());


    /*---- Miscellaneous ----*/

    private Base58Check() {
    }  // Not instantiable

}
