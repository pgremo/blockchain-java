package one.wangwei.blockchain.block;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import one.wangwei.blockchain.pow.ProofOfWork;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.ByteUtils;

import java.time.Instant;
import java.util.LinkedList;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;
import static one.wangwei.blockchain.util.Hashes.sha256;

/**
 * 区块
 *
 * @author wangwei
 * @date 2018/02/02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Block {

    /**
     * 区块hash值
     */
    private String hash;
    /**
     * 前一个区块的hash值
     */
    private String prevBlockHash;
    /**
     * 交易信息
     */
    private Transaction[] transactions;
    /**
     * 区块创建时间(单位:秒)
     */
    private long timeStamp;
    /**
     * 工作量证明计数器
     */
    private long nonce;

    /**
     * <p> 创建创世区块 </p>
     *
     * @param coinbase
     * @return
     */
    public static Block newGenesisBlock(Transaction coinbase) {
        return Block.newBlock(ByteUtils.ZERO_HASH, new Transaction[]{coinbase});
    }

    /**
     * <p> 创建新区块 </p>
     *
     * @param previousHash
     * @param transactions
     * @return
     */
    public static Block newBlock(String previousHash, Transaction[] transactions) {
        var block = new Block("", previousHash, transactions, Instant.now().getEpochSecond(), 0);
        var pow = ProofOfWork.newProofOfWork(block);
        var powResult = pow.run();
        block.setHash(powResult.getHash());
        block.setNonce(powResult.getNonce());
        return block;
    }

    public byte[] hashTransactions() {
        LinkedList<byte[]> hashes = stream(getTransactions()).map(Transaction::hash).collect(toCollection(LinkedList::new));
        if (hashes.size() % 2 != 0) hashes.add(hashes.getLast());
        return iterate(hashes).poll();
    }

    public LinkedList<byte[]> iterate(LinkedList<byte[]> hashes) {
        if (hashes.size() == 1) return hashes;
        var next = new LinkedList<byte[]>();
        while (!hashes.isEmpty()) {
            next.add(hash(hashes.poll(), hashes.poll()));
        }
        return iterate(next);
    }

    private byte[] hash(byte[] left, byte[] right) {
        if (right == null) return sha256(left);
        else return sha256(left, right);
    }
}
