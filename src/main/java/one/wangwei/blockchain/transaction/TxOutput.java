package one.wangwei.blockchain.transaction;

import one.wangwei.blockchain.util.Hashes;
import one.wangwei.blockchain.util.Numbers;
import one.wangwei.blockchain.wallet.Address;

import java.util.HexFormat;

public record TxOutput(int value, byte[] pubKeyHash) {

    public static TxOutput newTXOutput(int value, Address address) {
        return new TxOutput(value, address.hash());
    }

    public byte[] hash() {
        return Hashes.sha256(Numbers.toBytes(value), pubKeyHash);
    }

    @Override
    public String toString() {
        return "TXOutput[" +
                "hash=" + value +
                ", pubKeyHash=" + HexFormat.of().formatHex(pubKeyHash) +
                ']';
    }
}
