package one.wangwei.blockchain.cli;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

import static java.lang.Integer.parseInt;

public class NaturalNumberTypeConverter implements ITypeConverter<Integer> {
    @Override
    public Integer convert(String value) {
        var result = parseInt(value);
        if (result < 1) throw new TypeConversionException("amount must be greater than 0");
        return result;
    }
}
