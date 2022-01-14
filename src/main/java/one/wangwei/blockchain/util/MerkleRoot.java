package one.wangwei.blockchain.util;

import java.util.Deque;
import java.util.LinkedList;

import static one.wangwei.blockchain.util.Hashes.sha256;

public final class MerkleRoot {
    public static byte[] merkleRoot(Deque<byte[]> hashes) {
        if (hashes.isEmpty()) return hash(null, null);
        if (hashes.size() % 2 != 0) hashes.add(hashes.getLast());
        return iterate(hashes).poll();
    }

    public static Deque<byte[]> iterate(Deque<byte[]> hashes) {
        if (hashes.size() == 1) return hashes;
        var next = new LinkedList<byte[]>();
        while (!hashes.isEmpty()) {
            next.add(hash(hashes.poll(), hashes.poll()));
        }
        return iterate(next);
    }

    public static byte[] hash(byte[] left, byte[] right) {
        return right == null ? sha256(left) : sha256(left, right);
    }
}
