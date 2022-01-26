package one.wangwei.blockchain.cli;

import one.wangwei.blockchain.wallet.Address;
import picocli.CommandLine;
import picocli.CommandLine.ITypeConverter;

public class AddressTypeConverter implements ITypeConverter<Address> {
    @Override
    public Address convert(String s) {
        return Address.valueOf(s)
                .orElseThrow(() -> new CommandLine.TypeConversionException("%s is an invalid address".formatted(s)));
    }
}
