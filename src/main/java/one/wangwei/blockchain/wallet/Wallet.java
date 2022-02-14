package one.wangwei.blockchain.wallet;

import java.io.Serializable;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

import static java.lang.System.arraycopy;
import static one.wangwei.blockchain.util.BtcAddressUtils.checksum;
import static one.wangwei.blockchain.util.BtcAddressUtils.ripeMD160Hash;


public record Wallet(PrivateKey privateKey, PublicKey publicKey) implements Serializable {

    public static Wallet createWallet() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        var generator = KeyPairGenerator.getInstance("EC", "SunEC");
        generator.initialize(new ECGenParameterSpec("secp521r1"));
        var keyPair = generator.generateKeyPair();
        return new Wallet(keyPair.getPrivate(), keyPair.getPublic());
    }

    public Address getAddress() {
        var value = ripeMD160Hash(publicKey.getEncoded());
        var buffer = new byte[25];
        buffer[0] = 0;
        arraycopy(value, 0, buffer, 1, value.length);
        var versioned = new byte[21];
        arraycopy(buffer, 0, versioned, 0, versioned.length);
        var check = checksum(versioned);
        arraycopy(check, 0, buffer, 21, check.length);
        return new Address((byte) 0, value, check);
    }
}
