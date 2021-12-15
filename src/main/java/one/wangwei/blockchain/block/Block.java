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
public record Block(String hash, String previousHash, Transaction[] transactions, long timeStamp, long nonce) {

    public static final String ZERO_HASH = Bytes.byteArrayToHex(Bytes.EMPTY_BYTES);

    /**
     * <p> 创建创世区块 </p>
     *
     * @param coinbase
     * @return
     */
    public static Optional<Block> newGenesisBlock(Transaction coinbase) {
        return Block.newBlock(ZERO_HASH, coinbase);
    }

    /**
     * <p> 创建新区块 </p>
     *
     * @param previousHash
     * @param transactions
     * @return
     */
    public static Optional<Block> newBlock(String previousHash, Transaction... transactions) {
        var now = Instant.now();
        var request = new PowRequest(previousHash, transactions, now);
        var pow = new Pow();
        return pow.run(request).map(x -> new Block(
                Bytes.byteArrayToHex(x.hash()),
                previousHash,
                transactions,
                now.toEpochMilli(),
                x.nonce()
        ));
    }

    @Override
    public String toString() {
        return "Block[" +
                "hash=" + hash +
                ", previousHash=" + previousHash +
                ", transactions=" + Arrays.toString(transactions) +
                ", timeStamp=" + timeStamp +
                ", nonce=" + nonce +
                ']';
    }
}
