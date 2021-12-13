package one.wangwei.blockchain.store;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.transaction.TXOutput;
import one.wangwei.blockchain.util.Bytes;
import one.wangwei.blockchain.util.SerializeUtils;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
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
    /**
     * 链状态桶Key
     */
    private static final String CHAINSTATE_BUCKET_KEY = "chainstate";
    /**
     * 最新一个区块
     */
    private static final String LAST_BLOCK_KEY = "l";
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
    /**
     * chainstate buckets
     */
    private Map<byte[], byte[]> chainstateBucket;

    private RocksDBUtils() {
        openDB();
        initChainStateBucket();
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
     * 初始化 blocks 数据桶
     */
    private void initChainStateBucket() {
        try {
            var chainstateBucketKey = SerializeUtils.serialize(CHAINSTATE_BUCKET_KEY);
            var chainstateBucketBytes = db.get(chainstateBucketKey);
            if (chainstateBucketBytes != null) {
                chainstateBucket = SerializeUtils.deserialize(chainstateBucketBytes);
            } else {
                chainstateBucket = new TreeMap<>((Comparator<byte[]> & Serializable) Arrays::compare);
                db.put(chainstateBucketKey, SerializeUtils.serialize(chainstateBucket));
            }
        } catch (RocksDBException e) {
            logger.log(Level.SEVERE, "Fail to init chainstate bucket ! ", e);
            throw new RuntimeException("Fail to init chainstate bucket ! ", e);
        }
    }

    /**
     * 保存最新一个区块的Hash值
     *
     * @param tipBlockHash
     */
    public void putLastBlockHash(String tipBlockHash) {
        try {
            db.put(SerializeUtils.serialize(LAST_BLOCK_KEY), SerializeUtils.serialize(tipBlockHash));
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
            byte[] bytes = db.get(SerializeUtils.serialize(LAST_BLOCK_KEY));
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
     * 清空chainstate bucket
     */
    public void cleanChainStateBucket() {
        try {
            chainstateBucket.clear();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fail to clear chainstate bucket ! ", e);
            throw new RuntimeException("Fail to clear chainstate bucket ! ", e);
        }
    }

    /**
     * 保存UTXO数据
     *
     * @param key   交易ID
     * @param utxos UTXOs
     */
    public void putUTXOs(byte[] key, TXOutput[] utxos) {
        try {
            chainstateBucket.put(key, SerializeUtils.serialize(utxos));
            db.put(SerializeUtils.serialize(CHAINSTATE_BUCKET_KEY), SerializeUtils.serialize(chainstateBucket));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fail to put UTXOs into chainstate bucket ! key=" + key, e);
            throw new RuntimeException("Fail to put UTXOs into chainstate bucket ! key=" + key, e);
        }
    }

    /**
     * 查询UTXO数据
     *
     * @param key 交易ID
     */
    public TXOutput[] getUTXOs(byte[] key) {
        var data = chainstateBucket.get(key);
        if (data == null) return null;
        return SerializeUtils.deserialize(data);
    }

    /**
     * 删除 UTXO 数据
     *
     * @param key 交易ID
     */
    public void deleteUTXOs(byte[] key) {
        try {
            chainstateBucket.remove(key);
            db.put(SerializeUtils.serialize(CHAINSTATE_BUCKET_KEY), SerializeUtils.serialize(chainstateBucket));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fail to delete UTXOs by key ! key=" + key, e);
            throw new RuntimeException("Fail to delete UTXOs by key ! key=" + key, e);
        }
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

    /**
     * chainstate buckets
     */
    public Map<byte[], byte[]> getChainstateBucket() {
        return this.chainstateBucket;
    }
}
