package one.wangwei.blockchain.cli;

import picocli.CommandLine;

public class NaturalNumberTypeConverter implements CommandLine.ITypeConverter<Integer> {
    @Override
    public Integer convert(String value) {
        var result = Integer.parseInt(value);
        if (result < 1) throw new CommandLine.TypeConversionException("amount must be greater than 0");
        return result;
    }
}
