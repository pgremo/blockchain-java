package one.wangwei.blockchain.store;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.util.ObjectMapper;
import org.rocksdb.*;

import java.util.Optional;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.arraycopy;
import static java.lang.System.getLogger;
import static java.util.Optional.ofNullable;


public class RocksDbBlockRepository implements AutoCloseable {
    private static final System.Logger logger = getLogger(RocksDbBlockRepository.class.getName());
    private static final String DB_FILE = "blockchain.db";

    private final TransactionDB db;
    private final ObjectMapper serializer;

    public RocksDbBlockRepository(ObjectMapper serializer) throws RocksDBException {
        this.db = TransactionDB.open(
                new Options()
                        .setCreateIfMissing(true),
                new TransactionDBOptions(),
                DB_FILE
        );

        this.serializer = serializer;
    }

    public Optional<Block.Id> getLastBlockId() {
        try {
            return withTransaction(tx -> ofNullable(tx.get(new ReadOptions(), new byte[]{'l'})).map(Block.Id::new));
        } catch (RocksDBException e) {
            logger.log(ERROR, "Fail to get last block id !", e);
            throw new RuntimeException("Fail to get last block id !", e);
        }
    }

    public void appendBlock(Block block) {
        var x = block.id().value();
        var key = new byte[x.length + 1];
        key[0] = 'b';
        arraycopy(x, 0, key, 1, x.length);
        try {
            withTransaction(tx -> {
                tx.put(key, serializer.serialize(block));
                tx.put(new byte[]{'l'}, x);
                return true;
            });
        } catch (RocksDBException e) {
            logger.log(ERROR, () -> "Fail to put block ! block=%s".formatted(block), e);
            throw new RuntimeException("Fail to put block ! block=%s".formatted(block), e);
        }
    }

    public Optional<Block> getBlock(Block.Id id) {
        var raw = id.value();
        var key = new byte[raw.length + 1];
        key[0] = 'b';
        arraycopy(raw, 0, key, 1, raw.length);
        try {
            return withTransaction(tx -> ofNullable(tx.get(new ReadOptions(), key)).map(data -> serializer.deserialize(data, Block.class)));
        } catch (RocksDBException e) {
            logger.log(ERROR, () -> "Fail to get block ! block=%s".formatted(id), e);
            throw new RuntimeException("Fail to get block ! block=%s".formatted(id), e);
        }
    }

    @FunctionalInterface
    interface TransactionalCommand<T> {
        T apply(Transaction tx) throws RocksDBException;
    }

    private <T> T withTransaction(TransactionalCommand<T> command) throws RocksDBException {
        var transaction = db.beginTransaction(new WriteOptions());
        try {
            T result = command.apply(transaction);
            transaction.commit();
            return result;
        } catch (RocksDBException ex) {
            transaction.rollback();
            throw ex;
        }
    }

    public void close() {
        db.close();
    }
}
