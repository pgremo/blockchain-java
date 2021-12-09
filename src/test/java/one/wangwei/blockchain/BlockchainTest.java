package one.wangwei.blockchain;

import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.cli.CLI;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.wallet.Wallet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Test;

import java.security.Security;

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
    public void shouldVerify(){
        var wallet = Wallet.createWallet();
        var data = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";
        var tx = Transaction.newCoinbaseTX(wallet.getAddress(), data);
        Assert.assertTrue(Blockchain.initBlockchainFromDB().verifyTransactions(tx));
    }

    public static void main(String[] args) {
        try {
//            String[] argss = {"createwallet"};
            var argss = new String[]{"createblockchain", "-address", "1Aup3oweKvFd4upy2pZS9aKjn67SQgzbSC"};
            // 1CceyiwYXh6vL6dLPw6WiNc5ihqVxwYHSA
            // 1G9TkDEp9YTnGa6gS5zaWkwGQwKrRykXcf
            // 1EKacQPNxTd8N7Y83VK11zoqm7bhUZiDHm
//            String[] argss = {"printaddresses"};
//            var argss = new String[]{"printchain"};
//            var argss = new String[]{"getbalance", "-address", "1Aup3oweKvFd4upy2pZS9aKjn67SQgzbSC"};
//            var argss = new String[]{"send", "-from", "1Aup3oweKvFd4upy2pZS9aKjn67SQgzbSC", "-to", "1FfxJLfBGQuPN6vhaSoz278rfeC9GfAX2", "-amount", "5"};
            new CLI(argss).parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
