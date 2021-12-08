package one.wangwei.blockchain.pow;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.Bytes;
import one.wangwei.blockchain.util.Numbers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Deque;
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

public class ProofOfWork {
    private static final Logger logger = Logger.getLogger(ProofOfWork.class.getName());
    private static final int TARGET_BITS = 16;
    private static final BigInteger target = ONE.shiftLeft(256 - TARGET_BITS);

    public Optional<PowResult> run(PoWRequest request) {
        var start = now();
        return LongStream.range(0, Long.MAX_VALUE)
                .peek(x -> logger.info(() -> "POW running, nonce=%s".formatted(x)))
                .mapToObj(x -> new PowResult(x, sha256(prepareData(request, x))))
                .filter(x -> new BigInteger(1, x.hash()).compareTo(target) < 0)
                .peek(x -> {
                    logger.info(() -> "Elapsed Time: %s seconds ".formatted(between(start, now())));
                    logger.info(() -> "correct hash Hex: %s".formatted(Bytes.byteArrayToHex(x.hash())));
                })
                .findFirst();
    }

    public static boolean validate(Block block) {
        PoWRequest request = new PoWRequest(block.previousHash(), block.transactions().toArray(Transaction[]::new), Instant.ofEpochMilli(block.timeStamp()));
        return new BigInteger(1, sha256(prepareData(request, block.nonce()))).compareTo(target) < 0;
    }

    private static byte[] prepareData(PoWRequest request, long nonce) {
        var prevBlockHashBytes = request.previousHash().isBlank() ?
                new byte[0] :
                new BigInteger(request.previousHash(), 16).toByteArray();

        try (var stream = new ByteArrayOutputStream()) {
            stream.write(prevBlockHashBytes);
            stream.write(hashTransactions(request));
            stream.write(Numbers.toBytes(request.stamp().toEpochMilli()));
            stream.write(Numbers.toBytes(TARGET_BITS));
            stream.write(Numbers.toBytes(nonce));
            return stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] hashTransactions(PoWRequest request) {
        var hashes = stream(request.transactions()).map(Transaction::hash).collect(toCollection(LinkedList::new));
        if (hashes.size() % 2 != 0) hashes.add(hashes.getLast());
        return iterate(hashes).poll();
    }

    private static Deque<byte[]> iterate(Deque<byte[]> hashes) {
        if (hashes.size() == 1) return hashes;
        var next = new LinkedList<byte[]>();
        while (!hashes.isEmpty()) {
            next.add(hash(hashes.poll(), hashes.poll()));
        }
        return iterate(next);
    }

    private static byte[] hash(byte[] left, byte[] right) {
        return right == null ? sha256(left) : sha256(left, right);
    }
}
