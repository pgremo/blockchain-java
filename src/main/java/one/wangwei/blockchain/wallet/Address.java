package one.wangwei.blockchain.wallet;

import one.wangwei.blockchain.util.Base58Check;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;

import static java.lang.System.arraycopy;
import static one.wangwei.blockchain.util.BtcAddressUtils.checksum;

public record Address(byte[] value) implements Serializable {
    public static Optional<Address> valueOf(String value) {
        return Base58Check.valueOf(value)
                .map(x -> Arrays.copyOfRange(x, 1, x.length))
                .map(Address::new);
    }

    @Override
    public String toString() {
        var buffer = new byte[25];
        buffer[0] = 0;
        arraycopy(value, 0, buffer, 1, value.length);
        var versioned = new byte[21];
        arraycopy(buffer, 0, versioned, 0, versioned.length);
        var check = checksum(versioned);
        arraycopy(check, 0, buffer, 21, check.length);
        return Base58Check.encode(buffer);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Arrays.equals(value, address.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
