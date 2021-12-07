package one.wangwei.blockchain.wallet;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.ECNamedCurveTable;

import java.io.Serializable;
import java.security.*;
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
    private BCECPrivateKey privateKey;
    /**
     * 公钥
     */
    private byte[] publicKey;

    public Wallet() {
        initWallet();
    }

    /**
     * 初始化钱包
     */
    private void initWallet() {
        try {
            var keyPair = newECKeyPair();
            privateKey = (BCECPrivateKey) keyPair.getPrivate();
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
    private KeyPair newECKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        var keyPairGenerator = KeyPairGenerator.getInstance("ECDSA");
        // bitcoin 为什么会选择 secp256k1，详见：https://bitcointalk.org/index.php?topic=151120.0
        var ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        keyPairGenerator.initialize(ecSpec, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
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
    @SuppressWarnings("all")
    public BCECPrivateKey getPrivateKey() {
        return this.privateKey;
    }

    /**
     * 公钥
     */
    @SuppressWarnings("all")
    public byte[] getPublicKey() {
        return this.publicKey;
    }

    /**
     * 私钥
     */
    @SuppressWarnings("all")
    public void setPrivateKey(final BCECPrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * 公钥
     */
    @SuppressWarnings("all")
    public void setPublicKey(final byte[] publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Wallet)) return false;
        final Wallet other = (Wallet) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$privateKey = this.getPrivateKey();
        final Object other$privateKey = other.getPrivateKey();
        if (this$privateKey == null ? other$privateKey != null : !this$privateKey.equals(other$privateKey))
            return false;
        if (!java.util.Arrays.equals(this.getPublicKey(), other.getPublicKey())) return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof Wallet;
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $privateKey = this.getPrivateKey();
        result = result * PRIME + ($privateKey == null ? 43 : $privateKey.hashCode());
        result = result * PRIME + java.util.Arrays.hashCode(this.getPublicKey());
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "Wallet(privateKey=" + this.getPrivateKey() + ", publicKey=" + java.util.Arrays.toString(this.getPublicKey()) + ")";
    }

    @SuppressWarnings("all")
    public Wallet(final BCECPrivateKey privateKey, final byte[] publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }
}
