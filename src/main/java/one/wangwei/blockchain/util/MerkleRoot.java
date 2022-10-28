package one.wangwei.blockchain.util;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

import static one.wangwei.blockchain.util.Hashes.sha256;

public final class MerkleRoot {
    public static byte[] merkleRoot(Deque<byte[]> hashes) {
        return hashes.isEmpty() ? sha256(null) : iterate(hashes).poll();
    }

    public static Queue<byte[]> iterate(Queue<byte[]> hashes) {
        if (hashes.size() == 1) return hashes;
        var next = new LinkedList<byte[]>();
        while (!hashes.isEmpty()) {
            var first = hashes.poll();
            next.add(sha256(first, hashes.isEmpty() ? first : hashes.poll()));
        }
        return iterate(next);
    }
}
