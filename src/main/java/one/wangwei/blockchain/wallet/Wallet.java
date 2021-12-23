package one.wangwei.blockchain.wallet;

import java.io.Serializable;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

import static one.wangwei.blockchain.util.BtcAddressUtils.ripeMD160Hash;


public record Wallet(PrivateKey privateKey, PublicKey publicKey) implements Serializable {

    public static Wallet createWallet() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        var generator = KeyPairGenerator.getInstance("EC", "SunEC");
        generator.initialize(new ECGenParameterSpec("secp521r1"));
        var keyPair = generator.generateKeyPair();
        return new Wallet(keyPair.getPrivate(), keyPair.getPublic());
    }

    public Address getAddress() {
        return new Address(ripeMD160Hash(publicKey.getEncoded()));
    }
}
