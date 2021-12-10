package one.wangwei.blockchain.wallet;

import java.io.Serializable;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.arraycopy;
import static one.wangwei.blockchain.util.Base58Check.rawBytesToBase58;
import static one.wangwei.blockchain.util.BtcAddressUtils.checksum;
import static one.wangwei.blockchain.util.BtcAddressUtils.ripeMD160Hash;

/**
 * 钱包
 *
 * @author wangwei
 * @date 2018/03/14
 */
public class Wallet implements Serializable {
    private static final Logger logger = Logger.getLogger(Wallet.class.getName());
    private static final long serialVersionUID = 166249065006236265L;
    /**
     * 私钥
     */
    private PrivateKey privateKey;
    /**
     * 公钥
     */
    private byte[] publicKey;

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

    public Wallet() {
        initWallet();
    }

    /**
     * 初始化钱包
     */
    private void initWallet() {
        try {
            var keyPair = newECKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic().getEncoded();
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
        return rawBytesToBase58(buffer);
    }

    /**
     * 私钥
     */
    public PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    /**
     * 公钥
     */
    public byte[] getPublicKey() {
        return this.publicKey;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof final Wallet other)) return false;
        if (!other.canEqual(this)) return false;
        final Object this$privateKey = this.getPrivateKey();
        final Object other$privateKey = other.getPrivateKey();
        if (!Objects.equals(this$privateKey, other$privateKey)) return false;
        return java.util.Arrays.equals(this.getPublicKey(), other.getPublicKey());
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Wallet;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $privateKey = this.getPrivateKey();
        result = result * PRIME + ($privateKey == null ? 43 : $privateKey.hashCode());
        result = result * PRIME + java.util.Arrays.hashCode(this.getPublicKey());
        return result;
    }

    @Override
    public String toString() {
        return "Wallet(privateKey=" + this.getPrivateKey() + ", publicKey=" + java.util.Arrays.toString(this.getPublicKey()) + ")";
    }

    public Wallet(final PrivateKey privateKey, final byte[] publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }
}
