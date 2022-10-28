package one.wangwei.blockchain.transaction;

public record OutputReference(Transaction.Id txId, int index, Output output) {
}
