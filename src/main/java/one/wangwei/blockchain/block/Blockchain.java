package one.wangwei.blockchain.block;

import one.wangwei.blockchain.store.RocksDbBlockRepository;
import one.wangwei.blockchain.transaction.TXInput;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.transaction.TransactionId;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.getLogger;
import static java.util.Collections.emptyIterator;
import static java.util.stream.Collectors.toMap;

public class Blockchain implements Iterable<Block> {
    private static final Logger logger = getLogger(Blockchain.class.getName());

    private final RocksDbBlockRepository storage;

    public static Blockchain createBlockchain(RocksDbBlockRepository storage, String address) {
        var result = new Blockchain(storage);
        var last = result.getLastBlockHash();
        if (last.isEmpty()) {
            var baseData = "G4ZD3A4Ya!tFz6vkqFC8D@eDPXK2sLGT8tPqbeTKbzmC6e.sYy@RsmMm-_MytkACCwxFj";
            var tx = Transaction.newCoinbaseTX(address, baseData);
            var block = Block.newGenesisBlock(tx).orElseThrow();
            result.addBlock(block);
        }
        return result;
    }

    public Blockchain(RocksDbBlockRepository storage) {
        this.storage = storage;
    }

    public Optional<Block> mineBlock(Transaction[] transactions) throws InvalidKeySpecException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        for (var tx : transactions) {
            if (!verifyTransactions(tx)) {
                logger.log(ERROR, () -> "Fail to mine block ! Invalid transaction ! tx=%s".formatted(tx));
                throw new RuntimeException("Fail to mine block ! Invalid transaction ! ");
            }
        }
        return storage.getLastBlockId().flatMap(x -> {
            var block = Block.newBlock(x, transactions);
            block.ifPresent(this::addBlock);
            return block;
        });
    }

    private void addBlock(Block block) {
        storage.appendBlock(block);
    }

    public static class BlockIterator implements Iterator<Block> {
        private final RocksDbBlockRepository storage;
        private BlockId current;

        private BlockIterator(RocksDbBlockRepository storage, BlockId currentBlockHash) {
            this.storage = storage;
            this.current = currentBlockHash;
        }

        public boolean hasNext() {
            return !current.equals(BlockId.Null);
        }

        public Block next() {
            var result = storage.getBlock(current)
                    .orElseThrow(NoSuchElementException::new);
            current = result.previousId();
            return result;
        }
    }

    public Iterator<Block> iterator() {
        return storage.getLastBlockId().map(x -> (Iterator<Block>) new BlockIterator(storage, x)).orElse(emptyIterator());
    }

    public Stream<Block> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    private Optional<Transaction> findTransaction(TransactionId txId) {
        return stream()
                .flatMap(x -> Arrays.stream(x.transactions()))
                .filter(x -> x.getId().equals(txId))
                .findFirst();
    }

    public void signTransaction(Transaction tx, PrivateKey privateKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException {
        var prevTx = Arrays.stream(tx.getInputs())
                .map(TXInput::getTxId)
                .distinct()
                .map(this::findTransaction)
                .flatMap(Optional::stream)
                .collect(toMap(Transaction::getId, Function.identity()));
        tx.sign(privateKey, prevTx);
    }

    public boolean verifyTransactions(Transaction tx) throws InvalidKeySpecException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        if (tx.isCoinbase()) return true;
        var prevTx = Arrays.stream(tx.getInputs())
                .map(TXInput::getTxId)
                .distinct()
                .map(this::findTransaction)
                .flatMap(Optional::stream)
                .collect(toMap(Transaction::getId, Function.identity()));
        return tx.verify(prevTx);
    }

    public Optional<BlockId> getLastBlockHash() {
        return storage.getLastBlockId();
    }
}
