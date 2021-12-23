package one.wangwei.blockchain.transaction;

import one.wangwei.blockchain.util.Hashes;
import one.wangwei.blockchain.util.Numbers;
import one.wangwei.blockchain.wallet.Address;

import java.util.HexFormat;

public record TXOutput(int value, byte[] pubKeyHash) {

    public static TXOutput newTXOutput(int value, Address address) {
        return new TXOutput(value, address.value());
    }

    public byte[] hash() {
        return Hashes.sha256(Numbers.toBytes(value), pubKeyHash);
    }

    @Override
    public String toString() {
        return "TXOutput[" +
                "value=" + value +
                ", pubKeyHash=" + HexFormat.of().formatHex(pubKeyHash) +
                ']';
    }
}
