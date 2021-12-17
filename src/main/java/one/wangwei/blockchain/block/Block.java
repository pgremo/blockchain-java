package one.wangwei.blockchain.block;

import one.wangwei.blockchain.pow.PowRequest;
import one.wangwei.blockchain.pow.Pow;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.Bytes;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

/**
 * 区块
 *
 * @author wangwei
 * @date 2018/02/02
 */
public record Block(BlockId id, BlockId previousId, Transaction[] transactions, Instant timeStamp, long nonce) {

    public static final BlockId NULL_ID = new BlockId(Bytes.EMPTY_BYTES);

    /**
     * <p> 创建创世区块 </p>
     *
     * @param coinbase
     * @return
     */
    public static Optional<Block> newGenesisBlock(Transaction coinbase) {
        return Block.newBlock(NULL_ID, coinbase);
    }

    /**
     * <p> 创建新区块 </p>
     *
     * @param previousHash
     * @param transactions
     * @return
     */
    public static Optional<Block> newBlock(BlockId previousHash, Transaction... transactions) {
        var now = Instant.now();
        var request = new PowRequest(previousHash, transactions, now);
        var pow = new Pow();
        return pow.run(request).map(x -> new Block(
                new BlockId(x.hash()),
                previousHash,
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
