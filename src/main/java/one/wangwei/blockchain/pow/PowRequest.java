package one.wangwei.blockchain.pow;

import one.wangwei.blockchain.block.Block;
import one.wangwei.blockchain.transaction.Transaction;

import java.time.Instant;

public record PowRequest(Block.Id previousId, Transaction[] transactions, Instant stamp) {
}
