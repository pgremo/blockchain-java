package one.wangwei.blockchain.transaction;

import java.util.List;
import java.util.Map;

public record SpendableOutputResult(int accumulated, Map<String, List<Integer>> unspentOuts) {
}
