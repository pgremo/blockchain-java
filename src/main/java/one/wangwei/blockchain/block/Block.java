package one.wangwei.blockchain.block;

import one.wangwei.blockchain.pow.Pow;
import one.wangwei.blockchain.transaction.Transaction;

import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;

public record Block(Id id, Id previousId, Transaction[] transactions, Instant timeStamp, long nonce) {

    public static Optional<Block> createGenesisBlock(Transaction coinbase) {
        return Pow.createBlock(Id.Null, coinbase);
    }

    @Override
    public String toString() {
        return "Block[" +
                "id=" + id +
                ", previousId=" + previousId +
                ", transactions=" + Arrays.toString(transactions) +
                ", timeStamp=" + timeStamp +
                ", nonce=" + nonce +
                ']';
    }

    public record Id(byte[] value) {
        public static final Id Null = new Id(new byte[32]);

        @Override
        public String toString() {
            return HexFormat.of().formatHex(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Id id = (Id) o;
            return Arrays.equals(value, id.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }
    }
}
