package one.wangwei.blockchain.store;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.util.Bytes;
import one.wangwei.blockchain.util.SerializeUtils;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 存储工具类
 *
 * @author wangwei
 * @date 2018/02/27
 */
public class RocksDBUtils {
    private static final Logger logger = Logger.getLogger(RocksDBUtils.class.getName());
    /**
     * 区块链数据文件
     */
    private static final String DB_FILE = "blockchain.db";
    private static volatile RocksDBUtils instance;

    public static RocksDBUtils getInstance() {
        if (instance == null) {
            synchronized (RocksDBUtils.class) {
                if (instance == null) {
                    instance = new RocksDBUtils();
                }
            }
        }
        return instance;
    }

    private RocksDB db;

    private RocksDBUtils() {
        openDB();
    }

    /**
     * 打开数据库
     */
    private void openDB() {
        try {
            db = RocksDB.open(DB_FILE);
        } catch (RocksDBException e) {
            logger.log(Level.SEVERE, "Fail to open db ! ", e);
            throw new RuntimeException("Fail to open db ! ", e);
        }
    }

    /**
     * 保存最新一个区块的Hash值
     *
     * @param tipBlockHash
     */
    public void putLastBlockHash(String tipBlockHash) {
        try {
            db.put(new byte[]{'l'}, SerializeUtils.serialize(tipBlockHash));
        } catch (RocksDBException e) {
            logger.log(Level.SEVERE, "Fail to put last block hash ! tipBlockHash=" + tipBlockHash, e);
            throw new RuntimeException("Fail to put last block hash ! tipBlockHash=" + tipBlockHash, e);
        }
    }

    /**
     * 查询最新一个区块的Hash值
     *
     * @return
     */
    public String getLastBlockHash() {
        try {
            byte[] bytes = db.get(new byte[]{'l'});
            return bytes == null ? "" : SerializeUtils.deserialize(bytes);
        } catch (RocksDBException e) {
            logger.log(Level.SEVERE, "Fail to get last block hash !", e);
            throw new RuntimeException("Fail to get last block hash !", e);
        }
    }

    /**
     * 保存区块
     *
     * @param block
     */
    public void putBlock(Block block) {
        Bytes.hexToByteArray(block.hash()).ifPresent(x -> {
            try {
                var key = new byte[33];
                key[0] = 'b';
                System.arraycopy(x, 0, key, 1, x.length);
                db.put(key, SerializeUtils.serialize(block));
            } catch (RocksDBException e) {
                logger.log(Level.SEVERE, "Fail to put block ! block=" + block, e);
                throw new RuntimeException("Fail to put block ! block=" + block, e);
            }
        });
    }

    /**
     * 查询区块
     *
     * @param blockHash
     * @return
     */
    public Block getBlock(String blockHash) {
        return Bytes.hexToByteArray(blockHash).map(x -> {
            try {
                var key = new byte[33];
                key[0] = 'b';
                System.arraycopy(x, 0, key, 1, x.length);
                return SerializeUtils.<Block>deserialize(db.get(key));
            } catch (RocksDBException e) {
                logger.log(Level.SEVERE, "Fail to put block ! block=" + blockHash, e);
                throw new RuntimeException("Fail to put block ! block=" + blockHash, e);
            }
        }).orElseThrow();
    }

    /**
     * 关闭数据库
     */
    public void closeDB() {
        try {
            db.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fail to close db ! ", e);
            throw new RuntimeException("Fail to close db ! ", e);
        }
    }
}
