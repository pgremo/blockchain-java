package one.wangwei.blockchain.cli;

import picocli.CommandLine;

public class Main {
    public static void main(String... args) throws Exception {
        System.exit(new CommandLine(new PicoCli()).execute(args));
    }
}
