package one.wangwei.blockchain.wallet;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

import static one.wangwei.blockchain.util.BtcAddressUtils.checksum;
import static one.wangwei.blockchain.util.BtcAddressUtils.ripeMD160Hash;


public record Wallet(Address.Version version, PrivateKey privateKey, PublicKey publicKey) implements Serializable {
    private static final KeyPairGenerator generator;

    static {
        try {
            generator = KeyPairGenerator.getInstance("EC", "SunEC");
            generator.initialize(new ECGenParameterSpec("secp521r1"));
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    public static Wallet createWallet(Address.Version version) {
        var pair = generator.generateKeyPair();
        return new Wallet(version, pair.getPrivate(), pair.getPublic());
    }

    public Address getAddress() {
        var value = ripeMD160Hash(publicKey.getEncoded());
        var check = checksum(
                ByteBuffer.allocate(value.length + 1)
                        .put(version.value())
                        .put(value)
                        .array()
        );
        return new Address(version, value, check);
    }
}
