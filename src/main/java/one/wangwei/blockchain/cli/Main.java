package one.wangwei.blockchain.cli;

import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.pow.Pow;
import one.wangwei.blockchain.store.RocksDbBlockRepository;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.ObjectMapper;
import one.wangwei.blockchain.wallet.Address;
import one.wangwei.blockchain.wallet.WalletRepository;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.rocksdb.RocksDBException;
import picocli.CommandLine;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.LogManager;

import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.getLogger;
import static java.nio.charset.StandardCharsets.UTF_8;
import static one.wangwei.blockchain.block.Blockchain.createBlockchain;
import static one.wangwei.blockchain.transaction.Transaction.*;
import static picocli.CommandLine.*;

@Command(
        name = "blockchain",
        subcommands = {HelpCommand.class},
        description = "Manage a blockchain"
)
public class Main {
    private static final System.Logger logger = getLogger(Main.class.getName());

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WalletRepository walletRepository = new WalletRepository(objectMapper, "2oF@5sC%DNf32y!TmiZi!tG9W5rLaniD".getBytes(UTF_8));

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    static {
        try (var is = Main.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Main() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, IOException, InvalidKeyException, ClassNotFoundException {
    }

    @Command(
            description = "Create a blockchain",
            mixinStandardHelpOptions = true
    )
    void createblockchain(
            @Option(names = {"--address"}, converter = AddressTypeConverter.class) Address address
    ) throws RocksDBException {
        try (var storage = new RocksDbBlockRepository(objectMapper)) {
            createBlockchain(storage, address);
            logger.log(INFO, "Done!");
        }
    }

    @Command(
            description = "Get balance for an address",
            mixinStandardHelpOptions = true
    )
    void getbalance(
            @Option(names = {"--address"}, converter = AddressTypeConverter.class) Address address
    ) throws RocksDBException, NoSuchAlgorithmException, IOException, InvalidKeyException, ClassNotFoundException {
        try (var storage = new RocksDbBlockRepository(objectMapper)) {
            var blockchain = createBlockchain(storage, address);
            var balance = getUnspent(blockchain, walletRepository.getWallet(address))
                    .mapToInt(x -> x.output().value())
                    .sum();
            logger.log(INFO, () -> "Balance of '%s': %s".formatted(address, balance));
        }
    }

    @Command(
            description = "Send an amount from one address to another",
            mixinStandardHelpOptions = true
    )
    void send(
            @Option(names = {"--to"}, converter = AddressTypeConverter.class) Address to,
            @Option(names = {"--from"}, converter = AddressTypeConverter.class) Address from,
            @Option(names = {"--amount"}, converter = NaturalNumberTypeConverter.class) int amount
    ) throws RocksDBException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, InvalidKeySpecException, IOException, ClassNotFoundException {
        try (var storage = new RocksDbBlockRepository(objectMapper)) {
            var blockchain = createBlockchain(storage, from);
            var transaction = createTransaction(from, to, amount, blockchain, walletRepository);
            var rewardTx = createCoinbaseTX(from, "");
            blockchain.mineBlock(new Transaction[]{transaction, rewardTx}).orElseThrow();
            logger.log(INFO, "Success!");
        }
    }

    @Command(
            description = "Create a wallet",
            mixinStandardHelpOptions = true
    )
    void createwallet() throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, IOException, InvalidKeyException, ClassNotFoundException {
        var wallet = walletRepository.createWallet();
        logger.log(INFO, () -> "wallet address : %s".formatted(wallet.getAddress()));
    }

    @Command(
            description = "Print all wallets",
            mixinStandardHelpOptions = true
    )
    void printaddresses() throws NoSuchAlgorithmException, IOException, InvalidKeyException, ClassNotFoundException {
        var addresses = walletRepository.getAddresses();
        for (var address : addresses) {
            logger.log(INFO, () -> "Wallet address: %s".formatted(address));
        }
        if (addresses.isEmpty()) {
            logger.log(INFO, "No addresses");
        }
    }

    @Command(
            description = "Dump the blockchain",
            mixinStandardHelpOptions = true
    )
    void printchain() throws RocksDBException {
        try (var storage = new RocksDbBlockRepository(objectMapper)) {
            new Blockchain(storage).stream().forEach(x -> logger.log(INFO, () -> "%s, valid = %s".formatted(x, Pow.validate(x))));
        }
    }

    public static void main(String... args) throws Exception {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
