package one.wangwei.blockchain.util;

import java.util.Optional;

public class Numbers {
    public static Optional<Integer> parseInteger(String value) {
        try {
            return Optional.of(Integer.valueOf(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
