package one.wangwei.blockchain;

import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.cli.Main;
import one.wangwei.blockchain.store.RocksDbBlockRepository;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.ObjectMapper;
import one.wangwei.blockchain.wallet.Address;
import one.wangwei.blockchain.wallet.Wallet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDBException;
import picocli.CommandLine;

import java.security.*;
import java.security.spec.InvalidKeySpecException;

import static one.wangwei.blockchain.wallet.Address.Version.Prod;

public class BlockchainTest {
    @Test
    public void shouldVerify() throws RocksDBException, InvalidKeySpecException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        var wallet = Wallet.createWallet(Prod);
        var data = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";
        var tx = Transaction.createCoinbaseTX(wallet.getAddress(), data);
        Assertions.assertTrue(new Blockchain(new RocksDbBlockRepository(new ObjectMapper())).verifyTransactions(tx));
    }

    public static void main(String[] args) {
        try {
//            var argss = new String[]{"createwallet"};
//            var argss = new String[]{"createblockchain", "--address", "1DL2cxJgyZUDRiFc8ZLUEiSmyhsW89Jtfe"};
//            var argss = new String[]{"printaddresses"};
            var argss = new String[]{"printchain"};
//            var argss = new String[]{"getbalance", "--address", "1DL2cxJgyZUDRiFc8ZLUEiSmyhsW89Jtfe"};
//            var argss = new String[]{"send", "--from", "1DL2cxJgyZUDRiFc8ZLUEiSmyhsW89Jtfe", "--to", "1Kqs49YBR1TUs6YKiEZ1WgbH7e3U9mkto7", "--amount", "5"};
            new CommandLine(new Main()).execute(argss);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
