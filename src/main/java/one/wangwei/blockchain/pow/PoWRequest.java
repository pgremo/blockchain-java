package one.wangwei.blockchain.pow;

import one.wangwei.blockchain.transaction.Transaction;

import java.time.Instant;

public record PoWRequest(String previousHash, Transaction[] transactions, Instant stamp) {
}
