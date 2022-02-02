package one.wangwei.blockchain.cli;

import one.wangwei.blockchain.wallet.Address;
import picocli.CommandLine.ITypeConverter;

import static picocli.CommandLine.TypeConversionException;

public class AddressTypeConverter implements ITypeConverter<Address> {
    @Override
    public Address convert(String s) {
        return Address.valueOf(s)
                .orElseThrow(() -> new TypeConversionException("%s is an invalid address".formatted(s)));
    }
}
