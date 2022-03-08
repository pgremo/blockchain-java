package one.wangwei.blockchain.wallet;

import one.wangwei.blockchain.util.Base58;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public record Address(Version version, byte[] hash, byte[] check) implements Serializable {
    public static Optional<Address> valueOf(String value) {
        return Base58.valueOf(value)
                .flatMap(x -> {
                    var version = x[0];
                    var hash = Arrays.copyOfRange(x, 1, 20);
                    var check = Arrays.copyOfRange(x, 21, 25);
                    return Version.valueOf(version).map(y -> new Address(y, hash, check));
                });
    }

    @Override
    public String toString() {
        return Base58.encode(
                ByteBuffer.allocate(25)
                        .put(version.value())
                        .put(hash)
                        .put(check)
                        .array()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(version, address.version) && Arrays.equals(hash, address.hash) && Arrays.equals(check, address.check);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(version);
        result = 31 * result + Arrays.hashCode(hash);
        result = 31 * result + Arrays.hashCode(check);
        return result;
    }

    public enum Version {
        Prod((byte) 0),
        Test((byte) 1);

        private final byte value;

        Version(byte value) {
            this.value = value;
        }

        public byte value() {
            return value;
        }

        public static Optional<Version> valueOf(byte code){
            return Arrays.stream(Version.values()).filter(x -> x.value() == code).findFirst();
        }
    }
}
