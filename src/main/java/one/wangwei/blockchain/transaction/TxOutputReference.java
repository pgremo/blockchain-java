package one.wangwei.blockchain.transaction;

public record TxOutputReference(Transaction.Id txId, int index, TxOutput output) {
}
