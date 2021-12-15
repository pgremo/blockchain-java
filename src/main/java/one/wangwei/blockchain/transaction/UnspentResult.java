package one.wangwei.blockchain.transaction;

import java.util.List;

public record UnspentResult(int total, List<Transaction.TxIoReference> unspent) {
}
