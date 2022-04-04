package one.wangwei.blockchain.transaction;

import one.wangwei.blockchain.block.Blockchain;
import one.wangwei.blockchain.util.BtcAddressUtils;
import one.wangwei.blockchain.util.Hashes;
import one.wangwei.blockchain.util.Numbers;
import one.wangwei.blockchain.wallet.Address;
import one.wangwei.blockchain.wallet.Wallet;
import one.wangwei.blockchain.wallet.WalletRepository;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;
import static one.wangwei.blockchain.util.MerkleRoot.merkleRoot;

public class Transaction {
    private static final int SUBSIDY = 10;
    private Id id;
    private final TxInput[] inputs;
    private final TxOutput[] outputs;
    private final Instant created;

    public static Transaction createCoinbaseTX(Address to, String data) {
        if (data.isBlank()) data = "Reward to '%s'".formatted(to);
        var txInput = new TxInput(new Id(new byte[0]), -1, null, data.getBytes());
        var txOutput = TxOutput.newTXOutput(SUBSIDY, to);
        var tx = new Transaction(null, new TxInput[]{txInput}, new TxOutput[]{txOutput}, Instant.now());
        tx.id(new Id(tx.hash()));
        return tx;
    }

    public byte[] hash() {
        return Hashes.sha256(
                merkleRoot(Arrays.stream(inputs()).map(TxInput::hash).collect(toCollection(LinkedList::new))),
                merkleRoot(Arrays.stream(outputs()).map(TxOutput::hash).collect(toCollection(LinkedList::new))),
                Numbers.toBytes(created().toEpochMilli())
        );
    }

    public boolean isCoinbase() {
        return inputs().length == 1 && inputs()[0].getTxId().value().length == 0 && inputs()[0].getTxOutputIndex() == -1;
    }

    public static Transaction createTransaction(Address from, Address to, int amount, Blockchain chain, WalletRepository walletRepository) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, IOException, ClassNotFoundException {
        var fromWallet = walletRepository.getWallet(from);
        var predicate = new Predicate<TxOutputReference>() {
            private int total;

            @Override
            public boolean test(TxOutputReference x) {
                total += x.output().value();
                return total < amount;
            }
        };
        var result = getUnspent(chain, fromWallet)
                .takeWhile(predicate)
                .toList();
        if (predicate.total < amount) throw new RuntimeException("insufficient funds");

        var inputs = result.stream()
                .map(x -> new TxInput(x.txId(), x.index(), null, fromWallet.publicKey().getEncoded()))
                .toArray(TxInput[]::new);

        var toWallet = walletRepository.getWallet(to);
        var toPubKey = toWallet.publicKey().getEncoded();
        var toPubKeyHash = BtcAddressUtils.ripeMD160Hash(toPubKey);
        var first = new TxOutput(amount, toPubKeyHash);
        var outputs = predicate.total > amount ?
                new TxOutput[]{first, new TxOutput(predicate.total - amount, BtcAddressUtils.ripeMD160Hash(fromWallet.publicKey().getEncoded()))} :
                new TxOutput[]{first};

        var tx = new Transaction(null, inputs, outputs, Instant.now());
        tx.id(new Id(tx.hash()));

        chain.signTransaction(tx, fromWallet.privateKey());

        return tx;
    }

    public static Stream<TxOutputReference> getUnspent(Blockchain chain, Wallet fromWallet) {
        return chain.stream()
                .flatMap(x -> Arrays.stream(x.transactions()))
                .flatMap(new Function<>() {
                    private final List<TxInput> spent = new LinkedList<>();
                    private final byte[] fromPubKey = fromWallet.publicKey().getEncoded();
                    private final byte[] fromPubKeyHash = BtcAddressUtils.ripeMD160Hash(fromPubKey);

                    @Override
                    public Stream<? extends TxOutputReference> apply(Transaction transaction) {
                        var unspent = Stream.<TxOutputReference>builder();
                        var outputs = transaction.outputs();
                        for (var index = 0; index < outputs.length; index++) {
                            var output = outputs[index];

                            // continue to the next output if this one is not of the sender
                            if (!Arrays.equals(output.pubKeyHash(), fromPubKeyHash)) continue;

                            // continue to the next output if this one is spent
                            if (remove(transaction.id(), index)) continue;

                            // add valid to list of unspent
                            unspent.add(new TxOutputReference(transaction.id(), index, output));
                        }
                        // accumulate transaction inputs of sender
                        for (var input : transaction.inputs()) {
                            if (Arrays.equals(input.getPubKey(), fromPubKey)) {
                                spent.add(input);
                            }
                        }

                        return unspent.build();
                    }

                    private boolean remove(Id txId, int index) {
                        for (var iterator = spent.listIterator(spent.size()); iterator.hasPrevious(); ) {
                            var next = iterator.previous();
                            if (next.getTxOutputIndex() == index && Objects.equals(next.getTxId(), txId)) {
                                iterator.remove();
                                return true;
                            }
                        }
                        return false;
                    }
                });
    }

    public Transaction trimmedCopy() {
        var tmpTXInputs = new TxInput[inputs().length];
        for (var i = 0; i < inputs().length; i++) {
            var txInput = inputs()[i];
            tmpTXInputs[i] = new TxInput(txInput.getTxId(), txInput.getTxOutputIndex(), null, null);
        }
        var tmpTXOutputs = new TxOutput[outputs().length];
        for (var i = 0; i < outputs().length; i++) {
            var txOutput = outputs()[i];
            tmpTXOutputs[i] = new TxOutput(txOutput.value(), txOutput.pubKeyHash());
        }
        return new Transaction(id(), tmpTXInputs, tmpTXOutputs, created());
    }

    public void sign(PrivateKey privateKey, Map<Id, Transaction> prevTxMap) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchProviderException {
        if (isCoinbase()) return;
        for (var txInput : inputs()) {
            if (!prevTxMap.containsKey(txInput.getTxId()))
                throw new RuntimeException("ERROR: Previous transaction is not correct");
        }
        var txCopy = trimmedCopy();
        var signature = Signature.getInstance("SHA256withECDSA", "SunEC");
        signature.initSign(privateKey);
        for (var i = 0; i < txCopy.inputs().length; i++) {
            var txInputCopy = txCopy.inputs()[i];
            var prevTx = prevTxMap.get(txInputCopy.getTxId());
            var prevTxOutput = prevTx.outputs()[txInputCopy.getTxOutputIndex()];
            txInputCopy.setPubKey(prevTxOutput.pubKeyHash());
            txInputCopy.setSignature(null);
            signature.update(txCopy.hash());
            inputs()[i].setSignature(signature.sign());
            txInputCopy.setPubKey(null);
        }
    }

    public boolean verify(Map<Id, Transaction> prevTxMap) throws InvalidKeySpecException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, NoSuchProviderException {
        if (this.isCoinbase()) return true;
        for (var txInput : inputs()) {
            if (!prevTxMap.containsKey(txInput.getTxId()))
                throw new RuntimeException("ERROR: Previous transaction is not correct");
        }
        var txCopy = trimmedCopy();
        var keyFactory = KeyFactory.getInstance("EC", "SunEC");
        var signature = Signature.getInstance("SHA256withECDSA", "SunEC");
        for (var i = 0; i < inputs().length; i++) {
            var txInput = inputs()[i];
            var prevTx = prevTxMap.get(txInput.getTxId());
            var prevTxOutput = prevTx.outputs()[txInput.getTxOutputIndex()];
            var txInputCopy = txCopy.inputs()[i];
            txInputCopy.setSignature(null);
            txInputCopy.setPubKey(prevTxOutput.pubKeyHash());
            var publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(txInput.getPubKey()));
            signature.initVerify(publicKey);
            signature.update(txCopy.hash());
            if (!signature.verify(txInput.getSignature())) return false;
            txInputCopy.setPubKey(null);
        }
        return true;
    }

    public Id id() {
        return this.id;
    }

    public void id(final Id id) {
        this.id = id;
    }

    public TxInput[] inputs() {
        return this.inputs;
    }

    public TxOutput[] outputs() {
        return this.outputs;
    }

    public Instant created() {
        return this.created;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof final Transaction other)) return false;
        if (!other.canEqual(this)) return false;
        if (this.created() != other.created()) return false;
        if (!this.id().equals(other.id())) return false;
        if (!Arrays.deepEquals(this.inputs(), other.inputs())) return false;
        return Arrays.deepEquals(this.outputs(), other.outputs());
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Transaction;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $createTime = this.created().toEpochMilli();
        result = result * PRIME + (int) ($createTime >>> 32 ^ $createTime);
        result = result * PRIME + this.id().hashCode();
        result = result * PRIME + Arrays.deepHashCode(this.inputs());
        result = result * PRIME + Arrays.deepHashCode(this.outputs());
        return result;
    }

    @Override
    public String toString() {
        return "Transaction[txId=" + id + ", inputs=" + Arrays.deepToString(this.inputs()) + ", outputs=" + Arrays.deepToString(this.outputs()) + ", createTime=" + this.created() + "]";
    }

    public Transaction(final Id id, final TxInput[] inputs, final TxOutput[] outputs, final Instant created) {
        this.id = id;
        this.inputs = inputs;
        this.outputs = outputs;
        this.created = created;
    }

    public record Id(byte[] value) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Id that = (Id) o;
            return Arrays.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }

        @Override
        public String toString() {
            return HexFormat.of().formatHex(value);
        }
    }
}
