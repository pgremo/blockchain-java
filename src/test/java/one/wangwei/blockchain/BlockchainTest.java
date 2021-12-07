package one.wangwei.blockchain;

import one.wangwei.blockchain.cli.CLI;

/**
 * 测试
 *
 * @author wangwei
 */
public class BlockchainTest {

    public static void main(String[] args) {
        try {
//            String[] argss = {"createwallet"};
//            String[] argss = {"createblockchain", "-address", "1Aup3oweKvFd4upy2pZS9aKjn67SQgzbSC"};
            // 1CceyiwYXh6vL6dLPw6WiNc5ihqVxwYHSA
            // 1G9TkDEp9YTnGa6gS5zaWkwGQwKrRykXcf
            // 1EKacQPNxTd8N7Y83VK11zoqm7bhUZiDHm
//            String[] argss = {"printaddresses"};
//            String[] argss = {"printchain"};
            var argss = new String[]{"getbalance", "-address", "1FfxJLfBGQuPN6vhaSoz278rfeC9GfAX2"};
//            String[] argss = {"send", "-from", "1Aup3oweKvFd4upy2pZS9aKjn67SQgzbSC", "-to", "1FfxJLfBGQuPN6vhaSoz278rfeC9GfAX2", "-amount", "5"};
            new CLI(argss).parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
