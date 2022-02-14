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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

public class WalletRepository {
    public WalletRepository(ObjectMapper serializer) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, IOException, BadPaddingException, InvalidKeyException, ClassNotFoundException {
        this.serializer = serializer;
        if (Files.exists(WALLET_FILE)) {
            loadFromDisk();
        } else {
            saveToDisk(new HashMap<>());
        }
    }

    private static final Path WALLET_FILE = Path.of("wallet.dat");
    private static final String ALGORITHM = "AES";
    private static final byte[] CIPHER_TEXT = "2oF@5sC%DNf32y!TmiZi!tG9W5rLaniD".getBytes(UTF_8);

    private final ObjectMapper serializer;

    public Set<Address> getAddresses() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, IOException, BadPaddingException, InvalidKeyException, ClassNotFoundException {
        return loadFromDisk().orElseThrow().keySet();
    }

    public Wallet getWallet(Address address) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, IOException, BadPaddingException, InvalidKeyException, ClassNotFoundException {
        return loadFromDisk().orElseThrow().get(address);
    }

    public Wallet createWallet() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException, ClassNotFoundException {
        var wallet = Wallet.createWallet();
        var wallets = loadFromDisk().orElse(new HashMap<>());
        wallets.put(wallet.getAddress(), wallet);
        saveToDisk(wallets);
        return wallet;
    }

    private void saveToDisk(HashMap<Address, Wallet> wallets) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException, IllegalBlockSizeException {
        var cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(ENCRYPT_MODE, new SecretKeySpec(CIPHER_TEXT, ALGORITHM));
        try (var outputStream = new CipherOutputStream(new BufferedOutputStream(newOutputStream(WALLET_FILE)), cipher)) {
            outputStream.write(serializer.serialize(new SealedObject(wallets, cipher)));
        }
    }

    private Optional<HashMap<Address, Wallet>> loadFromDisk() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        var cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(DECRYPT_MODE, new SecretKeySpec(CIPHER_TEXT, ALGORITHM));
        try (var inputStream = new CipherInputStream(new BufferedInputStream(newInputStream(WALLET_FILE)), cipher)) {
            var sealedObject = serializer.deserialize(inputStream, SealedObject.class);
            return Optional.of((HashMap<Address, Wallet>) sealedObject.getObject(cipher));
        }
    }
}
