package one.wangwei.blockchain.wallet;

import one.wangwei.blockchain.util.Base58Check;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SealedObject;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 钱包工具类
 *
 * @author wangwei
 * @date 2018/03/21
 */
public class WalletUtils {
    @SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WalletUtils.class);
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
    private static final byte[] CIPHER_TEXT = "2oF@5sC%DNf32y!TmiZi!tG9W5rLaniD".getBytes();

    /**
     * 初始化钱包文件
     */
    private void initWalletFile() {
        var file = new File(WALLET_FILE);
        if (!file.exists()) {
            this.saveToDisk(new Wallets());
        } else {
            this.loadFromDisk();
        }
    }

    /**
     * 获取所有的钱包地址
     *
     * @return
     */
    public Set<String> getAddresses() {
        return this.loadFromDisk().getAddresses();
    }

    /**
     * 获取钱包数据
     *
     * @param address 钱包地址
     * @return
     */
    public Wallet getWallet(String address) {
        return this.loadFromDisk().getWallet(address);
    }

    /**
     * 创建钱包
     *
     * @return
     */
    public Wallet createWallet() {
        var wallet = new Wallet();
        var wallets = this.loadFromDisk();
        wallets.addWallet(wallet);
        this.saveToDisk(wallets);
        return wallet;
    }

    /**
     * 保存钱包数据
     */
    private void saveToDisk(Wallets wallets) {
        try {
            if (wallets == null) {
                log.error("Fail to save wallet to file ! wallets is null ");
                throw new Exception("ERROR: Fail to save wallet to file !");
            }
            var sks = new SecretKeySpec(CIPHER_TEXT, ALGORITHM);
            // Create cipher
            var cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, sks);
            var sealedObject = new SealedObject(wallets, cipher);
            // Wrap the output stream
            var cos = new CipherOutputStream(new BufferedOutputStream(new FileOutputStream(WALLET_FILE)), cipher);
            try {
                var outputStream = new ObjectOutputStream(cos);
                try {
                    outputStream.writeObject(sealedObject);
                } finally {
                    if (java.util.Collections.singletonList(outputStream).get(0) != null) {
                        outputStream.close();
                    }
                }
            } finally {
                if (java.util.Collections.singletonList(cos).get(0) != null) {
                    cos.close();
                }
            }
        } catch (Exception e) {
            log.error("Fail to save wallet to disk !", e);
            throw new RuntimeException("Fail to save wallet to disk !");
        }
    }

    /**
     * 加载钱包数据
     */
    private Wallets loadFromDisk() {
        try {
            var sks = new SecretKeySpec(CIPHER_TEXT, ALGORITHM);
            var cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, sks);
            var cipherInputStream = new CipherInputStream(new BufferedInputStream(new FileInputStream(WALLET_FILE)), cipher);
            try {
                var inputStream = new ObjectInputStream(cipherInputStream);
                try {
                    var sealedObject = (SealedObject) inputStream.readObject();
                    return (Wallets) sealedObject.getObject(cipher);
                } finally {
                    if (java.util.Collections.singletonList(inputStream).get(0) != null) {
                        inputStream.close();
                    }
                }
            } finally {
                if (java.util.Collections.singletonList(cipherInputStream).get(0) != null) {
                    cipherInputStream.close();
                }
            }
        } catch (Exception e) {
            log.error("Fail to load wallet from disk ! ", e);
            throw new RuntimeException("Fail to load wallet from disk ! ");
        }
    }


    /**
     * 钱包存储对象
     */
    public static class Wallets implements Serializable {
        private static final long serialVersionUID = -2542070981569243131L;
        private Map<String, Wallet> walletMap = new HashMap();

        /**
         * 添加钱包
         *
         * @param wallet
         */
        private void addWallet(Wallet wallet) {
            try {
                this.walletMap.put(wallet.getAddress(), wallet);
            } catch (Exception e) {
                log.error("Fail to add wallet ! ", e);
                throw new RuntimeException("Fail to add wallet !");
            }
        }

        /**
         * 获取所有的钱包地址
         *
         * @return
         */
        Set<String> getAddresses() {
            if (walletMap == null) {
                log.error("Fail to get address ! walletMap is null ! ");
                throw new RuntimeException("Fail to get addresses ! ");
            }
            return walletMap.keySet();
        }

        /**
         * 获取钱包数据
         *
         * @param address 钱包地址
         * @return
         */
        Wallet getWallet(String address) {
            // 检查钱包地址是否合法
            try {
                Base58Check.base58ToBytes(address);
            } catch (Exception e) {
                log.error("Fail to get wallet ! address invalid ! address=" + address, e);
                throw new RuntimeException("Fail to get wallet ! ");
            }
            var wallet = walletMap.get(address);
            if (wallet == null) {
                log.error("Fail to get wallet ! wallet don`t exist ! address=" + address);
                throw new RuntimeException("Fail to get wallet ! ");
            }
            return wallet;
        }

        @SuppressWarnings("all")
        public Map<String, Wallet> getWalletMap() {
            return this.walletMap;
        }

        @SuppressWarnings("all")
        public void setWalletMap(final Map<String, Wallet> walletMap) {
            this.walletMap = walletMap;
        }

        @Override
        @SuppressWarnings("all")
        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof Wallets)) return false;
            final Wallets other = (Wallets) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$walletMap = this.getWalletMap();
            final Object other$walletMap = other.getWalletMap();
            if (this$walletMap == null ? other$walletMap != null : !this$walletMap.equals(other$walletMap)) return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final Object other) {
            return other instanceof Wallets;
        }

        @Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $walletMap = this.getWalletMap();
            result = result * PRIME + ($walletMap == null ? 43 : $walletMap.hashCode());
            return result;
        }

        @Override
        @SuppressWarnings("all")
        public String toString() {
            return "WalletUtils.Wallets(walletMap=" + this.getWalletMap() + ")";
        }

        @SuppressWarnings("all")
        public Wallets() {
        }

        @SuppressWarnings("all")
        public Wallets(final Map<String, Wallet> walletMap) {
            this.walletMap = walletMap;
        }
    }
}
