package one.wangwei.blockchain.wallet;

import one.wangwei.blockchain.util.SerializeUtils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SealedObject;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;

import static java.lang.System.Logger.Level.ERROR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

/**
 * 钱包工具类
 *
 * @author wangwei
 * @date 2018/03/21
 */
public class WalletUtils {
    private static final System.Logger logger = System.getLogger(WalletUtils.class.getName());
    /**
     * 钱包工具实例
     */
    private static volatile WalletUtils instance;

    public static WalletUtils getInstance() {
        if (instance == null) {
            synchronized (WalletUtils.class) {
                if (instance == null) {
                    instance = new WalletUtils();
                }
            }
        }
        return instance;
    }

    private WalletUtils() {
        initWalletFile();
    }

    /**
     * 钱包文件
     */
    private static final String WALLET_FILE = "wallet.dat";
    /**
     * 加密算法
     */
    private static final String ALGORITHM = "AES";
    /**
     * 密文
     */
    private static final byte[] CIPHER_TEXT = "2oF@5sC%DNf32y!TmiZi!tG9W5rLaniD".getBytes(UTF_8);

    /**
     * 初始化钱包文件
     */
    private void initWalletFile() {
        var file = new File(WALLET_FILE);
        if (file.exists()) {
            this.loadFromDisk();
        } else {
            this.saveToDisk(new Wallets());
        }
    }

    /**
     * 获取所有的钱包地址
     *
     * @return
     */
    public Set<Address> getAddresses() {
        return loadFromDisk().orElseThrow().getAddresses();
    }

    /**
     * 获取钱包数据
     *
     * @param address 钱包地址
     * @return
     */
    public Wallet getWallet(Address address) {
        return loadFromDisk().orElseThrow().getWallet(address);
    }

    /**
     * 创建钱包
     *
     * @return
     */
    public Wallet createWallet() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        var wallet = Wallet.createWallet();
        var wallets = loadFromDisk().orElse(new Wallets());
        wallets.addWallet(wallet);
        saveToDisk(wallets);
        return wallet;
    }

    /**
     * 保存钱包数据
     */
    private void saveToDisk(Wallets wallets) {
        try {
            if (wallets == null) {
                logger.log(ERROR, "Fail to save wallet to file ! wallets is null ");
                throw new Exception("ERROR: Fail to save wallet to file !");
            }
            var sks = new SecretKeySpec(CIPHER_TEXT, ALGORITHM);
            var cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(ENCRYPT_MODE, sks);
            try (var outputStream = new CipherOutputStream(new BufferedOutputStream(new FileOutputStream(WALLET_FILE)), cipher)) {
                SerializeUtils.serializeToStream(new SealedObject(wallets, cipher), outputStream);
            }
        } catch (Exception e) {
            logger.log(ERROR, "Fail to save wallet to disk !", e);
            throw new RuntimeException("Fail to save wallet to disk !");
        }
    }

    /**
     * 加载钱包数据
     *
     * @return
     */
    private Optional<Wallets> loadFromDisk() {
        try {
            var sks = new SecretKeySpec(CIPHER_TEXT, ALGORITHM);
            var cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(DECRYPT_MODE, sks);
            try (var inputStream = new CipherInputStream(new BufferedInputStream(new FileInputStream(WALLET_FILE)), cipher)) {
                var sealedObject = SerializeUtils.deserialize(inputStream, SealedObject.class);
                return Optional.of((Wallets) sealedObject.getObject(cipher));
            }
        } catch (Exception e) {
            logger.log(ERROR, "error loading wallets", e);
            return Optional.empty();
        }
    }


    /**
     * 钱包存储对象
     */
    public static class Wallets implements Serializable {
        @Serial
        private static final long serialVersionUID = -2542070981569243131L;
        private final Map<Address, Wallet> walletMap = new HashMap<>();

        /**
         * 添加钱包
         *
         * @param wallet
         */
        private void addWallet(Wallet wallet) {
            this.walletMap.put(wallet.getAddress(), wallet);
        }

        /**
         * 获取所有的钱包地址
         *
         * @return
         */
        Set<Address> getAddresses() {
            return walletMap.keySet();
        }

        /**
         * 获取钱包数据
         *
         * @param address 钱包地址
         * @return
         */
        Wallet getWallet(Address address) {
            var wallet = walletMap.get(address);
            if (wallet == null) {
                logger.log(ERROR, "Fail to get wallet ! wallet don`t exist ! address=" + address);
                throw new RuntimeException("Fail to get wallet ! ");
            }
            return wallet;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof final Wallets other)) return false;
            return Objects.equals(walletMap, other.walletMap);
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + walletMap.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "WalletUtils.Wallets(walletMap=" + walletMap + ")";
        }
    }
}
