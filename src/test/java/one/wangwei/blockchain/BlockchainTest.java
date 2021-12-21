package one.wangwei.blockchain;

import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.cli.CLI;
import one.wangwei.blockchain.store.RocksDbBlockRepository;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.wallet.Wallet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Test;
import org.rocksdb.RocksDBException;

import java.security.*;
import java.security.spec.InvalidKeySpecException;

/**
 * 测试
 *
 * @author wangwei
 */
public class BlockchainTest {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void shouldVerify() throws RocksDBException, InvalidKeySpecException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, InvalidAlgorithmParameterException {
        var wallet = Wallet.createWallet();
        var data = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";
        var tx = Transaction.newCoinbaseTX(wallet.getAddress(), data);
        Assert.assertTrue(new Blockchain(new RocksDbBlockRepository()).verifyTransactions(tx));
    }

    public static void main(String[] args) {
        try {
//            var argss = new String[]{"createwallet"};
//            var argss = new String[]{"createblockchain", "-address", "1JCnyVZZZgPbhDM6bDn5LzgLcFQtAqEsZy"};
//            var argss = new String[]{"printaddresses"};
            var argss = new String[]{"printchain"};
//            var argss = new String[]{"getbalance", "-address", "1JCnyVZZZgPbhDM6bDn5LzgLcFQtAqEsZy"};
//            var argss = new String[]{"send", "-from", "1JCnyVZZZgPbhDM6bDn5LzgLcFQtAqEsZy", "-to", "1LWdJqVcEat6GiLRWyfB9ewTjdqGPrfeug", "-amount", "15"};
            new CLI(argss).parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
