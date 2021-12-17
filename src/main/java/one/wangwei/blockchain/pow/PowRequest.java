package one.wangwei.blockchain.pow;

import one.wangwei.blockchain.block.BlockId;
import one.wangwei.blockchain.transaction.Transaction;

import java.time.Instant;

public record PowRequest(BlockId previousHash, Transaction[] transactions, Instant stamp) {
}
