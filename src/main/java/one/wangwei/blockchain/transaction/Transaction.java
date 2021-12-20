package one.wangwei.blockchain.transaction;

import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.util.BtcAddressUtils;
import one.wangwei.blockchain.util.Bytes;
import one.wangwei.blockchain.util.Hashes;
import one.wangwei.blockchain.util.Numbers;
import one.wangwei.blockchain.wallet.Wallet;
import one.wangwei.blockchain.wallet.WalletUtils;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.*;

import static java.lang.System.Logger.Level.ERROR;
import static java.util.stream.Collectors.toCollection;
import static one.wangwei.blockchain.util.MerkleRoot.merkleRoot;

/**
 * 交易
 *
 * @author wangwei
 * @date 2017/03/04
 */
public class Transaction {
    private static final System.Logger logger = System.getLogger(Transaction.class.getName());
    private static final int SUBSIDY = 10;
    /**
     * 交易的Hash
     */
    private byte[] txId;
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
        var txInput = new TXInput(new byte[0], -1, null, data.getBytes());
        // 创建交易输出
        var txOutput = TXOutput.newTXOutput(SUBSIDY, to);
        // 创建交易
        var tx = new Transaction(null, new TXInput[]{txInput}, new TXOutput[]{txOutput}, Instant.now());
        // 设置交易ID
        tx.setTxId(tx.hash());
        return tx;
    }

    /**
     * 是否为 Coinbase 交易
     *
     * @return
     */
    public boolean isCoinbase() {
        return getInputs().length == 1 && getInputs()[0].getId().length == 0 && getInputs()[0].getTxOutputIndex() == -1;
    }

    record TxIoReference(byte[] txId, int index) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TxIoReference that = (TxIoReference) o;
            return index == that.index && Arrays.equals(txId, that.txId);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(index);
            result = 31 * result + Arrays.hashCode(txId);
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
        tx.setTxId(tx.hash());

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

                    var reference = new TxIoReference(transaction.getTxId(), index);

                    // continue to the next output if this one is spent
                    if (spent.remove(reference)) continue;

                    // add valid to list of unspent
                    unspent.add(reference);
                    // update total
                    total += output.value();
                    // if we have enough then break all the way out
                    if (total >= amount) {
                        break processBlocks;
                    }
                }
                // accumulate transaction inputs of sender
                for (var input : transaction.getInputs()) {
                    if (Arrays.equals(input.getPubKey(), fromPubKey)) {
                        spent.add(new TxIoReference(input.getId(), input.getTxOutputIndex()));
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
        var tmpTXInputs = new TXInput[this.getInputs().length];
        for (var i = 0; i < this.getInputs().length; i++) {
            var txInput = this.getInputs()[i];
            tmpTXInputs[i] = new TXInput(txInput.getId(), txInput.getTxOutputIndex(), null, null);
        }
        var tmpTXOutputs = new TXOutput[this.getOutputs().length];
        for (var i = 0; i < this.getOutputs().length; i++) {
            var txOutput = this.getOutputs()[i];
            tmpTXOutputs[i] = new TXOutput(txOutput.value(), txOutput.pubKeyHash());
        }
        return new Transaction(this.getTxId(), tmpTXInputs, tmpTXOutputs, this.getCreated());
    }

    /**
     * 签名
     *
     * @param privateKey 私钥
     * @param prevTxMap  前面多笔交易集合
     */
    public void sign(PrivateKey privateKey, Map<byte[], Transaction> prevTxMap) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchProviderException {
        // coinbase 交易信息不需要签名，因为它不存在交易输入信息
        if (this.isCoinbase()) {
            return;
        }
        // 再次验证一下交易信息中的交易输入是否正确，也就是能否查找对应的交易数据
        for (var txInput : this.getInputs()) {
            if (!prevTxMap.containsKey(txInput.getId()))
                throw new RuntimeException("ERROR: Previous transaction is not correct");
        }
        // 创建用于签名的交易信息的副本
        var txCopy = this.trimmedCopy();
        var ecdsaSign = Signature.getInstance("SHA256withECDSA", "SunEC");
        ecdsaSign.initSign(privateKey);
        for (var i = 0; i < txCopy.getInputs().length; i++) {
            var txInputCopy = txCopy.getInputs()[i];
            // 获取交易输入TxID对应的交易数据
            var prevTx = prevTxMap.get(txInputCopy.getId());
            // 获取交易输入所对应的上一笔交易中的交易输出
            var prevTxOutput = prevTx.getOutputs()[txInputCopy.getTxOutputIndex()];
            txInputCopy.setPubKey(prevTxOutput.pubKeyHash());
            txInputCopy.setSignature(null);
            // 得到要签名的数据，即交易ID
            txCopy.setTxId(txCopy.hash());
            txInputCopy.setPubKey(null);
            // 对整个交易信息仅进行签名，即对交易ID进行签名
            ecdsaSign.update(txCopy.getTxId());
            var signature = ecdsaSign.sign();
            // 将整个交易数据的签名赋值给交易输入，因为交易输入需要包含整个交易信息的签名
            // 注意是将得到的签名赋值给原交易信息中的交易输入
            this.getInputs()[i].setSignature(signature);
        }
    }

    /**
     * 验证交易信息
     *
     * @param prevTxMap 前面多笔交易集合
     * @return
     */
    public boolean verify(TreeMap<byte[], Transaction> prevTxMap) {
        // coinbase 交易信息不需要签名，也就无需验证
        if (this.isCoinbase()) {
            return true;
        }
        // 再次验证一下交易信息中的交易输入是否正确，也就是能否查找对应的交易数据
        for (var txInput : this.getInputs()) {
            if (!prevTxMap.containsKey(txInput.getId()))
                throw new RuntimeException("ERROR: Previous transaction is not correct");
        }
        // 创建用于签名验证的交易信息的副本
        try {
            var txCopy = this.trimmedCopy();
            var keyFactory = KeyFactory.getInstance("EC", "SunEC");
            var ecdsaVerify = Signature.getInstance("SHA256withECDSA", "SunEC");
            for (var i = 0; i < this.getInputs().length; i++) {
                var txInput = this.getInputs()[i];
                // 获取交易输入TxID对应的交易数据
                var prevTx = prevTxMap.get(txInput.getId());
                // 获取交易输入所对应的上一笔交易中的交易输出
                var prevTxOutput = prevTx.getOutputs()[txInput.getTxOutputIndex()];
                var txInputCopy = txCopy.getInputs()[i];
                txInputCopy.setSignature(null);
                txInputCopy.setPubKey(prevTxOutput.pubKeyHash());
                // 得到要签名的数据，即交易ID
                txCopy.setTxId(txCopy.hash());
                txInputCopy.setPubKey(null);
                var publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(txInput.getPubKey()));
                ecdsaVerify.initVerify(publicKey);
                ecdsaVerify.update(txCopy.getTxId());
                if (!ecdsaVerify.verify(txInput.getSignature())) {
                    return false;
                }
            }
            return true;
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | InvalidKeySpecException | NoSuchProviderException e) {
            logger.log(ERROR, "Fail to verify transaction ! transaction invalid ! ", e);
            return false;
        }
    }

    /**
     * 交易的Hash
     */
    public byte[] getTxId() {
        return this.txId;
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
     */
    public void setTxId(final byte[] txId) {
        this.txId = txId;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof final Transaction other)) return false;
        if (!other.canEqual(this)) return false;
        if (this.getCreated() != other.getCreated()) return false;
        if (!Arrays.equals(this.getTxId(), other.getTxId())) return false;
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
        result = result * PRIME + Arrays.hashCode(this.getTxId());
        result = result * PRIME + Arrays.deepHashCode(this.getInputs());
        result = result * PRIME + Arrays.deepHashCode(this.getOutputs());
        return result;
    }

    @Override
    public String toString() {
        return "Transaction[txId=" + Bytes.byteArrayToHex(this.getTxId()) + ", inputs=" + Arrays.deepToString(this.getInputs()) + ", outputs=" + Arrays.deepToString(this.getOutputs()) + ", createTime=" + this.getCreated() + "]";
    }

    public Transaction(final byte[] txId, final TXInput[] inputs, final TXOutput[] outputs, final Instant created) {
        this.txId = txId;
        this.inputs = inputs;
        this.outputs = outputs;
        this.created = created;
    }
}
