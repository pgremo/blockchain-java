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

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.LogManager;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.getLogger;
import static picocli.CommandLine.*;

@Command(
        name = "blockchain",
        subcommands = {HelpCommand.class},
        description = "Manage a blockchain"
)
public class Main {
    private static final System.Logger logger = getLogger(Main.class.getName());

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WalletRepository walletRepository = new WalletRepository(objectMapper);

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

    @Command(
            description = "Create a blockchain",
            mixinStandardHelpOptions = true,
            version = "4.1.3"
    )
    void createblockchain(
            @Option(names = {"--address"}, converter = AddressTypeConverter.class) Address address
    ) throws RocksDBException {
        try (var storage = new RocksDbBlockRepository(objectMapper)) {
            Blockchain.createBlockchain(storage, address);
            logger.log(INFO, "Done!");
        }
    }

    @Command(
            description = "Get balance for an address",
            mixinStandardHelpOptions = true,
            version = "4.1.3"
    )
    void getbalance(
            @Option(names = {"--address"}, converter = AddressTypeConverter.class) Address address
    ) throws RocksDBException {
        try (var storage = new RocksDbBlockRepository(objectMapper)) {
            var blockchain = Blockchain.createBlockchain(storage, address);
            var balance = Transaction.getUnspent(blockchain, walletRepository.getWallet(address))
                    .mapToInt(x -> x.output().value())
                    .sum();
            logger.log(INFO, () -> "Balance of '%s': %s".formatted(address, balance));
        }
    }

    @Command(
            description = "Send an amount from one address to another",
            mixinStandardHelpOptions = true,
            version = "4.1.3"
    )
    void send(
            @Option(names = {"--to"}, converter = AddressTypeConverter.class) Address to,
            @Option(names = {"--from"}, converter = AddressTypeConverter.class) Address from,
            @Option(names = {"--amount"}, converter = AmountTypeConverter.class) int amount
    ) throws RocksDBException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, InvalidKeySpecException {
        try (var storage = new RocksDbBlockRepository(objectMapper)) {
            if (amount < 1) {
                logger.log(ERROR, "amount invalid ! amount=%s".formatted(amount));
                throw new RuntimeException("amount invalid ! amount=" + amount);
            }
            var blockchain = Blockchain.createBlockchain(storage, from);
            // 新交易
            var transaction = Transaction.create(from, to, amount, blockchain, walletRepository);
            // 奖励
            var rewardTx = Transaction.newCoinbaseTX(from, "");
            blockchain.mineBlock(new Transaction[]{transaction, rewardTx}).orElseThrow();
            logger.log(INFO, "Success!");
        }
    }

    @Command(
            description = "Create a wallet",
            mixinStandardHelpOptions = true,
            version = "4.1.3"
    )
    void createwallet() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        var wallet = walletRepository.createWallet();
        logger.log(INFO, () -> "wallet address : %s".formatted(wallet.getAddress()));
    }

    @Command(
            description = "Print all wallets",
            mixinStandardHelpOptions = true,
            version = "4.1.3"
    )
    void printaddresses() {
        var addresses = walletRepository.getAddresses();
        if (addresses == null || addresses.isEmpty()) {
            logger.log(INFO, "There isn't address");
            return;
        }
        for (var address : addresses) {
            logger.log(INFO, () -> "Wallet address: %s".formatted(address));
        }
    }

    @Command(
            description = "Dump the blockchain",
            mixinStandardHelpOptions = true,
            version = "4.1.3"
    )
    void printchain() throws RocksDBException {
        try (var storage = new RocksDbBlockRepository(objectMapper)) {
            for (var block : new Blockchain(storage)) {
                logger.log(INFO, () -> "%s, valid = %s".formatted(block, Pow.validate(block)));
            }
        }
    }

    public static void main(String... args) throws Exception {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
