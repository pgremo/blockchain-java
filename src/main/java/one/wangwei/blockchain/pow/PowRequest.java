package one.wangwei.blockchain.pow;

import one.wangwei.blockchain.transaction.Transaction;

import java.time.Instant;

public record PowRequest(String previousHash, Transaction[] transactions, Instant stamp) {
}
