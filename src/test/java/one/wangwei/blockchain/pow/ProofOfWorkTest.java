package one.wangwei.blockchain.pow;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.wallet.Wallet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Test;

import java.security.Security;

public class ProofOfWorkTest {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void shouldMatch() {
        var wallet = Wallet.createWallet();
        var data = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";
        var tx = Transaction.newCoinbaseTX(wallet.getAddress(), data);
        var block = Block.newGenesisBlock(tx).orElseThrow();
        Assert.assertTrue(Pow.validate(block));
    }
}