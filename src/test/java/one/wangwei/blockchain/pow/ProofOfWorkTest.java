package one.wangwei.blockchain.pow;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.wallet.Wallet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.Security;

import static one.wangwei.blockchain.wallet.Address.Version.Prod;

public class ProofOfWorkTest {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void shouldMatch() {
        var wallet = Wallet.createWallet(Prod);
        var data = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";
        var tx = Transaction.createCoinbaseTX(wallet.getAddress(), data);
        var block = Block.createGenesisBlock(tx).orElseThrow();
        Assertions.assertTrue(Pow.validate(block));
    }
}