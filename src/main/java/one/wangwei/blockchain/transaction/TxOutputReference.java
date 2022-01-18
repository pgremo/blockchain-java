package one.wangwei.blockchain.transaction;

public record TxOutputReference(TransactionId txId, int index, TxOutput output) {
}
