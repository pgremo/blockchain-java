package one.wangwei.blockchain.block;

import lombok.Getter;
import one.wangwei.blockchain.transaction.Transaction;
import one.wangwei.blockchain.util.RocksDBUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * <p> 区块链 </p>
 *
 * @author wangwei
 * @date 2018/02/02
 */
public class Blockchain {

    @Getter
    private String lastBlockHash;

    private Blockchain(String lastBlockHash) {
        this.lastBlockHash = lastBlockHash;
    }

    /**
     * <p> 创建区块链 </p>
     *
     * @param address 钱包地址
     * @return
     */
    public static Blockchain newBlockchain(String address) throws Exception {
        String lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        if (StringUtils.isBlank(lastBlockHash)) {
            // 创建 coinBase 交易
            Transaction coinbaseTX = Transaction.newCoinbaseTX(address, "");
            Block genesisBlock = Block.newGenesisBlock(coinbaseTX);
            lastBlockHash = genesisBlock.getHash();
            RocksDBUtils.getInstance().putBlock(genesisBlock);
            RocksDBUtils.getInstance().putLastBlockHash(lastBlockHash);
        }
        return new Blockchain(lastBlockHash);
    }

//    /**
//     * <p> 添加区块  </p>
//     *
//     * @param data
//     */
//    public void addBlock(String data) throws Exception {
//        String lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
//        if (StringUtils.isBlank(lastBlockHash)) {
//            throw new Exception("Fail to add block into blockchain ! ");
//        }
//        this.addBlock(Block.newBlock(lastBlockHash, data));
//    }

    /**
     * <p> 添加区块  </p>
     *
     * @param block
     */
    public void addBlock(Block block) throws Exception {
        RocksDBUtils.getInstance().putLastBlockHash(block.getHash());
        RocksDBUtils.getInstance().putBlock(block);
        this.lastBlockHash = block.getHash();
    }


    /**
     * 区块链迭代器
     */
    public class BlockchainIterator {

        private String currentBlockHash;

        public BlockchainIterator(String currentBlockHash) {
            this.currentBlockHash = currentBlockHash;
        }

        /**
         * 是否有下一个区块
         *
         * @return
         */
        public boolean hashNext() throws Exception {
            if (StringUtils.isBlank(currentBlockHash)) {
                return false;
            }
            Block lastBlock = RocksDBUtils.getInstance().getBlock(currentBlockHash);
            if (lastBlock == null) {
                return false;
            }
            // 创世区块直接放行
            if (lastBlock.getPrevBlockHash().length() == 0) {
                return true;
            }
            return RocksDBUtils.getInstance().getBlock(lastBlock.getPrevBlockHash()) != null;
        }


        /**
         * 返回区块
         *
         * @return
         */
        public Block next() throws Exception {
            Block currentBlock = RocksDBUtils.getInstance().getBlock(currentBlockHash);
            if (currentBlock != null) {
                this.currentBlockHash = currentBlock.getPrevBlockHash();
                return currentBlock;
            }
            return null;
        }
    }

    public BlockchainIterator getBlockchainIterator() {
        return new BlockchainIterator(lastBlockHash);
    }


}
