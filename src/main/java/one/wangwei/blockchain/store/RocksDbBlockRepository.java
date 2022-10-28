package one.wangwei.blockchain.store;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.util.ObjectMapper;
import org.rocksdb.*;

import java.util.Optional;

import static java.lang.System.arraycopy;
import static java.util.Optional.ofNullable;


public class RocksDbBlockRepository implements AutoCloseable {
    private static final String DB_FILE = "blockchain.db";

    private final TransactionDB db;
    private final ObjectMapper serializer;
    private final Options options = new Options().setCreateIfMissing(true);

    public RocksDbBlockRepository(ObjectMapper serializer) throws RocksDBException {
        this.serializer = serializer;
        this.db = TransactionDB.open(
                options,
                new TransactionDBOptions(),
                DB_FILE
        );
    }

    public Optional<Block.Id> getLastBlockId() {
        try {
            return withTransaction(tx -> ofNullable(tx.get(new ReadOptions(), new byte[]{'l'})).map(Block.Id::new));
        } catch (RocksDBException e) {
            throw new RuntimeException("Fail to get last block id !", e);
        }
    }

    public void append(Block block) {
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
            throw new RuntimeException("Fail to put block ! block=%s".formatted(block), e);
        }
    }

    public Optional<Block> findById(Block.Id id) {
        var raw = id.value();
        var key = new byte[raw.length + 1];
        key[0] = 'b';
        arraycopy(raw, 0, key, 1, raw.length);
        try {
            return withTransaction(tx -> ofNullable(tx.get(new ReadOptions(), key)).map(data -> serializer.deserialize(data, Block.class)));
        } catch (RocksDBException e) {
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
        options.close();
        db.close();
    }
}
