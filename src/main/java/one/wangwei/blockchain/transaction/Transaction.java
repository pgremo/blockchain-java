package one.wangwei.blockchain.transaction;

import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.util.BtcAddressUtils;
import one.wangwei.blockchain.util.Bytes;
import one.wangwei.blockchain.util.Hashes;
import one.wangwei.blockchain.util.Numbers;
import one.wangwei.blockchain.wallet.WalletUtils;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toCollection;
import static one.wangwei.blockchain.util.MerkleRoot.merkleRoot;

/**
 * 交易
 *
 * @author wangwei
 * @date 2017/03/04
 */
public class Transaction {
    private static final Logger logger = Logger.getLogger(Transaction.class.getName());
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
    private final long createTime;

    /**
     * 计算交易信息的Hash值
     *
     * @return
     */
    public byte[] hash() {
        return Hashes.sha256(
                merkleRoot(Arrays.stream(inputs).map(TXInput::hash).collect(toCollection(LinkedList::new))),
                merkleRoot(Arrays.stream(outputs).map(TXOutput::hash).collect(toCollection(LinkedList::new))),
                Numbers.toBytes(createTime)
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
        if (data.isBlank()) data = String.format("Reward to \'%s\'", to);
        // 创建交易输入
        var txInput = new TXInput(new byte[0], -1, null, data.getBytes());
        // 创建交易输出
        var txOutput = TXOutput.newTXOutput(SUBSIDY, to);
        // 创建交易
        var tx = new Transaction(null, new TXInput[]{txInput}, new TXOutput[]{txOutput}, System.currentTimeMillis());
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
        return this.getInputs().length == 1 && this.getInputs()[0].getTxId().length == 0 && this.getInputs()[0].getTxOutputIndex() == -1;
    }

    /**
     * 从 from 向  to 支付一定的 amount 的金额
     *
     * @param from       支付钱包地址
     * @param to         收款钱包地址
     * @param amount     交易金额
     * @param blockchain 区块链
     * @return
     */
    public static Transaction newUTXOTransaction(String from, String to, int amount, Blockchain blockchain) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException {
        // 获取钱包
        var senderWallet = WalletUtils.getInstance().getWallet(from);
        var pubKey = senderWallet.getPublicKey();
        var pubKeyHash = BtcAddressUtils.ripeMD160Hash(pubKey);
        var result = new UTXOSet(blockchain).findSpendableOutputs(pubKeyHash, amount);
        var accumulated = result.accumulated();
        var unspentOuts = result.unspentOuts();
        if (accumulated < amount) {
            logger.severe(() -> "ERROR: Not enough funds ! accumulated=%s, amount=%s".formatted(accumulated, amount));
            throw new RuntimeException("ERROR: Not enough funds ! ");
        }
        var txInputs = new LinkedList<TXInput>();
        for (var entry : unspentOuts.entrySet()) {
            var txIdStr = entry.getKey();
            var outIds = entry.getValue();
            // TODO:  should just be byte[]'s all around
            var txId = Bytes.hexToByteArray(txIdStr).orElseThrow();
            for (var outIndex : outIds) {
                txInputs.add(new TXInput(txId, outIndex, null, pubKey));
            }
        }
        var txOutput = new LinkedList<TXOutput>();
        txOutput.add(TXOutput.newTXOutput(amount, to));
        if (accumulated > amount) {
            txOutput.add(TXOutput.newTXOutput(accumulated - amount, from));
        }
        var newTx = new Transaction(null, txInputs.toArray(TXInput[]::new), txOutput.toArray(TXOutput[]::new), System.currentTimeMillis());
        newTx.setTxId(newTx.hash());
        // 进行交易签名
        blockchain.signTransaction(newTx, senderWallet.getPrivateKey());
        return newTx;
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
            tmpTXInputs[i] = new TXInput(txInput.getTxId(), txInput.getTxOutputIndex(), null, null);
        }
        var tmpTXOutputs = new TXOutput[this.getOutputs().length];
        for (var i = 0; i < this.getOutputs().length; i++) {
            var txOutput = this.getOutputs()[i];
            tmpTXOutputs[i] = new TXOutput(txOutput.value(), txOutput.pubKeyHash());
        }
        return new Transaction(this.getTxId(), tmpTXInputs, tmpTXOutputs, this.getCreateTime());
    }

    /**
     * 签名
     *
     * @param privateKey 私钥
     * @param prevTxMap  前面多笔交易集合
     */
    public void sign(PrivateKey privateKey, Map<byte[], Optional<Transaction>> prevTxMap) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchProviderException {
        // coinbase 交易信息不需要签名，因为它不存在交易输入信息
        if (this.isCoinbase()) {
            return;
        }
        // 再次验证一下交易信息中的交易输入是否正确，也就是能否查找对应的交易数据
        for (var txInput : this.getInputs()) {
            prevTxMap.get(txInput.getTxId()).orElseThrow(() -> new RuntimeException("ERROR: Previous transaction is not correct"));
        }
        // 创建用于签名的交易信息的副本
        var txCopy = this.trimmedCopy();
        var ecdsaSign = Signature.getInstance("SHA256withECDSA", "SunEC");
        ecdsaSign.initSign(privateKey);
        for (var i = 0; i < txCopy.getInputs().length; i++) {
            var txInputCopy = txCopy.getInputs()[i];
            // 获取交易输入TxID对应的交易数据
            var prevTx = prevTxMap.get(txInputCopy.getTxId()).get();
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
    public boolean verify(Map<byte[], Optional<Transaction>> prevTxMap) {
        // coinbase 交易信息不需要签名，也就无需验证
        if (this.isCoinbase()) {
            return true;
        }
        // 再次验证一下交易信息中的交易输入是否正确，也就是能否查找对应的交易数据
        for (var txInput : this.getInputs()) {
            prevTxMap.get(txInput.getTxId()).orElseThrow(() -> new RuntimeException("ERROR: Previous transaction is not correct"));
        }
        // 创建用于签名验证的交易信息的副本
        try {
            var txCopy = this.trimmedCopy();
            var keyFactory = KeyFactory.getInstance("ECDSA", "SunEC");
            var ecdsaVerify = Signature.getInstance("SHA256withECDSA", "SunEC");
            for (var i = 0; i < this.getInputs().length; i++) {
                var txInput = this.getInputs()[i];
                // 获取交易输入TxID对应的交易数据
                var prevTx = prevTxMap.get(txInput.getTxId()).get();
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
            logger.log(Level.SEVERE, "Fail to verify transaction ! transaction invalid ! ", e);
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
     */
    public long getCreateTime() {
        return this.createTime;
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
        if (this.getCreateTime() != other.getCreateTime()) return false;
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
        final long $createTime = this.getCreateTime();
        result = result * PRIME + (int) ($createTime >>> 32 ^ $createTime);
        result = result * PRIME + Arrays.hashCode(this.getTxId());
        result = result * PRIME + Arrays.deepHashCode(this.getInputs());
        result = result * PRIME + Arrays.deepHashCode(this.getOutputs());
        return result;
    }

    @Override
    public String toString() {
        return "Transaction[txId=" + Bytes.byteArrayToHex(this.getTxId()) + ", inputs=" + Arrays.deepToString(this.getInputs()) + ", outputs=" + Arrays.deepToString(this.getOutputs()) + ", createTime=" + this.getCreateTime() + "]";
    }

    public Transaction(final byte[] txId, final TXInput[] inputs, final TXOutput[] outputs, final long createTime) {
        this.txId = txId;
        this.inputs = inputs;
        this.outputs = outputs;
        this.createTime = createTime;
    }
}
