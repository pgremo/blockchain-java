package one.wangwei.blockchain.pow;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.Bytes;
import one.wangwei.blockchain.util.Hashes;
import one.wangwei.blockchain.util.Numbers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
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

/**
 * 工作量证明
 *
 * @author wangwei
 * @date 2018/02/04
 */
public class ProofOfWork {
    @SuppressWarnings("all")
    private static final Logger logger = Logger.getLogger(ProofOfWork.class.getName());
    /**
     * 难度目标位
     */
    public static final int TARGET_BITS = 16;
    /**
     * 区块
     */
    private PoWRequest block;
    /**
     * 难度目标值
     */
    private static final BigInteger target = ONE.shiftLeft(256 - TARGET_BITS);

    /**
     * 创建新的工作量证明，设定难度目标值
     * <p>
     * 对1进行移位运算，将1向左移动 (256 - TARGET_BITS) 位，得到我们的难度目标值
     *
     * @param block
     * @return
     */
    public static ProofOfWork newProofOfWork(PoWRequest block) {
        return new ProofOfWork(block);
    }

    private byte[] hashTransactions() {
        var hashes = stream(block.transactions()).map(Transaction::hash).collect(toCollection(LinkedList::new));
        if (hashes.size() % 2 != 0) hashes.add(hashes.getLast());
        return iterate(hashes).poll();
    }

    private Deque<byte[]> iterate(Deque<byte[]> hashes) {
        if (hashes.size() == 1) return hashes;
        var next = new LinkedList<byte[]>();
        while (!hashes.isEmpty()) {
            next.add(hash(hashes.poll(), hashes.poll()));
        }
        return iterate(next);
    }

    private byte[] hash(byte[] left, byte[] right) {
        return right == null ? sha256(left) : sha256(left, right);
    }

    /**
     * 运行工作量证明，开始挖矿，找到小于难度目标值的Hash
     *
     * @return
     */
    public Optional<PowResult> run() {
        var start = now();
        return LongStream.range(0, Long.MAX_VALUE)
                .peek(x -> logger.info(() -> "POW running, nonce=%s".formatted(x)))
                .mapToObj(x -> new PowResult(x, sha256(prepareData(x))))
                .filter(x -> new BigInteger(1, x.hash()).compareTo(target) < 0)
                .peek(x -> {
                    logger.info(() -> "Elapsed Time: %s seconds ".formatted(between(start, now())));
                    logger.info(() -> "correct hash Hex: %s".formatted(x.hash()));
                })
                .findFirst();
    }

    /**
     * 验证区块是否有效
     *
     * @return
     */
    public static boolean validate(Block block) {
        return new BigInteger(Bytes.byteArrayToHex(Hashes.sha256(Numbers.toBytes(block.nonce()))), 16).compareTo(target) < 0;
    }

    /**
     * 准备数据
     * <p>
     * 注意：在准备区块数据时，一定要从原始数据类型转化为byte[]，不能直接从字符串进行转换
     *
     * @param nonce
     * @return
     */
    private byte[] prepareData(long nonce) {
        var prevBlockHashBytes = block.previousHash().isBlank() ?
                new byte[0] :
                new BigInteger(block.previousHash(), 16).toByteArray();

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            stream.write(prevBlockHashBytes);
            stream.write(hashTransactions());
            stream.write(Numbers.toBytes(block.stamp().getEpochSecond()));
            stream.write(Numbers.toBytes(TARGET_BITS));
            stream.write(Numbers.toBytes(nonce));
            return stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 区块
     */
    @SuppressWarnings("all")
    public PoWRequest getBlock() {
        return this.block;
    }

    /**
     * 难度目标值
     */
    @SuppressWarnings("all")
    public BigInteger getTarget() {
        return this.target;
    }

    /**
     * 区块
     */
    @SuppressWarnings("all")
    public void setBlock(final PoWRequest block) {
        this.block = block;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ProofOfWork)) return false;
        final ProofOfWork other = (ProofOfWork) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$block = this.getBlock();
        final Object other$block = other.getBlock();
        if (this$block == null ? other$block != null : !this$block.equals(other$block)) return false;
        final Object this$target = this.getTarget();
        final Object other$target = other.getTarget();
        if (this$target == null ? other$target != null : !this$target.equals(other$target)) return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof ProofOfWork;
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $block = this.getBlock();
        result = result * PRIME + ($block == null ? 43 : $block.hashCode());
        final Object $target = this.getTarget();
        result = result * PRIME + ($target == null ? 43 : $target.hashCode());
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "ProofOfWork(block=" + this.getBlock() + ", target=" + this.getTarget() + ")";
    }

    @SuppressWarnings("all")
    public ProofOfWork(final PoWRequest block) {
        this.block = block;
    }

    @SuppressWarnings("all")
    public ProofOfWork() {
    }
}
