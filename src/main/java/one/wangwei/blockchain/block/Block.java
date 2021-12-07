package one.wangwei.blockchain.block;

import one.wangwei.blockchain.pow.ProofOfWork;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.Bytes;

import java.time.Instant;
import java.util.Optional;

/**
 * 区块
 *
 * @author wangwei
 * @date 2018/02/02
 */
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
    public static Optional<Block> newGenesisBlock(Transaction coinbase) {
        return Block.newBlock(Bytes.ZERO_HASH, new Transaction[] {coinbase});
    }

    /**
     * <p> 创建新区块 </p>
     *
     * @param previousHash
     * @param transactions
     * @return
     */
    public static Optional<Block> newBlock(String previousHash, Transaction[] transactions) {
        var block = new Block("", previousHash, transactions, Instant.now().getEpochSecond(), 0);
        var pow = ProofOfWork.newProofOfWork(block);
        return pow.run().map(x -> {
            block.setHash(Bytes.byteArrayToHex(x.hash()));
            block.setNonce(x.nonce());
            return block;
        });
    }

    /**
     * 区块hash值
     */
    @SuppressWarnings("all")
    public String getHash() {
        return this.hash;
    }

    /**
     * 前一个区块的hash值
     */
    @SuppressWarnings("all")
    public String getPrevBlockHash() {
        return this.prevBlockHash;
    }

    /**
     * 交易信息
     */
    @SuppressWarnings("all")
    public Transaction[] getTransactions() {
        return this.transactions;
    }

    /**
     * 区块创建时间(单位:秒)
     */
    @SuppressWarnings("all")
    public long getTimeStamp() {
        return this.timeStamp;
    }

    /**
     * 工作量证明计数器
     */
    @SuppressWarnings("all")
    public long getNonce() {
        return this.nonce;
    }

    /**
     * 区块hash值
     */
    @SuppressWarnings("all")
    public void setHash(final String hash) {
        this.hash = hash;
    }

    /**
     * 前一个区块的hash值
     */
    @SuppressWarnings("all")
    public void setPrevBlockHash(final String prevBlockHash) {
        this.prevBlockHash = prevBlockHash;
    }

    /**
     * 交易信息
     */
    @SuppressWarnings("all")
    public void setTransactions(final Transaction[] transactions) {
        this.transactions = transactions;
    }

    /**
     * 区块创建时间(单位:秒)
     */
    @SuppressWarnings("all")
    public void setTimeStamp(final long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * 工作量证明计数器
     */
    @SuppressWarnings("all")
    public void setNonce(final long nonce) {
        this.nonce = nonce;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Block)) return false;
        final Block other = (Block) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getTimeStamp() != other.getTimeStamp()) return false;
        if (this.getNonce() != other.getNonce()) return false;
        final Object this$hash = this.getHash();
        final Object other$hash = other.getHash();
        if (this$hash == null ? other$hash != null : !this$hash.equals(other$hash)) return false;
        final Object this$prevBlockHash = this.getPrevBlockHash();
        final Object other$prevBlockHash = other.getPrevBlockHash();
        if (this$prevBlockHash == null ? other$prevBlockHash != null : !this$prevBlockHash.equals(other$prevBlockHash)) return false;
        if (!java.util.Arrays.deepEquals(this.getTransactions(), other.getTransactions())) return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof Block;
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $timeStamp = this.getTimeStamp();
        result = result * PRIME + (int) ($timeStamp >>> 32 ^ $timeStamp);
        final long $nonce = this.getNonce();
        result = result * PRIME + (int) ($nonce >>> 32 ^ $nonce);
        final Object $hash = this.getHash();
        result = result * PRIME + ($hash == null ? 43 : $hash.hashCode());
        final Object $prevBlockHash = this.getPrevBlockHash();
        result = result * PRIME + ($prevBlockHash == null ? 43 : $prevBlockHash.hashCode());
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getTransactions());
        return result;
    }

    @SuppressWarnings("all")
    public Block(final String hash, final String prevBlockHash, final Transaction[] transactions, final long timeStamp, final long nonce) {
        this.hash = hash;
        this.prevBlockHash = prevBlockHash;
        this.transactions = transactions;
        this.timeStamp = timeStamp;
        this.nonce = nonce;
    }

    @SuppressWarnings("all")
    public Block() {
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "Block(hash=" + this.getHash() + ", prevBlockHash=" + this.getPrevBlockHash() + ", transactions=" + java.util.Arrays.deepToString(this.getTransactions()) + ", timeStamp=" + this.getTimeStamp() + ", nonce=" + this.getNonce() + ")";
    }
}
