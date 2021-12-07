package one.wangwei.blockchain.cli;

import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.pow.ProofOfWork;
import one.wangwei.blockchain.store.RocksDBUtils;
import one.wangwei.blockchain.transaction.TXOutput;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.transaction.UTXOSet;
import one.wangwei.blockchain.util.Base58Check;
import one.wangwei.blockchain.util.Numbers;
import one.wangwei.blockchain.wallet.WalletUtils;
import org.apache.commons.cli.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * 命令行解析器
 *
 * @author wangwei
 * @date 2018/03/08
 */
public class CLI {
    @SuppressWarnings("all")
    private static final Logger logger = Logger.getLogger(CLI.class.getName());
    private final String[] args;
    private final Options options = new Options();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public CLI(String[] args) {
        this.args = args;
        Option helpCmd = Option.builder("h").desc("show help").build();
        options.addOption(helpCmd);
        Option address = Option.builder("address").hasArg(true).desc("Source wallet address").build();
        Option sendFrom = Option.builder("from").hasArg(true).desc("Source wallet address").build();
        Option sendTo = Option.builder("to").hasArg(true).desc("Destination wallet address").build();
        Option sendAmount = Option.builder("amount").hasArg(true).desc("Amount to send").build();
        options.addOption(address);
        options.addOption(sendFrom);
        options.addOption(sendTo);
        options.addOption(sendAmount);
    }

    /**
     * 命令行解析入口
     */
    public void parse() throws ParseException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        this.validateArgs(args);
        try {
            DefaultParser parser = new DefaultParser();
            var cmd = parser.parse(options, args);
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
        var blockchain = Blockchain.createBlockchain(address);
        var utxoSet = new UTXOSet(blockchain);
        utxoSet.reIndex();
        logger.info("Done ! ");
    }

    /**
     * 创建钱包
     *
     * @throws Exception
     */
    private void createWallet() {
        var wallet = WalletUtils.getInstance().createWallet();
        logger.info(() -> "wallet address : %s".formatted(wallet.getAddress()));
    }

    /**
     * 打印钱包地址
     */
    private void printAddresses() {
        var addresses = WalletUtils.getInstance().getAddresses();
        if (addresses == null || addresses.isEmpty()) {
            logger.info("There isn\'t address");
            return;
        }
        for (var address : addresses) {
            logger.info(() -> "Wallet address: %s".formatted(address));
        }
    }

    /**
     * 查询钱包余额
     *
     * @param address 钱包地址
     */
    private void getBalance(String address) {
        // 检查钱包地址是否合法
        Base58Check.base58ToBytes(address);
        // 得到公钥Hash值
        var versionedPayload = Base58Check.base58ToBytes(address);
        var pubKeyHash = Arrays.copyOfRange(versionedPayload, 1, versionedPayload.length);
        var blockchain = Blockchain.createBlockchain(address);
        var utxoSet = new UTXOSet(blockchain);
        var txOutputs = utxoSet.findUTXOs(pubKeyHash);
        var balance = txOutputs.stream().mapToInt(TXOutput::getValue).sum();
        logger.info(() -> "Balance of \'%s\': %s".formatted(address, balance));
    }

    /**
     * 转账
     *
     * @param from
     * @param to
     * @param amount
     * @throws Exception
     */
    private void send(String from, String to, int amount) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        // 检查钱包地址是否合法
        Base58Check.base58ToBytes(from);
        // 检查钱包地址是否合法
        Base58Check.base58ToBytes(to);
        if (amount < 1) {
            logger.severe("ERROR: amount invalid ! amount=%s".formatted(amount));
            throw new RuntimeException("ERROR: amount invalid ! amount=" + amount);
        }
        var blockchain = Blockchain.createBlockchain(from);
        // 新交易
        var transaction = Transaction.newUTXOTransaction(from, to, amount, blockchain);
        // 奖励
        var rewardTx = Transaction.newCoinbaseTX(from, "");
        var newBlock = blockchain.mineBlock(new Transaction[]{transaction, rewardTx}).orElseThrow();
        new UTXOSet(blockchain).update(newBlock);
        logger.info("Success!");
    }

    /**
     * 打印帮助信息
     */
    private void help() {
        System.out.println("Usage:");
        System.out.println("  createwallet - Generates a new key-pair and saves it into the wallet file");
        System.out.println("  printaddresses - print all wallet address");
        System.out.println("  getbalance -address ADDRESS - Get balance of ADDRESS");
        System.out.println("  createblockchain -address ADDRESS - Create a blockchain and send genesis block reward to ADDRESS");
        System.out.println("  printchain - Print all the blocks of the blockchain");
        System.out.println("  send -from FROM -to TO -amount AMOUNT - Send AMOUNT of coins from FROM address to TO");
        System.exit(0);
    }

    /**
     * 打印出区块链中的所有区块
     */
    private void printChain() {
        for (var block : Blockchain.initBlockchainFromDB()) {
            if (block != null) {
                logger.info("%s, validate = %s".formatted(block, ProofOfWork.newProofOfWork(block).validate()));
            }
        }
    }
}
