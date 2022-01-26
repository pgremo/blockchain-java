package one.wangwei.blockchain;

import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.cli.Main;
import one.wangwei.blockchain.store.RocksDbBlockRepository;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.ObjectMapper;
import one.wangwei.blockchain.wallet.Wallet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Test;
import org.rocksdb.RocksDBException;
import picocli.CommandLine;

import java.security.*;
import java.security.spec.InvalidKeySpecException;

public class BlockchainTest {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void shouldVerify() throws RocksDBException, InvalidKeySpecException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, InvalidAlgorithmParameterException {
        var wallet = Wallet.createWallet();
        var data = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";
        var tx = Transaction.newCoinbaseTX(wallet.getAddress(), data);
        Assert.assertTrue(new Blockchain(new RocksDbBlockRepository(new ObjectMapper())).verifyTransactions(tx));
    }

    public static void main(String[] args) {
        try {
//            var argss = new String[]{"createwallet"};
            var argss = new String[]{"createblockchain", "--address", "1GZJE8xPjLEVW5QAgNZiLJUowCrSe4ktVW"};
//            var argss = new String[]{"printaddresses"};
//            var argss = new String[]{"printchain"};
//            var argss = new String[]{"getbalance", "--address", "15H4pofKxDHh2dS7kjevtvFzAj3asy4Wud"};
//            var argss = new String[]{"send", "--from", "1GZJE8xPjLEVW5QAgNZiLJUowCrSe4ktVW", "--to", "15H4pofKxDHh2dS7kjevtvFzAj3asy4Wud", "--amount", "5"};
            new CommandLine(new Main()).execute(argss);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
