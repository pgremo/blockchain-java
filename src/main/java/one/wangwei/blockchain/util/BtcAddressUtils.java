package one.wangwei.blockchain.util;

import org.bouncycastle.util.Arrays;

/**
 * 地址工具类
 *
 * @author wangwei
 * @date 2018/03/21
 */
public final class BtcAddressUtils {

    /**
     * 双重Hash
     *
     * @param data
     * @return
     */
    public static byte[] doubleHash(byte[] data) {
        return Hashes.sha256(Hashes.sha256(data));
    }

    /**
     * 计算公钥的 RIPEMD160 Hash值
     *
     * @param pubKey 公钥
     * @return ipeMD160Hash(sha256 ( pubkey))
     */
    public static byte[] ripeMD160Hash(byte[] pubKey) {
        return Hashes.ripemd160(Hashes.sha256(pubKey));
    }

    /**
     * 生成公钥的校验码
     *
     * @param payload
     * @return
     */
    public static byte[] checksum(byte[] payload) {
        return Arrays.copyOfRange(doubleHash(payload), 0, 4);
    }

}
