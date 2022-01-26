package one.wangwei.blockchain.store;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.util.ObjectMapper;
import org.rocksdb.*;

import java.util.Optional;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.arraycopy;
import static java.lang.System.getLogger;


public class RocksDbBlockRepository implements AutoCloseable {
    private static final System.Logger logger = getLogger(RocksDbBlockRepository.class.getName());
    private static final String DB_FILE = "blockchain.db";

    private final TransactionDB db;
    private final ObjectMapper serializer;

    public RocksDbBlockRepository(ObjectMapper serializer) throws RocksDBException {
        Options options = new Options();
        options.setCreateIfMissing(true);
        db = TransactionDB.open(options, new TransactionDBOptions(), DB_FILE);

        this.serializer = serializer;
    }

    public Optional<Block.Id> getLastBlockId() {
        try {
            return withTransaction(tx -> Optional.ofNullable(tx.get(new ReadOptions(), new byte[]{'l'})).map(Block.Id::new));
        } catch (RocksDBException e) {
            logger.log(ERROR, "Fail to get last block id !", e);
            throw new RuntimeException("Fail to get last block id !", e);
        }
    }

    public void appendBlock(Block block) {
        var x = block.id().value();
        try {
            withTransaction(tx -> {
                var key = new byte[x.length + 1];
                key[0] = 'b';
                arraycopy(x, 0, key, 1, x.length);
                tx.put(key, serializer.serialize(block));
                tx.put(new byte[]{'l'}, x);
                return true;
            });
        } catch (RocksDBException e) {
            logger.log(ERROR, () -> "Fail to put block ! block=%s".formatted(block), e);
            throw new RuntimeException("Fail to put block ! block=" + block, e);
        }
    }

    public Optional<Block> getBlock(Block.Id id) {
        var raw = id.value();
        try {
            return withTransaction(tx -> {
                var key = new byte[raw.length + 1];
                key[0] = 'b';
                arraycopy(raw, 0, key, 1, raw.length);
                return Optional.ofNullable(tx.get(new ReadOptions(), key)).map(data -> serializer.deserialize(data, Block.class));
            });
        } catch (RocksDBException e) {
            logger.log(ERROR, () -> "Fail to get block ! block=%s".formatted(id), e);
            throw new RuntimeException("Fail to get block ! block=" + id, e);
        }
    }

    @FunctionalInterface
    interface TransactionalCommand<T> {
        T apply(Transaction tx) throws RocksDBException;
    }

    private <T> T withTransaction(TransactionalCommand<T> command) throws RocksDBException {
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
