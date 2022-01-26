package one.wangwei.blockchain.util;

import java.util.Deque;
import java.util.LinkedList;

import static one.wangwei.blockchain.util.Hashes.sha256;

public final class MerkleRoot {
    public static byte[] merkleRoot(Deque<byte[]> hashes) {
        return hashes.isEmpty() ? sha256(null) : iterate(hashes).poll();
    }

    public static Deque<byte[]> iterate(Deque<byte[]> hashes) {
        if (hashes.size() == 1) return hashes;
        if (hashes.size() % 2 != 0) hashes.add(hashes.getLast());
        var next = new LinkedList<byte[]>();
        while (!hashes.isEmpty()) {
            next.add(sha256(hashes.poll(), hashes.poll()));
        }
        return iterate(next);
    }
}
