package one.wangwei.blockchain.store;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.util.Bytes;
import one.wangwei.blockchain.util.SerializeUtils;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.Optional;

import static java.lang.System.*;
import static java.lang.System.Logger.Level.ERROR;


public class RocksDbBlockRepository implements AutoCloseable {
    private static final Logger logger = getLogger(RocksDbBlockRepository.class.getName());
    private static final String DB_FILE = "blockchain.db";

    private final RocksDB db;

    public RocksDbBlockRepository() throws RocksDBException {
        db = RocksDB.open(DB_FILE);
    }

    public Optional<String> getLastBlockHash() {
        try {
            byte[] bytes = db.get(new byte[]{'l'});
            if (bytes == null) return Optional.empty();
            return Optional.of(SerializeUtils.deserialize(bytes));
        } catch (RocksDBException e) {
            logger.log(ERROR, "Fail to get last block hash !", e);
            throw new RuntimeException("Fail to get last block hash !", e);
        }
    }

    public void appendBlock(Block block) {
        Bytes.hexToByteArray(block.hash()).ifPresent(x -> {
            try {
                var key = new byte[33];
                key[0] = 'b';
                arraycopy(x, 0, key, 1, x.length);
                db.put(key, SerializeUtils.serialize(block));
                db.put(new byte[]{'l'}, SerializeUtils.serialize(x));
            } catch (RocksDBException e) {
                logger.log(ERROR, "Fail to put block ! block=" + block, e);
                throw new RuntimeException("Fail to put block ! block=" + block, e);
            }
        });
    }

    public Block getBlock(String blockHash) {
        return Bytes.hexToByteArray(blockHash).map(x -> {
            try {
                var key = new byte[33];
                key[0] = 'b';
                arraycopy(x, 0, key, 1, x.length);
                return SerializeUtils.<Block>deserialize(db.get(key));
            } catch (RocksDBException e) {
                logger.log(ERROR, "Fail to put block ! block=" + blockHash, e);
                throw new RuntimeException("Fail to put block ! block=" + blockHash, e);
            }
        }).orElseThrow();
    }

    public void close() {
        db.close();
    }
}
