package one.wangwei.blockchain.cli;

import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.pow.Pow;
import one.wangwei.blockchain.store.RocksDbBlockRepository;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.Numbers;
import one.wangwei.blockchain.util.ObjectMapper;
import one.wangwei.blockchain.wallet.Address;
import one.wangwei.blockchain.wallet.WalletRepository;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.LogManager;

import static java.lang.System.*;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

public class CLI {
    private static final Logger logger = getLogger(CLI.class.getName());

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WalletRepository walletRepository = new WalletRepository(objectMapper);
    private final String[] args;
    private final Options options = new Options();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    static {
        try (var is = CLI.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public CLI(String[] args) {
        this.args = args;
        var helpCmd = Option.builder("h").desc("show help").build();
        options.addOption(helpCmd);
        var address = Option.builder().longOpt("address").hasArg(true).desc("Source wallet address").build();
        var sendFrom = Option.builder().longOpt("from").hasArg(true).desc("Source wallet address").build();
        var sendTo = Option.builder().longOpt("to").hasArg(true).desc("Destination wallet address").build();
        var sendAmount = Option.builder().longOpt("amount").hasArg(true).desc("Amount to send").build();
        options.addOption(address);
        options.addOption(sendFrom);
        options.addOption(sendTo);
        options.addOption(sendAmount);
    }

    public void parse() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException, RocksDBException, ParseException, InvalidKeySpecException, InvalidAlgorithmParameterException {
        this.validateArgs(args);
        try (var storage = new RocksDbBlockRepository(objectMapper)) {
            var cmd = new DefaultParser().parse(options, args);
            switch (args[0]) {
                case "createblockchain" -> {
                    var address = Address.valueOf(cmd.getOptionValue("address"));
                    if (address.isEmpty()) {
                        help();
                    }
                    this.createBlockchain(storage, address.get());
                }
                case "getbalance" -> {
                    var address = Address.valueOf(cmd.getOptionValue("address"));
                    if (address.isEmpty()) {
                        help();
                    }
                    this.getBalance(storage, address.get());
                }
                case "send" -> {
                    var sendFrom = Address.valueOf(cmd.getOptionValue("from"));
                    var sendTo = Address.valueOf(cmd.getOptionValue("to"));
                    var sendAmount = Numbers.parseInteger(cmd.getOptionValue("amount"));
                    if (sendFrom.isEmpty() || sendTo.isEmpty() || sendAmount.isEmpty()) {
                        help();
                    }
                    this.send(storage, sendFrom.get(), sendTo.get(), sendAmount.get());
                }
                case "createwallet" -> this.createWallet();
                case "printaddresses" -> this.printAddresses();
                case "printchain" -> this.printChain(storage);
                default -> this.help();
            }
        }
    }

    private void validateArgs(String[] args) {
        if (args == null || args.length < 1) {
            help();
        }
    }

    private void createBlockchain(RocksDbBlockRepository storage, Address address) {
        Blockchain.createBlockchain(storage, address);
        logger.log(INFO, "Done ! ");
    }

    private void createWallet() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        var wallet = walletRepository.createWallet();
        logger.log(INFO, () -> "wallet address : %s".formatted(wallet.getAddress()));
    }

    private void printAddresses() {
        var addresses = walletRepository.getAddresses();
        if (addresses == null || addresses.isEmpty()) {
            logger.log(INFO, "There isn't address");
            return;
        }
        for (var address : addresses) {
            logger.log(INFO, () -> "Wallet address: %s".formatted(address));
        }
    }

    private void getBalance(RocksDbBlockRepository storage, Address address) {
        var blockchain = Blockchain.createBlockchain(storage, address);
        var balance = Transaction.getUnspent(blockchain, walletRepository.getWallet(address))
                .mapToInt(x -> x.output().value())
                .sum();
        logger.log(INFO, () -> "Balance of '%s': %s".formatted(address, balance));
    }

    private void send(RocksDbBlockRepository storage, Address from, Address to, int amount) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException, InvalidKeySpecException {
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

    private void help() {
        out.println("Usage:");
        out.println("  createwallet - Generates a new key-pair and saves it into the wallet file");
        out.println("  printaddresses - print all wallet address");
        out.println("  getbalance -address ADDRESS - Get balance of ADDRESS");
        out.println("  createblockchain -address ADDRESS - Create a blockchain and send genesis block reward to ADDRESS");
        out.println("  printchain - Print all the blocks of the blockchain");
        out.println("  send -from FROM -to TO -amount AMOUNT - Send AMOUNT of coins from FROM address to TO");
        exit(0);
    }

    private void printChain(RocksDbBlockRepository storage) throws RocksDBException {
        for (var block : new Blockchain(storage)) {
            logger.log(INFO, () -> "%s, valid = %s".formatted(block, Pow.validate(block)));
        }
    }
}
