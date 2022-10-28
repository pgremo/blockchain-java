package one.wangwei.blockchain.util;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.serializers.RecordSerializer;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.Pool;
import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.transaction.Transaction;
import org.objenesis.strategy.StdInstantiatorStrategy;

import javax.crypto.SealedObject;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class ObjectMapper {

    private static final Pool<PooledKryo> pool = new Pool<>(true, true, 16) {
        protected PooledKryo create() {
            var rs = new RecordSerializer<>();
            rs.setFixedFieldTypes(true);

            var kryo = new PooledKryo(this);
            kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
            kryo.addDefaultSerializer(Record.class, rs);
            kryo.register(ArrayList.class);
            kryo.register(Block.class);
            kryo.register(Block.Id.class);
            kryo.register(HashMap.class);
            kryo.register(Instant.class);
            kryo.register(List.class);
            kryo.register(SealedObject.class, new JavaSerializer());
            kryo.register(Transaction.class);
            kryo.register(Transaction[].class);
            kryo.register(Transaction.Id.class);
            kryo.register(TreeMap.class, new JavaSerializer());
            kryo.register(one.wangwei.blockchain.transaction.Input.class);
            kryo.register(one.wangwei.blockchain.transaction.Input[].class);
            kryo.register(one.wangwei.blockchain.transaction.Output.class);
            kryo.register(one.wangwei.blockchain.transaction.Output[].class);
            kryo.register(byte[].class);

            return kryo;
        }
    };

    public <T> T deserialize(byte[] bytes, Class<T> type) {
        try (
                var input = new Input(bytes);
                var mapper = pool.obtain()
        ) {
            return mapper.readObject(input, type);
        }
    }

    public <T> T deserialize(InputStream bytes, Class<T> type) {
        try (
                var input = new Input(bytes);
                var mapper = pool.obtain()
        ) {
            return mapper.readObject(input, type);
        }
    }

    public byte[] serialize(Object object) {
        try (
                var output = new Output(4096, -1);
                var mapper = pool.obtain()
        ) {
            mapper.writeObject(output, object);
            return output.toBytes();
        }
    }
}
