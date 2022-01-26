package one.wangwei.blockchain.util;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.serializers.RecordSerializer;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.Pool;
import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.transaction.TxInput;
import one.wangwei.blockchain.transaction.TxOutput;
import org.objenesis.strategy.StdInstantiatorStrategy;

import javax.crypto.SealedObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class ObjectMapper {

    private static final Pool<PooledKryo> pool = new Pool<>(true, true, 16) {
        protected PooledKryo create() {
            var kryo = new PooledKryo(this);

            kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

            var rs = new RecordSerializer<>();
            rs.setFixedFieldTypes(true);
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
            kryo.register(TxInput.class);
            kryo.register(TxInput[].class);
            kryo.register(TxOutput.class);
            kryo.register(TxOutput[].class);
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

    public void serializeToStream(Object object, OutputStream out) {
        try (
                var mapper = pool.obtain()
        ) {
            mapper.writeObject(new Output(out), object);
        }
    }
}
