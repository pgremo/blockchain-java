package one.wangwei.blockchain.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.serializers.RecordSerializer;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.block.BlockId;
import one.wangwei.blockchain.transaction.TxInput;
import one.wangwei.blockchain.transaction.TxOutput;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.transaction.TransactionId;
import org.objenesis.strategy.StdInstantiatorStrategy;

import javax.crypto.SealedObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public final class SerializeUtils {

    private static final Kryo kryo;

    static {
        kryo = new Kryo();

        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        final RecordSerializer<?> rs = new RecordSerializer<>();
        rs.setFixedFieldTypes(true);
        kryo.addDefaultSerializer(Record.class, rs);

        kryo.register(ArrayList.class);
        kryo.register(Block.class);
        kryo.register(BlockId.class);
        kryo.register(HashMap.class);
        kryo.register(Instant.class);
        kryo.register(List.class);
        kryo.register(SealedObject.class, new JavaSerializer());
        kryo.register(Transaction.class);
        kryo.register(Transaction[].class);
        kryo.register(TransactionId.class);
        kryo.register(TreeMap.class, new JavaSerializer());
        kryo.register(TxInput.class);
        kryo.register(TxInput[].class);
        kryo.register(TxOutput.class);
        kryo.register(TxOutput[].class);
        kryo.register(byte[].class);
    }

    public static <T> T deserialize(byte[] bytes, Class<T> type) {
        try (var input = new Input(bytes)) {
            return kryo.readObject(input, type);
        }
    }

    public static <T> T deserialize(InputStream bytes, Class<T> type) {
        try (var input = new Input(bytes)) {
            return kryo.readObject(input, type);
        }
    }

    public static byte[] serialize(Object object) {
        try (var output = new Output(4096, -1)) {
            kryo.writeObject(output, object);
            return output.toBytes();
        }
    }

    public static void serializeToStream(Object object, OutputStream out) {
        kryo.writeObject(new Output(out), object);
    }
}
