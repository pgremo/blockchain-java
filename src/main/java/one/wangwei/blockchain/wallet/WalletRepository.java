package one.wangwei.blockchain.wallet;

import one.wangwei.blockchain.util.ObjectMapper;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static javax.crypto.Cipher.ENCRYPT_MODE;

public class WalletRepository {
    private static final Path WALLET_FILE = Path.of("wallet.dat");
    private static final String ALGORITHM = "AES";
    private final ObjectMapper serializer;
    private final SecretKeySpec key;

    public WalletRepository(ObjectMapper serializer, byte[] cipherText) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, IOException, InvalidKeyException, ClassNotFoundException {
        this.serializer = serializer;
        this.key = new SecretKeySpec(cipherText, ALGORITHM);

        if (Files.exists(WALLET_FILE)) {
            loadFromDisk();
        } else {
            saveToDisk(new HashMap<>());
        }
    }

    public Set<Address> getAddresses() throws NoSuchAlgorithmException, IOException, InvalidKeyException, ClassNotFoundException {
        return loadFromDisk().orElseGet(HashMap::new).keySet();
    }

    public Wallet getWallet(Address address) throws NoSuchAlgorithmException, IOException, InvalidKeyException, ClassNotFoundException {
        return loadFromDisk().orElseGet(HashMap::new).get(address);
    }

    public Wallet createWallet() throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, IOException, InvalidKeyException, ClassNotFoundException {
        var wallet = Wallet.createWallet();
        var wallets = loadFromDisk().orElseGet(HashMap::new);
        wallets.put(wallet.getAddress(), wallet);
        saveToDisk(wallets);
        return wallet;
    }

    private void saveToDisk(HashMap<Address, Wallet> wallets) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException, IllegalBlockSizeException {
        var cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(ENCRYPT_MODE, key);
        try (var outputStream = new BufferedOutputStream(newOutputStream(WALLET_FILE))) {
            outputStream.write(serializer.serialize(new SealedObject(wallets, cipher)));
        }
    }

    private Optional<HashMap<Address, Wallet>> loadFromDisk() throws NoSuchAlgorithmException, InvalidKeyException, IOException, ClassNotFoundException {
        try (var inputStream = new BufferedInputStream(newInputStream(WALLET_FILE))) {
            var sealedObject = serializer.deserialize(inputStream, SealedObject.class);
            return Optional.of((HashMap<Address, Wallet>) sealedObject.getObject(key));
        }
    }
}
