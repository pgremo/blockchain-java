package one.wangwei.blockchain.transaction;

import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.util.BtcAddressUtils;
import one.wangwei.blockchain.util.Hashes;
import one.wangwei.blockchain.util.Numbers;
import one.wangwei.blockchain.wallet.Wallet;
import one.wangwei.blockchain.wallet.WalletUtils;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.*;

import static java.util.stream.Collectors.toCollection;
import static one.wangwei.blockchain.util.MerkleRoot.merkleRoot;

/**
 * 交易
 *
 * @author wangwei
 * @date 2017/03/04
 */
public class Transaction {
    private static final int SUBSIDY = 10;
    /**
     * 交易的Hash
     */
    private TransactionId id;
    /**
     * 交易输入
     */
    private final TXInput[] inputs;
    /**
     * 交易输出
     */
    private final TXOutput[] outputs;
    /**
     * 创建日期
     */
    private final Instant created;

    /**
     * 计算交易信息的Hash值
     *
     * @return
     */
    public byte[] hash() {
        return Hashes.sha256(
                merkleRoot(Arrays.stream(getInputs()).map(TXInput::hash).collect(toCollection(LinkedList::new))),
                merkleRoot(Arrays.stream(getOutputs()).map(TXOutput::hash).collect(toCollection(LinkedList::new))),
                Numbers.toBytes(getCreated().toEpochMilli())
        );
    }

    /**
     * 创建CoinBase交易
     *
     * @param to   收账的钱包地址
     * @param data 解锁脚本数据
     * @return
     */
    public static Transaction newCoinbaseTX(String to, String data) {
        if (data.isBlank()) data = String.format("Reward to '%s'", to);
        // 创建交易输入
        var txInput = new TXInput(new TransactionId(new byte[0]), -1, null, data.getBytes());
        // 创建交易输出
        var txOutput = TXOutput.newTXOutput(SUBSIDY, to);
        // 创建交易
        var tx = new Transaction(null, new TXInput[]{txInput}, new TXOutput[]{txOutput}, Instant.now());
        // 设置交易ID
        tx.setId(new TransactionId(tx.hash()));
        return tx;
    }

    /**
     * 是否为 Coinbase 交易
     *
     * @return
     */
    public boolean isCoinbase() {
        return getInputs().length == 1 && getInputs()[0].getTxId().value().length == 0 && getInputs()[0].getTxOutputIndex() == -1;
    }

    record TxIoReference(TransactionId txId, int index) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TxIoReference that = (TxIoReference) o;
            return index == that.index && txId.equals(that.txId);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(index);
            result = 31 * result + txId.hashCode();
            return result;
        }
    }

    public static Transaction create(String from, String to, int amount, Blockchain chain) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException {
        var fromWallet = WalletUtils.getInstance().getWallet(from);
        var result = getUnspent(amount, chain, fromWallet);
        if (result.total() < amount) throw new RuntimeException("insufficient funds");

        var inputs = result.unspent().stream()
                .map(x -> new TXInput(x.txId(), x.index(), null, fromWallet.publicKey().getEncoded()))
                .toArray(TXInput[]::new);

        var toWallet = WalletUtils.getInstance().getWallet(to);
        var toPubKey = toWallet.publicKey().getEncoded();
        var toPubKeyHash = BtcAddressUtils.ripeMD160Hash(toPubKey);
        var first = new TXOutput(amount, toPubKeyHash);
        var outputs = result.total() > amount ?
                new TXOutput[]{first, new TXOutput(result.total() - amount, BtcAddressUtils.ripeMD160Hash(fromWallet.publicKey().getEncoded()))} :
                new TXOutput[]{first};

        var tx = new Transaction(null, inputs, outputs, Instant.now());
        tx.setId(new TransactionId(tx.hash()));

        chain.signTransaction(tx, fromWallet.privateKey());

        return tx;
    }

    public static UnspentResult getUnspent(int amount, Blockchain chain, Wallet fromWallet) {
        var fromPubKey = fromWallet.publicKey().getEncoded();
        var fromPubKeyHash = BtcAddressUtils.ripeMD160Hash(fromPubKey);
        var spent = new LinkedList<TxIoReference>();

        var unspent = new LinkedList<TxIoReference>();
        var total = 0;

        processBlocks:
        for (var block : chain) {
            for (var transaction : block.transactions()) {
                var outputs = transaction.getOutputs();
                for (var index = 0; index < outputs.length; index++) {
                    var output = outputs[index];

                    // continue to the next output if this one is not of the sender
                    if (!Arrays.equals(output.pubKeyHash(), fromPubKeyHash)) continue;

                    var reference = new TxIoReference(transaction.getId(), index);

                    // continue to the next output if this one is spent
                    if (spent.remove(reference)) continue;

                    // add valid to list of unspent
                    unspent.add(reference);
                    // update total
                    total += output.value();
                    // if we have enough then break all the way out
                    if (total >= amount) break processBlocks;
                }
                // accumulate transaction inputs of sender
                for (var input : transaction.getInputs()) {
                    if (Arrays.equals(input.getPubKey(), fromPubKey)) {
                        spent.add(new TxIoReference(input.getTxId(), input.getTxOutputIndex()));
                    }
                }
            }
        }
        return new UnspentResult(total, unspent);
    }


    /**
     * 创建用于签名的交易数据副本，交易输入的 signature 和 pubKey 需要设置为null
     *
     * @return
     */
    public Transaction trimmedCopy() {
        var tmpTXInputs = new TXInput[getInputs().length];
        for (var i = 0; i < getInputs().length; i++) {
            var txInput = getInputs()[i];
            tmpTXInputs[i] = new TXInput(txInput.getTxId(), txInput.getTxOutputIndex(), null, null);
        }
        var tmpTXOutputs = new TXOutput[getOutputs().length];
        for (var i = 0; i < getOutputs().length; i++) {
            var txOutput = getOutputs()[i];
            tmpTXOutputs[i] = new TXOutput(txOutput.value(), txOutput.pubKeyHash());
        }
        return new Transaction(getId(), tmpTXInputs, tmpTXOutputs, getCreated());
    }

    /**
     * 签名
     *
     * @param privateKey 私钥
     * @param prevTxMap  前面多笔交易集合
     */
    public void sign(PrivateKey privateKey, Map<TransactionId, Transaction> prevTxMap) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchProviderException {
        if (isCoinbase()) return;
        for (var txInput : getInputs()) {
            if (!prevTxMap.containsKey(txInput.getTxId()))
                throw new RuntimeException("ERROR: Previous transaction is not correct");
        }
        var txCopy = trimmedCopy();
        var signature = Signature.getInstance("SHA256withECDSA", "SunEC");
        signature.initSign(privateKey);
        for (var i = 0; i < txCopy.getInputs().length; i++) {
            var txInputCopy = txCopy.getInputs()[i];
            var prevTx = prevTxMap.get(txInputCopy.getTxId());
            var prevTxOutput = prevTx.getOutputs()[txInputCopy.getTxOutputIndex()];
            txInputCopy.setPubKey(prevTxOutput.pubKeyHash());
            txInputCopy.setSignature(null);
            signature.update(txCopy.hash());
            getInputs()[i].setSignature(signature.sign());
            txInputCopy.setPubKey(null);
        }
    }

    /**
     * 验证交易信息
     *
     * @param prevTxMap 前面多笔交易集合
     * @return
     */
    public boolean verify(Map<TransactionId, Transaction> prevTxMap) throws InvalidKeySpecException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, NoSuchProviderException {
        if (this.isCoinbase()) return true;
        for (var txInput : getInputs()) {
            if (!prevTxMap.containsKey(txInput.getTxId()))
                throw new RuntimeException("ERROR: Previous transaction is not correct");
        }
        var txCopy = trimmedCopy();
        var keyFactory = KeyFactory.getInstance("EC", "SunEC");
        var signature = Signature.getInstance("SHA256withECDSA", "SunEC");
        for (var i = 0; i < getInputs().length; i++) {
            var txInput = getInputs()[i];
            var prevTx = prevTxMap.get(txInput.getTxId());
            var prevTxOutput = prevTx.getOutputs()[txInput.getTxOutputIndex()];
            var txInputCopy = txCopy.getInputs()[i];
            txInputCopy.setSignature(null);
            txInputCopy.setPubKey(prevTxOutput.pubKeyHash());
            txCopy.setId(new TransactionId(txCopy.hash()));
            txInputCopy.setPubKey(null);
            var publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(txInput.getPubKey()));
            signature.initVerify(publicKey);
            signature.update(txCopy.getId().value());
            if (!signature.verify(txInput.getSignature())) return false;
        }
        return true;
    }

    /**
     * 交易的Hash
     * @return
     */
    public TransactionId getId() {
        return this.id;
    }

    /**
     * 交易输入
     */
    public TXInput[] getInputs() {
        return this.inputs;
    }

    /**
     * 交易输出
     */
    public TXOutput[] getOutputs() {
        return this.outputs;
    }

    /**
     * 创建日期
     *
     * @return
     */
    public Instant getCreated() {
        return this.created;
    }

    /**
     * 交易的Hash
     * @param id
     */
    public void setId(final TransactionId id) {
        this.id = id;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof final Transaction other)) return false;
        if (!other.canEqual(this)) return false;
        if (this.getCreated() != other.getCreated()) return false;
        if (!this.getId().equals(other.getId())) return false;
        if (!Arrays.deepEquals(this.getInputs(), other.getInputs())) return false;
        return Arrays.deepEquals(this.getOutputs(), other.getOutputs());
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Transaction;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $createTime = this.getCreated().toEpochMilli();
        result = result * PRIME + (int) ($createTime >>> 32 ^ $createTime);
        result = result * PRIME + this.getId().hashCode();
        result = result * PRIME + Arrays.deepHashCode(this.getInputs());
        result = result * PRIME + Arrays.deepHashCode(this.getOutputs());
        return result;
    }

    @Override
    public String toString() {
        return "Transaction[txId=" + id + ", inputs=" + Arrays.deepToString(this.getInputs()) + ", outputs=" + Arrays.deepToString(this.getOutputs()) + ", createTime=" + this.getCreated() + "]";
    }

    public Transaction(final TransactionId id, final TXInput[] inputs, final TXOutput[] outputs, final Instant created) {
        this.id = id;
        this.inputs = inputs;
        this.outputs = outputs;
        this.created = created;
    }
}
