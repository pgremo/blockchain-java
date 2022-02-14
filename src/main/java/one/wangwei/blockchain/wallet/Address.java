package one.wangwei.blockchain.wallet;

import one.wangwei.blockchain.util.Base58Check;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public record Address(byte version, byte[] hash, byte[] check) implements Serializable {
    public static Optional<Address> valueOf(String value) {
        return Base58Check.valueOf(value)
                .map(x -> {
                    var version = x[0];
                    var hash = Arrays.copyOfRange(x, 1, 20);
                    var check = Arrays.copyOfRange(x, 21, 25);
                    return new Address(version, hash, check);
                });
    }

    @Override
    public String toString() {
        return Base58Check.encode(ByteBuffer.allocate(25).put(version).put(hash).put(check).array());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return version == address.version && Arrays.equals(hash, address.hash) && Arrays.equals(check, address.check);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(version);
        result = 31 * result + Arrays.hashCode(hash);
        result = 31 * result + Arrays.hashCode(check);
        return result;
    }
}
