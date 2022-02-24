package one.wangwei.blockchain.pow;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.Numbers;

import java.math.BigInteger;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.LongStream;

import static java.lang.System.Logger.Level.INFO;
import static java.math.BigInteger.ONE;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;
import static one.wangwei.blockchain.util.Hashes.sha256;
import static one.wangwei.blockchain.util.MerkleRoot.merkleRoot;

public class Pow {
    private static final System.Logger logger = System.getLogger(Pow.class.getName());
    private static final int TARGET_BITS = 16;
    private static final BigInteger target = ONE.shiftLeft(256 - TARGET_BITS);

    public static Optional<Block> createBlock(Block.Id previousId, Transaction... transactions) {
        var start = now();
        return LongStream.range(0, Long.MAX_VALUE)
                .peek(x -> logger.log(INFO, () -> "POW running, nonce=%s".formatted(x)))
                .mapToObj(x -> {
                    var now = now();
                    return new Block(
                            new Block.Id(hash(x, previousId, transactions, now)),
                            previousId,
                            transactions,
                            now,
                            x
                    );
                })
                .filter(x -> new BigInteger(1, x.id().value()).compareTo(target) < 0)
                .peek(x -> {
                    logger.log(INFO, () -> "Elapsed Time: %s seconds ".formatted(between(start, now())));
                    logger.log(INFO, () -> "correct hash Hex: %s".formatted(x.id()));
                })
                .findFirst();
    }

    public static boolean validate(Block block) {
        return new BigInteger(1, hash(block.nonce(), block.previousId(), block.transactions(), block.timeStamp())).compareTo(target) < 0;
    }

    private static byte[] hash(long nonce, Block.Id id, Transaction[] transactions, Instant stamp) {
        return sha256(
                new BigInteger(1, id.value()).toByteArray(),
                merkleRoot(stream(transactions).map(Transaction::hash).collect(toCollection(LinkedList::new))),
                Numbers.toBytes(stamp.toEpochMilli()),
                Numbers.toBytes(TARGET_BITS),
                Numbers.toBytes(nonce)
        );
    }
}
