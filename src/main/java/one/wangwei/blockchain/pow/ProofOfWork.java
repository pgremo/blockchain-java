package one.wangwei.blockchain.pow;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.Bytes;
import one.wangwei.blockchain.util.Numbers;

import java.math.BigInteger;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.LongStream;

import static java.math.BigInteger.ONE;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;
import static one.wangwei.blockchain.util.Hashes.sha256;
import static one.wangwei.blockchain.util.MerkleRoot.merkleRoot;

public class ProofOfWork {
    private static final Logger logger = Logger.getLogger(ProofOfWork.class.getName());
    private static final int TARGET_BITS = 16;
    private static final BigInteger target = ONE.shiftLeft(256 - TARGET_BITS);

    public Optional<PowResult> run(PoWRequest request) {
        var start = now();
        return LongStream.range(0, Long.MAX_VALUE)
                .peek(x -> logger.info(() -> "POW running, nonce=%s".formatted(x)))
                .mapToObj(x -> new PowResult(x, prepareData(request, x)))
                .filter(x -> new BigInteger(1, x.hash()).compareTo(target) < 0)
                .peek(x -> {
                    logger.info(() -> "Elapsed Time: %s seconds ".formatted(between(start, now())));
                    logger.info(() -> "correct hash Hex: %s".formatted(Bytes.byteArrayToHex(x.hash())));
                })
                .findFirst();
    }

    public static boolean validate(Block block) {
        var request = new PoWRequest(block.previousHash(), block.transactions(), Instant.ofEpochMilli(block.timeStamp()));
        return new BigInteger(1, prepareData(request, block.nonce())).compareTo(target) < 0;
    }

    private static byte[] prepareData(PoWRequest request, long nonce) {
        var prevBlockHashBytes = request.previousHash().isBlank() ?
                new byte[0] :
                new BigInteger(request.previousHash(), 16).toByteArray();

        return sha256(
                prevBlockHashBytes,
                merkleRoot(stream(request.transactions()).map(Transaction::hash).collect(toCollection(LinkedList::new))),
                Numbers.toBytes(request.stamp().toEpochMilli()),
                Numbers.toBytes(TARGET_BITS),
                Numbers.toBytes(nonce)
        );
    }
}
