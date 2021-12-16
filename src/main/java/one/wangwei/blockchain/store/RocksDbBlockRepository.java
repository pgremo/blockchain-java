package one.wangwei.blockchain.store;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.util.Bytes;
import one.wangwei.blockchain.util.SerializeUtils;
import org.rocksdb.*;

import java.util.Optional;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.arraycopy;
import static java.lang.System.getLogger;


public class RocksDbBlockRepository implements AutoCloseable {
    private static final System.Logger logger = getLogger(RocksDbBlockRepository.class.getName());
    private static final String DB_FILE = "blockchain.db";

    private final TransactionDB db;

    public RocksDbBlockRepository() throws RocksDBException {
        db = TransactionDB.open(new Options(), new TransactionDBOptions(), DB_FILE);
    }

    public Optional<String> getLastBlockHash() {
        try {
            return doInTransaction(tx -> Optional.ofNullable(db.get(new byte[]{'l'})).map(Bytes::byteArrayToHex));
        } catch (RocksDBException e) {
            logger.log(ERROR, "Fail to get last block hash !", e);
            throw new RuntimeException("Fail to get last block hash !", e);
        }
    }

    public void appendBlock(Block block) {
        Bytes.hexToByteArray(block.hash()).ifPresent(x -> {
            var transaction = db.beginTransaction(new WriteOptions());
            try {
                doInTransaction(tx -> {
                    var key = new byte[x.length + 1];
                    key[0] = 'b';
                    arraycopy(x, 0, key, 1, x.length);
                    transaction.put(key, SerializeUtils.serialize(block));
                    transaction.put(new byte[]{'l'}, x);
                    return true;
                });
            } catch (RocksDBException e) {
                logger.log(ERROR, () -> "Fail to put block ! block=%s".formatted(block), e);
                throw new RuntimeException("Fail to put block ! block=" + block, e);
            }
        });
    }

    public Optional<Block> getBlock(String blockHash) {
        return Bytes.hexToByteArray(blockHash).map(x -> {
            try {
                return doInTransaction(tx -> {
                    var key = new byte[x.length + 1];
                    key[0] = 'b';
                    arraycopy(x, 0, key, 1, x.length);
                    return Optional.ofNullable(db.get(key)).<Block>map(SerializeUtils::deserialize);
                });
            } catch (RocksDBException e) {
                logger.log(ERROR, () -> "Fail to get block ! block=%s".formatted(blockHash), e);
                throw new RuntimeException("Fail to get block ! block=" + blockHash, e);
            }
        }).orElseThrow();
    }

    @FunctionalInterface
    interface Command<T> {
        T apply(Transaction tx) throws RocksDBException;
    }

    private <T> T doInTransaction(Command<T> command) throws RocksDBException {
        var transaction = db.beginTransaction(new WriteOptions());
        T result;
        try {
            result = command.apply(transaction);
            transaction.commit();
        } catch (RocksDBException ex) {
            transaction.rollback();
            throw ex;
        }
        return result;
    }

    public void close() {
        db.close();
    }
}
