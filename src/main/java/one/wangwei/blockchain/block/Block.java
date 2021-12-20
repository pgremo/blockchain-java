package one.wangwei.blockchain.block;

import one.wangwei.blockchain.pow.Pow;
import one.wangwei.blockchain.pow.PowRequest;
import one.wangwei.blockchain.transaction.Transaction;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

public record Block(BlockId id, BlockId previousId, Transaction[] transactions, Instant timeStamp, long nonce) {

    public static Optional<Block> newGenesisBlock(Transaction coinbase) {
        return Block.newBlock(BlockId.Null, coinbase);
    }

    public static Optional<Block> newBlock(BlockId previousId, Transaction... transactions) {
        var now = Instant.now();
        var request = new PowRequest(previousId, transactions, now);
        var pow = new Pow();
        return pow.run(request).map(x -> new Block(
                new BlockId(x.hash()),
                previousId,
                transactions,
                now,
                x.nonce()
        ));
    }

    @Override
    public String toString() {
        return "Block[" +
                "id=" + id +
                ", previousId=" + previousId +
                ", transactions=" + Arrays.toString(transactions) +
                ", timeStamp=" + timeStamp +
                ", nonce=" + nonce +
                ']';
    }
}
