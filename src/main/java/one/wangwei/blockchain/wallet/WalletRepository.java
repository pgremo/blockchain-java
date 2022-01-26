package one.wangwei.blockchain.wallet;

import one.wangwei.blockchain.util.ObjectMapper;

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

public class WalletRepository {
    private static final System.Logger logger = System.getLogger(WalletRepository.class.getName());

    public WalletRepository(ObjectMapper serializer) {
        this.serializer = serializer;
        initWalletFile();
    }

    private static final String WALLET_FILE = "wallet.dat";
    private static final String ALGORITHM = "AES";
    private static final byte[] CIPHER_TEXT = "2oF@5sC%DNf32y!TmiZi!tG9W5rLaniD".getBytes(UTF_8);

    private final ObjectMapper serializer;

    private void initWalletFile() {
        var file = new File(WALLET_FILE);
        if (file.exists()) {
            this.loadFromDisk();
        } else {
            this.saveToDisk(new Wallets());
        }
    }

    public Set<Address> getAddresses() {
        return loadFromDisk().orElseThrow().getAddresses();
    }

    public Wallet getWallet(Address address) {
        return loadFromDisk().orElseThrow().getWallet(address);
    }

    public Wallet createWallet() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        var wallet = Wallet.createWallet();
        var wallets = loadFromDisk().orElse(new Wallets());
        wallets.addWallet(wallet);
        saveToDisk(wallets);
        return wallet;
    }

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
                serializer.serializeToStream(new SealedObject(wallets, cipher), outputStream);
            }
        } catch (Exception e) {
            logger.log(ERROR, "Fail to save wallet to disk !", e);
            throw new RuntimeException("Fail to save wallet to disk !");
        }
    }

    private Optional<Wallets> loadFromDisk() {
        try {
            var sks = new SecretKeySpec(CIPHER_TEXT, ALGORITHM);
            var cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(DECRYPT_MODE, sks);
            try (var inputStream = new CipherInputStream(new BufferedInputStream(new FileInputStream(WALLET_FILE)), cipher)) {
                var sealedObject = serializer.deserialize(inputStream, SealedObject.class);
                return Optional.of((Wallets) sealedObject.getObject(cipher));
            }
        } catch (Exception e) {
            logger.log(ERROR, "error loading wallets", e);
            return Optional.empty();
        }
    }


    public static class Wallets implements Serializable {
        @Serial
        private static final long serialVersionUID = -2542070981569243131L;
        private final Map<Address, Wallet> walletMap = new HashMap<>();

        private void addWallet(Wallet wallet) {
            this.walletMap.put(wallet.getAddress(), wallet);
        }

        Set<Address> getAddresses() {
            return walletMap.keySet();
        }

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
