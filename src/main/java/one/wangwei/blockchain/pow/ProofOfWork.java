package one.wangwei.blockchain.pow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.ByteUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.LongStream;

import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;
import static one.wangwei.blockchain.util.Hashes.sha256;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

/**
 * 工作量证明
 *
 * @author wangwei
 * @date 2018/02/04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class ProofOfWork {

    /**
     * 难度目标位
     */
    public static final int TARGET_BITS = 16;

    /**
     * 区块
     */
    private Block block;
    /**
     * 难度目标值
     */
    private BigInteger target;


    /**
     * 创建新的工作量证明，设定难度目标值
     * <p>
     * 对1进行移位运算，将1向左移动 (256 - TARGET_BITS) 位，得到我们的难度目标值
     *
     * @param block
     * @return
     */
    public static ProofOfWork newProofOfWork(Block block) {
        var targetValue = BigInteger.valueOf(1).shiftLeft((256 - TARGET_BITS));
        return new ProofOfWork(block, targetValue);
    }

    private byte[] hashTransactions() {
        var hashes = stream(block.getTransactions()).map(Transaction::hash).collect(toCollection(LinkedList::new));
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
        var startTime = now();
        return LongStream
                .range(0, Long.MAX_VALUE)
                .peek(x -> log.info("POW running, nonce={}", x))
                .mapToObj(x -> new PowResult(x, sha256(prepareData(x))))
                .filter(x -> new BigInteger(1, x.hash()).compareTo(target) < 0)
                .peek(x -> {
                    log.info("Elapsed Time: {} seconds \n", between(startTime, now()));
                    log.info("correct hash Hex: {} \n", x.hash());
                })
                .findFirst();
    }

    /**
     * 验证区块是否有效
     *
     * @return
     */
    public boolean validate() {
        return new BigInteger(
                sha256Hex(prepareData(block.getNonce())),
                16
        ).compareTo(target) < 0;
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
        var prevBlockHashBytes = StringUtils.isNoneBlank(block.getPrevBlockHash()) ?
                new BigInteger(block.getPrevBlockHash(), 16).toByteArray() :
                new byte[0];

        return ByteUtils.merge(
                prevBlockHashBytes,
                hashTransactions(),
                ByteUtils.toBytes(block.getTimeStamp()),
                ByteUtils.toBytes(TARGET_BITS),
                ByteUtils.toBytes(nonce)
        );
    }

}
