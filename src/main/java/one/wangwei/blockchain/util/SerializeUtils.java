package one.wangwei.blockchain.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.transaction.TXInput;
import one.wangwei.blockchain.transaction.TXOutput;
import one.wangwei.blockchain.transaction.Transaction;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 序列化工具类
 *
 * @author wangwei
 * @date 2018/02/07
 */
public class SerializeUtils {

    private static Kryo kryo;

    static{
        kryo = new Kryo();
        kryo.register(HashMap.class);
        kryo.register(Transaction.class);
        kryo.register(Transaction[].class);
        kryo.register(TXInput.class);
        kryo.register(TXInput[].class);
        kryo.register(TXOutput.class);
        kryo.register(TXOutput[].class);
        kryo.register(ArrayList.class);
        kryo.register(byte[].class);
        kryo.register(Block.class);
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
}
