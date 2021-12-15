package one.wangwei.blockchain.cli;

import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.pow.Pow;
import one.wangwei.blockchain.store.RocksDBUtils;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.Base58Check;
import one.wangwei.blockchain.util.Numbers;
import one.wangwei.blockchain.wallet.WalletUtils;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.*;
import java.util.logging.LogManager;

import static java.lang.System.*;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

/**
 * 命令行解析器
 *
 * @author wangwei
 * @date 2018/03/08
 */
public class CLI {
    private static final Logger logger = getLogger(CLI.class.getName());
    private final String[] args;
    private final Options options = new Options();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    static {
        final LogManager logManager = LogManager.getLogManager();
        try (final var is = CLI.class.getResourceAsStream("/logging.properties")) {
            logManager.readConfiguration(is);
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

    /**
     * 命令行解析入口
     */
    public void parse() throws ParseException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException {
        this.validateArgs(args);
        try {
            var cmd = new DefaultParser().parse(options, args);
            switch (args[0]) {
                case "createblockchain" -> {
                    var createblockchainAddress = cmd.getOptionValue("address");
                    if (createblockchainAddress.isBlank()) {
                        help();
                    }
                    this.createBlockchain(createblockchainAddress);
                }
                case "getbalance" -> {
                    var getBalanceAddress = cmd.getOptionValue("address");
                    if (getBalanceAddress.isBlank()) {
                        help();
                    }
                    this.getBalance(getBalanceAddress);
                }
                case "send" -> {
                    var sendFrom = cmd.getOptionValue("from");
                    var sendTo = cmd.getOptionValue("to");
                    var sendAmount = Numbers.parseInteger(cmd.getOptionValue("amount"));
                    if (sendFrom.isBlank() || sendTo.isBlank() || sendAmount.isEmpty()) {
                        help();
                    }
                    this.send(sendFrom, sendTo, sendAmount.orElseThrow());
                }
                case "createwallet" -> this.createWallet();
                case "printaddresses" -> this.printAddresses();
                case "printchain" -> this.printChain();
                default -> this.help();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            RocksDBUtils.getInstance().closeDB();
        }
    }

    /**
     * 验证入参
     *
     * @param args
     */
    private void validateArgs(String[] args) {
        if (args == null || args.length < 1) {
            help();
        }
    }

    /**
     * 创建区块链
     *
     * @param address
     */
    private void createBlockchain(String address) {
        Blockchain.createBlockchain(address);
        logger.log(INFO, "Done ! ");
    }

    /**
     * 创建钱包
     *
     * @throws Exception
     */
    private void createWallet() {
        var wallet = WalletUtils.getInstance().createWallet();
        logger.log(INFO, () -> "wallet address : %s".formatted(wallet.getAddress()));
    }

    /**
     * 打印钱包地址
     */
    private void printAddresses() {
        var addresses = WalletUtils.getInstance().getAddresses();
        if (addresses == null || addresses.isEmpty()) {
            logger.log(INFO, "There isn\'t address");
            return;
        }
        for (var address : addresses) {
            logger.log(INFO, () -> "Wallet address: %s".formatted(address));
        }
    }

    /**
     * 查询钱包余额
     *
     * @param address 钱包地址
     */
    private void getBalance(String address) {
        // 检查钱包地址是否合法
        Base58Check.decodeChecked(address);
        // 得到公钥Hash值
        var blockchain = Blockchain.createBlockchain(address);
        var txOutputs = Transaction.getUnspent(Integer.MAX_VALUE, blockchain, WalletUtils.getInstance().getWallet(address));
        logger.log(INFO, () -> "Balance of '%s': %s".formatted(address, txOutputs.total()));
    }

    /**
     * 转账
     *
     * @param from
     * @param to
     * @param amount
     * @throws Exception
     */
    private void send(String from, String to, int amount) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException {
        // 检查钱包地址是否合法
        Base58Check.decodeChecked(from);
        // 检查钱包地址是否合法
        Base58Check.decodeChecked(to);
        if (amount < 1) {
            logger.log(ERROR, "amount invalid ! amount=%s".formatted(amount));
            throw new RuntimeException("amount invalid ! amount=" + amount);
        }
        var blockchain = Blockchain.createBlockchain(from);
        // 新交易
        var transaction = Transaction.create(from, to, amount, blockchain);
        // 奖励
        var rewardTx = Transaction.newCoinbaseTX(from, "");
        var newBlock = blockchain.mineBlock(new Transaction[]{transaction, rewardTx}).orElseThrow();
        logger.log(INFO, "Success!");
    }

    /**
     * 打印帮助信息
     */
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

    /**
     * 打印出区块链中的所有区块
     */
    private void printChain() {
        for (var block : Blockchain.initBlockchainFromDB()) {
            if (block != null) {
                logger.log(INFO, () -> "%s, validate = %s".formatted(block, Pow.validate(block)));
            }
        }
    }
}
