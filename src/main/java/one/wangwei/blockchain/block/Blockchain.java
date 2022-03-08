package one.wangwei.blockchain.block;

import one.wangwei.blockchain.pow.Pow;
import one.wangwei.blockchain.store.RocksDbBlockRepository;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.transaction.TxInput;
import one.wangwei.blockchain.wallet.Address;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Optional;
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
            storage.appendBlock(block);
            return Optional.of(block.id());
        });

        return new Blockchain(storage);
    }

    public Blockchain(RocksDbBlockRepository storage) {
        this.storage = storage;
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
        storage.appendBlock(block);
    }

    public Stream<Block> stream() {
        return Stream
                .iterate(
                        storage.getLastBlockId().flatMap(storage::getBlock),
                        not(Optional::isEmpty),
                        x -> x.map(Block::previousId).flatMap(storage::getBlock)
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
                .map(TxInput::getTxId)
                .distinct()
                .map(this::findTransaction)
                .flatMap(Optional::stream)
                .collect(toMap(Transaction::id, identity()));
        tx.sign(privateKey, prevTx);
    }

    public boolean verifyTransactions(Transaction tx) throws InvalidKeySpecException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        if (tx.isCoinbase()) return true;
        var prevTx = Arrays.stream(tx.inputs())
                .map(TxInput::getTxId)
                .distinct()
                .map(this::findTransaction)
                .flatMap(Optional::stream)
                .collect(toMap(Transaction::id, identity()));
        return tx.verify(prevTx);
    }
}
