package one.wangwei.blockchain.wallet;

import one.wangwei.blockchain.util.Base58Check;
import one.wangwei.blockchain.util.BtcAddressUtils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     * 校验码长度
     */
    private static final int ADDRESS_CHECKSUM_LEN = 4;
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
            var privateKey = (BCECPrivateKey) keyPair.getPrivate();
            var publicKey = (BCECPublicKey) keyPair.getPublic();
            var publicKeyBytes = publicKey.getQ().getEncoded(false);
            this.setPrivateKey(privateKey);
            this.setPublicKey(publicKeyBytes);
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
    private KeyPair newECKeyPair() throws Exception {
        // 注册 BC Provider
        Security.addProvider(new BouncyCastleProvider());
        // 创建椭圆曲线算法的密钥对生成器，算法为 ECDSA
        var keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
        // 椭圆曲线（EC）域参数设定
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
        try {
            // 1. 获取 ripemdHashedKey
            var ripemdHashedKey = BtcAddressUtils.ripeMD160Hash(this.getPublicKey());
            // 2. 添加版本 0x00
            var addrStream = new ByteArrayOutputStream();
            addrStream.write((byte) 0);
            addrStream.write(ripemdHashedKey);
            var versionedPayload = addrStream.toByteArray();
            // 3. 计算校验码
            var checksum = BtcAddressUtils.checksum(versionedPayload);
            // 4. 得到 version + paylod + checksum 的组合
            addrStream.write(checksum);
            var binaryAddress = addrStream.toByteArray();
            // 5. 执行Base58转换处理
            return Base58Check.rawBytesToBase58(binaryAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Fail to get wallet address ! ");
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
        if (this$privateKey == null ? other$privateKey != null : !this$privateKey.equals(other$privateKey)) return false;
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
