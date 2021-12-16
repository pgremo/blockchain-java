package one.wangwei.blockchain.block;

import one.wangwei.blockchain.store.RocksDbBlockRepository;
import one.wangwei.blockchain.transaction.TXInput;
import one.wangwei.blockchain.transaction.Transaction;

import java.security.*;
import java.util.*;
import java.util.stream.StreamSupport;

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.getLogger;

public class Blockchain implements Iterable<Block> {
    private static final Logger logger = getLogger(Blockchain.class.getName());
    private final RocksDbBlockRepository storage;

    public static Blockchain createBlockchain(RocksDbBlockRepository storage, String address) {
        var result = new Blockchain(storage);
        var last = result.getLastBlockHash();
        if (last.isEmpty()) {
            var genesisCoinbaseData = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";
            var coinbaseTX = Transaction.newCoinbaseTX(address, genesisCoinbaseData);
            var genesisBlock = one.wangwei.blockchain.block.Block.newGenesisBlock(coinbaseTX).orElseThrow();
            result.addBlock(genesisBlock);
        }
        return result;
    }

    public Blockchain(RocksDbBlockRepository storage) {
        this.storage = storage;
    }

    public Optional<one.wangwei.blockchain.block.Block> mineBlock(Transaction[] transactions) {
        for (var tx : transactions) {
            if (!verifyTransactions(tx)) {
                logger.log(ERROR, () -> "Fail to mine block ! Invalid transaction ! tx=%s".formatted(tx));
                throw new RuntimeException("Fail to mine block ! Invalid transaction ! ");
            }
        }
        return storage.getLastBlockHash().flatMap(x -> {
            var block = one.wangwei.blockchain.block.Block.newBlock(x, transactions);
            block.ifPresent(this::addBlock);
            return block;
        });
    }

    private void addBlock(one.wangwei.blockchain.block.Block block) {
        storage.appendBlock(block);
    }

    public static class BlockIterator implements Iterator<Block> {
        private final RocksDbBlockRepository storage;
        private String currentBlockHash;

        private BlockIterator(RocksDbBlockRepository storage, String currentBlockHash) {
            this.storage = storage;
            this.currentBlockHash = currentBlockHash;
        }

        public boolean hasNext() {
            return !currentBlockHash.equals(Block.ZERO_HASH);
        }

        public Block next() {
            var currentBlock = storage.getBlock(currentBlockHash);
            if (currentBlock == null) throw new NoSuchElementException();
            currentBlockHash = currentBlock.previousHash();
            return currentBlock;
        }
    }

    public Iterator<Block> iterator() {
        return storage.getLastBlockHash().map(x -> (Iterator<Block>) new BlockIterator(storage, x)).orElseGet(() -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Block next() {
                throw new NoSuchElementException();
            }
        });
    }

    private Optional<Transaction> findTransaction(byte[] txId) {
        return StreamSupport.stream(spliterator(), false)
                .flatMap(x -> Arrays.stream(x.transactions()))
                .filter(x -> Arrays.equals(x.getTxId(), txId))
                .findFirst();
    }

    public void signTransaction(Transaction tx, PrivateKey privateKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException {
        var prevTx = new TreeMap<byte[], Transaction>(Arrays::compare);
        for (TXInput txInput : tx.getInputs()) {
            byte[] id = txInput.getTxId();
            findTransaction(id).ifPresent(transaction -> prevTx.putIfAbsent(id, transaction));
        }
        tx.sign(privateKey, prevTx);
    }

    public boolean verifyTransactions(Transaction tx) {
        if (tx.isCoinbase()) return true;
        var prevTx = new TreeMap<byte[], Transaction>(Arrays::compare);
        for (TXInput txInput : tx.getInputs()) {
            byte[] id = txInput.getTxId();
            findTransaction(id).ifPresent(transaction -> prevTx.putIfAbsent(id, transaction));
        }
        return tx.verify(prevTx);
    }

    public Optional<String> getLastBlockHash() {
        return storage.getLastBlockHash();
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof final Blockchain other)) return false;
        if (!other.canEqual(this)) return false;
        final Object this$lastBlockHash = this.getLastBlockHash();
        final Object other$lastBlockHash = other.getLastBlockHash();
        return Objects.equals(this$lastBlockHash, other$lastBlockHash);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Blockchain;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $lastBlockHash = this.getLastBlockHash();
        result = result * PRIME + ($lastBlockHash == null ? 43 : $lastBlockHash.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Blockchain(lastBlockHash=" + this.getLastBlockHash() + ")";
    }
}
