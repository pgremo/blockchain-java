package one.wangwei.blockchain.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.serializers.RecordSerializer;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.transaction.TXInput;
import one.wangwei.blockchain.transaction.TXOutput;
import one.wangwei.blockchain.transaction.Transaction;
import org.objenesis.strategy.StdInstantiatorStrategy;

import javax.crypto.SealedObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 序列化工具类
 *
 * @author wangwei
 * @date 2018/02/07
 */
public final class SerializeUtils {

    private static final Kryo kryo;

    static {
        try {
            kryo = new Kryo();

            kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

            final RecordSerializer<?> rs = new RecordSerializer<>();
            rs.setFixedFieldTypes(true);
            kryo.addDefaultSerializer(Record.class, rs);

            kryo.register(ArrayList.class);
            kryo.register(Block.class);
            kryo.register(HashMap.class);
            kryo.register(Instant.class);
            kryo.register(List.class);
            kryo.register(SealedObject.class, new JavaSerializer());
            kryo.register(Transaction.class);
            kryo.register(Transaction[].class);
            kryo.register(TXInput.class);
            kryo.register(TXInput[].class);
            kryo.register(TXOutput.class);
            kryo.register(TXOutput[].class);
            kryo.register(byte[].class);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 反序列化
     *
     * @param bytes 对象对应的字节数组
     * @return
     */
    public static <T> T deserialize(byte[] bytes) {
        try (var input = new Input(bytes)) {
            return (T) kryo.readClassAndObject(input);
        }
    }

    public static <T> T deserialize(InputStream bytes) {
        try (var input = new Input(bytes)) {
            return (T) kryo.readClassAndObject(input);
        }
    }

    /**
     * 序列化
     *
     * @param object 需要序列化的对象
     * @return
     */
    public static byte[] serialize(Object object) {
        try (var output = new Output(4096, -1)) {
            kryo.writeClassAndObject(output, object);
            return output.toBytes();
        }
    }

    public static void serializeToStream(Object object, OutputStream out) {
        kryo.writeClassAndObject(new Output(out), object);
    }
}
