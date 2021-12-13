package one.wangwei.blockchain.wallet;

import java.io.Serializable;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.arraycopy;
import static one.wangwei.blockchain.util.Base58Check.encode;
import static one.wangwei.blockchain.util.BtcAddressUtils.checksum;
import static one.wangwei.blockchain.util.BtcAddressUtils.ripeMD160Hash;

/**
 * 钱包
 *
 * @author wangwei
 * @date 2018/03/14
 */
public record Wallet(PrivateKey privateKey, byte[] publicKey) implements Serializable {
    private static final Logger logger = Logger.getLogger(Wallet.class.getName());

    public static Wallet createWallet() {
        try {
            var keyPair = newECKeyPair();
            var privateKey = keyPair.getPrivate();
            var publicKey = keyPair.getPublic().getEncoded();
            return new Wallet(privateKey, publicKey);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fail to init wallet ! ", e);
            throw new RuntimeException("Fail to init wallet ! ", e);
        }
    }

    /**
     * 创建新的密钥对
     *
     * @return
     * @throws Exception
     */
    private static KeyPair newECKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {
        var generator = KeyPairGenerator.getInstance("EC", "SunEC");
        generator.initialize(new ECGenParameterSpec("secp521r1"));
        return generator.generateKeyPair();
    }

    /**
     * 获取钱包地址
     *
     * @return
     */
    public String getAddress() {
        var buffer = new byte[25];
        buffer[0] = 0;
        byte[] hash = ripeMD160Hash(publicKey);
        arraycopy(hash, 0, buffer, 1, hash.length);
        var versioned = new byte[21];
        arraycopy(buffer, 0, versioned, 0, versioned.length);
        var check = checksum(versioned);
        arraycopy(check, 0, buffer, 21, check.length);
        return encode(buffer);
    }
}
