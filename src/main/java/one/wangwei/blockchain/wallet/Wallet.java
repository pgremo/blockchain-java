package one.wangwei.blockchain.wallet;

import java.io.Serializable;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

import static java.lang.System.arraycopy;
import static one.wangwei.blockchain.util.Base58Check.encode;
import static one.wangwei.blockchain.util.BtcAddressUtils.checksum;
import static one.wangwei.blockchain.util.BtcAddressUtils.ripeMD160Hash;


public record Wallet(PrivateKey privateKey, PublicKey publicKey) implements Serializable {
    private static final System.Logger logger = System.getLogger(Wallet.class.getName());

    public static Wallet createWallet() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        var keyPair = newECKeyPair();
        return new Wallet(keyPair.getPrivate(), keyPair.getPublic());
    }

    private static KeyPair newECKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {
        var generator = KeyPairGenerator.getInstance("EC", "SunEC");
        generator.initialize(new ECGenParameterSpec("secp521r1"));
        return generator.generateKeyPair();
    }

    public String getAddress() {
        var buffer = new byte[25];
        buffer[0] = 0;
        byte[] hash = ripeMD160Hash(publicKey.getEncoded());
        arraycopy(hash, 0, buffer, 1, hash.length);
        var versioned = new byte[21];
        arraycopy(buffer, 0, versioned, 0, versioned.length);
        var check = checksum(versioned);
        arraycopy(check, 0, buffer, 21, check.length);
        return encode(buffer);
    }
}
