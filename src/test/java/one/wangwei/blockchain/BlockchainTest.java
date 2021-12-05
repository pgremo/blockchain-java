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
            String[] argss = {"createblockchain", "-address", "17b9Fu48NY65LJnM7xYMEsyro9vezJegjc"};
            // 1CceyiwYXh6vL6dLPw6WiNc5ihqVxwYHSA
            // 1G9TkDEp9YTnGa6gS5zaWkwGQwKrRykXcf
            // 1EKacQPNxTd8N7Y83VK11zoqm7bhUZiDHm
//            String[] argss = {"printaddresses"};
//            String[] argss = {"printchain"};
//            var argss = new String[]{"getbalance", "-address", "1KkLE2ETJ3iuVrrKaTyq9QhzLnxn1mGEuU"};
//            String[] argss = {"send", "-from", "1KkLE2ETJ3iuVrrKaTyq9QhzLnxn1mGEuU", "-to", "1FMJkyivY2xCqwcGv6h7RC9wToESCyoRGq", "-amount", "5"};
            var cli = new CLI(argss);
            cli.parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
