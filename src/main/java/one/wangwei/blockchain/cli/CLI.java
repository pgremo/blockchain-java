package one.wangwei.blockchain.cli;

import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.pow.ProofOfWork;
import one.wangwei.blockchain.store.RocksDBUtils;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.transaction.UTXOSet;
import one.wangwei.blockchain.util.Base58Check;
import one.wangwei.blockchain.util.Numbers;
import one.wangwei.blockchain.wallet.WalletUtils;
import org.apache.commons.cli.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.util.Arrays;

/**
 * 命令行解析器
 *
 * @author wangwei
 * @date 2018/03/08
 */
public class CLI {
    @SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CLI.class);
    private String[] args;
    private Options options = new Options();

    static{
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
    public void parse() {
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
                case "h" -> this.help();
                default -> this.help();
            }
        } catch (Exception e) {
            log.error("Fail to parse cli command ! ", e);
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
        log.info("Done ! ");
    }

    /**
     * 创建钱包
     *
     * @throws Exception
     */
    private void createWallet() throws Exception {
        var wallet = WalletUtils.getInstance().createWallet();
        log.info("wallet address : " + wallet.getAddress());
    }

    /**
     * 打印钱包地址
     */
    private void printAddresses() {
        var addresses = WalletUtils.getInstance().getAddresses();
        if (addresses == null || addresses.isEmpty()) {
            log.info("There isn\'t address");
            return;
        }
        for (var address : addresses) {
            log.info("Wallet address: " + address);
        }
    }

    /**
     * 查询钱包余额
     *
     * @param address 钱包地址
     */
    private void getBalance(String address) {
        // 检查钱包地址是否合法
        try {
            Base58Check.base58ToBytes(address);
        } catch (Exception e) {
            log.error("ERROR: invalid wallet address", e);
            throw new RuntimeException("ERROR: invalid wallet address", e);
        }
        // 得到公钥Hash值
        var versionedPayload = Base58Check.base58ToBytes(address);
        var pubKeyHash = Arrays.copyOfRange(versionedPayload, 1, versionedPayload.length);
        var blockchain = Blockchain.createBlockchain(address);
        var utxoSet = new UTXOSet(blockchain);
        var txOutputs = utxoSet.findUTXOs(pubKeyHash);
        var balance = 0;
        if (txOutputs != null && !txOutputs.isEmpty()) {
            for (var txOutput : txOutputs) {
                balance += txOutput.getValue();
            }
        }
        log.info("Balance of \'{}\': {}\n", address, balance);
    }

    /**
     * 转账
     *
     * @param from
     * @param to
     * @param amount
     * @throws Exception
     */
    private void send(String from, String to, int amount) throws Exception {
        // 检查钱包地址是否合法
        try {
            Base58Check.base58ToBytes(from);
        } catch (Exception e) {
            log.error("ERROR: sender address invalid ! address=" + from, e);
            throw new RuntimeException("ERROR: sender address invalid ! address=" + from, e);
        }
        // 检查钱包地址是否合法
        try {
            Base58Check.base58ToBytes(to);
        } catch (Exception e) {
            log.error("ERROR: receiver address invalid ! address=" + to, e);
            throw new RuntimeException("ERROR: receiver address invalid ! address=" + to, e);
        }
        if (amount < 1) {
            log.error("ERROR: amount invalid ! amount=" + amount);
            throw new RuntimeException("ERROR: amount invalid ! amount=" + amount);
        }
        var blockchain = Blockchain.createBlockchain(from);
        // 新交易
        var transaction = Transaction.newUTXOTransaction(from, to, amount, blockchain);
        // 奖励
        var rewardTx = Transaction.newCoinbaseTX(from, "");
        var newBlock = blockchain.mineBlock(new Transaction[] {transaction, rewardTx}).orElseThrow();
        new UTXOSet(blockchain).update(newBlock);
        log.info("Success!");
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
                log.info(block + ", validate = " + ProofOfWork.newProofOfWork(block).validate());
            }
        }
    }
}
