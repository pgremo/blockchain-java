package one.wangwei.blockchain.block;

import one.wangwei.blockchain.pow.Pow;
import one.wangwei.blockchain.store.RocksDbBlockRepository;
import one.wangwei.blockchain.transaction.Input;
import one.wangwei.blockchain.transaction.OutputReference;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.BtcAddressUtils;
import one.wangwei.blockchain.wallet.Address;
import one.wangwei.blockchain.wallet.Wallet;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;
import static one.wangwei.blockchain.block.Block.createGenesisBlock;
import static one.wangwei.blockchain.transaction.Transaction.Id;
import static one.wangwei.blockchain.transaction.Transaction.createCoinbaseTX;

public class Blockchain {

    private final RocksDbBlockRepository storage;

    public static Blockchain createBlockchain(RocksDbBlockRepository storage, Address address) {
        storage.getLastBlockId().or(() -> {
            var baseData = "G4ZD3A4Ya!tFz6vkqFC8D@eDPXK2sLGT8tPqbeTKbzmC6e.sYy@RsmMm-_MytkACCwxFj";
            var tx = createCoinbaseTX(address, baseData);
            var block = createGenesisBlock(tx).orElseThrow();
            storage.append(block);
            return Optional.of(block.id());
        });

        return new Blockchain(storage);
    }

    public Blockchain(RocksDbBlockRepository storage) {
        this.storage = storage;
    }

    public Stream<OutputReference> getUnspent(Wallet fromWallet) {
        return stream()
                .flatMap(x -> Arrays.stream(x.transactions()))
                .flatMap(new Function<>() {
                    private final List<Input> spent = new LinkedList<>();
                    private final byte[] fromPubKey = fromWallet.publicKey().getEncoded();
                    private final byte[] fromPubKeyHash = BtcAddressUtils.ripeMD160Hash(fromPubKey);

                    @Override
                    public Stream<? extends OutputReference> apply(Transaction transaction) {
                        var unspent = Stream.<OutputReference>builder();
                        var outputs = transaction.outputs();
                        for (var index = 0; index < outputs.length; index++) {
                            var output = outputs[index];

                            // continue to the next output if this one is not of the sender
                            if (!Arrays.equals(output.pubKeyHash(), fromPubKeyHash)) continue;

                            // continue to the next output if this one is spent
                            if (remove(transaction.id(), index)) continue;

                            // add valid to list of unspent
                            unspent.add(new OutputReference(transaction.id(), index, output));
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
                        var iterator = spent.listIterator(spent.size());
                        while (iterator.hasPrevious()) {
                            var next = iterator.previous();
                            if (next.getOutputIndex() == index && Objects.equals(next.getTxId(), txId)) {
                                iterator.remove();
                                return true;
                            }
                        }
                        return false;
                    }
                });
    }

    public Optional<Block> mineBlock(Transaction[] transactions) throws InvalidKeySpecException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        for (var tx : transactions) {
            if (!verifyTransactions(tx)) {
                throw new IllegalArgumentException("transactions are not valid");
            }
        }
        return storage.getLastBlockId().flatMap(x -> {
            var block = Pow.createBlock(x, transactions);
            block.ifPresent(this::add);
            return block;
        });
    }

    private void add(Block block) {
        storage.append(block);
    }

    public Stream<Block> stream() {
        return Stream
                .iterate(
                        storage.getLastBlockId().flatMap(storage::findById),
                        not(Optional::isEmpty),
                        x -> x.map(Block::previousId).flatMap(storage::findById)
                )
                .flatMap(Optional::stream);
    }

    private Optional<Transaction> findTransaction(Id txId) {
        return stream()
                .map(Block::transactions)
                .flatMap(Arrays::stream)
                .filter(x -> x.id().equals(txId))
                .findFirst();
    }

    public void signTransaction(Transaction tx, PrivateKey privateKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException {
        var prevTx = Arrays.stream(tx.inputs())
                .map(Input::getTxId)
                .distinct()
                .map(this::findTransaction)
                .flatMap(Optional::stream)
                .collect(toMap(Transaction::id, identity()));
        tx.sign(privateKey, prevTx);
    }

    public boolean verifyTransactions(Transaction tx) throws InvalidKeySpecException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        if (tx.isCoinbase()) return true;
        var prevTx = Arrays.stream(tx.inputs())
                .map(Input::getTxId)
                .distinct()
                .map(this::findTransaction)
                .flatMap(Optional::stream)
                .collect(toMap(Transaction::id, identity()));
        return tx.verify(prevTx);
    }
}
