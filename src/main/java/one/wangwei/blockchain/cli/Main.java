package one.wangwei.blockchain.cli;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public class Main {

    public static void main(String... args) {
        Security.addProvider(new BouncyCastleProvider());
        new CLI(args).parse();
    }
}
