package one.wangwei.blockchain.wallet;

import one.wangwei.blockchain.util.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static java.nio.file.Files.*;
import static javax.crypto.Cipher.ENCRYPT_MODE;
import static one.wangwei.blockchain.wallet.Address.Version.Prod;

public class WalletRepository {
    private static final Path WALLET_FILE = Path.of("wallet.dat");
    private final ObjectMapper serializer;
    private final Key key;

    public WalletRepository(ObjectMapper serializer, Key key) {
        this.serializer = serializer;
        this.key = key;
    }

    public Set<Address> getAddresses() throws NoSuchAlgorithmException, IOException, InvalidKeyException, ClassNotFoundException {
        return load().orElseGet(HashMap::new).keySet();
    }

    public Wallet getWallet(Address address) throws NoSuchAlgorithmException, IOException, InvalidKeyException, ClassNotFoundException {
        return load().orElseGet(HashMap::new).get(address);
    }

    public Wallet createWallet() throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, IOException, InvalidKeyException, ClassNotFoundException {
        var wallet = Wallet.createWallet(Prod);
        var wallets = load().orElseGet(HashMap::new);
        wallets.put(wallet.getAddress(), wallet);
        save(wallets);
        return wallet;
    }

    private void save(HashMap<Address, Wallet> wallets) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException, IllegalBlockSizeException {
        var cipher = Cipher.getInstance(key.getAlgorithm());
        cipher.init(ENCRYPT_MODE, key);
        try (var stream = new BufferedOutputStream(newOutputStream(WALLET_FILE))) {
            stream.write(serializer.serialize(new SealedObject(wallets, cipher)));
        }
    }

    private Optional<HashMap<Address, Wallet>> load() throws NoSuchAlgorithmException, InvalidKeyException, IOException, ClassNotFoundException {
        if (!exists(WALLET_FILE)) return Optional.empty();
        try (var stream = new BufferedInputStream(newInputStream(WALLET_FILE))) {
            var sealedObject = serializer.deserialize(stream, SealedObject.class);
            return Optional.of((HashMap<Address, Wallet>) sealedObject.getObject(key));
        }
    }
}
