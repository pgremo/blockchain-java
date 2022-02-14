package one.wangwei.blockchain.pow;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.wallet.Wallet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

public class ProofOfWorkTest {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void shouldMatch() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        var wallet = Wallet.createWallet();
        var data = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";
        var tx = Transaction.createCoinbaseTX(wallet.getAddress(), data);
        var block = Block.createGenesisBlock(tx).orElseThrow();
        Assert.assertTrue(Pow.validate(block));
    }
}