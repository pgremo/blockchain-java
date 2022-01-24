package one.wangwei.blockchain.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.Pool;

public class PooledKryo extends Kryo implements AutoCloseable{
    private Pool<PooledKryo> pool;

    public PooledKryo(Pool<PooledKryo> pool) {
        this.pool = pool;
    }

    @Override
    public void close() {
        pool.free(this);
    }
}
