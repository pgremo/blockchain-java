package one.wangwei.blockchain.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.Security;

import static org.junit.Assert.*;

public class HashesTest {

    @Test
    public void shouldFindRIPEMD160(){
        Security.addProvider(new BouncyCastleProvider());
        Hashes.ripemd160("Hello".getBytes(StandardCharsets.UTF_8));
    }

}