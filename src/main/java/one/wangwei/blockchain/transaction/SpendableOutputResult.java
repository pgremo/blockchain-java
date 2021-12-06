package one.wangwei.blockchain.transaction;

import java.util.Map;

/**
 * 查询结果
 *
 * @author wangwei
 * @date 2018/03/09
 */
public class SpendableOutputResult {
    /**
     * 交易时的支付金额
     */
    private int accumulated;
    /**
     * 未花费的交易
     */
    private Map<String, int[]> unspentOuts;

    /**
     * 交易时的支付金额
     */
    @SuppressWarnings("all")
    public int getAccumulated() {
        return this.accumulated;
    }

    /**
     * 未花费的交易
     */
    @SuppressWarnings("all")
    public Map<String, int[]> getUnspentOuts() {
        return this.unspentOuts;
    }

    /**
     * 交易时的支付金额
     */
    @SuppressWarnings("all")
    public void setAccumulated(final int accumulated) {
        this.accumulated = accumulated;
    }

    /**
     * 未花费的交易
     */
    @SuppressWarnings("all")
    public void setUnspentOuts(final Map<String, int[]> unspentOuts) {
        this.unspentOuts = unspentOuts;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SpendableOutputResult)) return false;
        final SpendableOutputResult other = (SpendableOutputResult) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getAccumulated() != other.getAccumulated()) return false;
        final Object this$unspentOuts = this.getUnspentOuts();
        final Object other$unspentOuts = other.getUnspentOuts();
        if (this$unspentOuts == null ? other$unspentOuts != null : !this$unspentOuts.equals(other$unspentOuts)) return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof SpendableOutputResult;
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getAccumulated();
        final Object $unspentOuts = this.getUnspentOuts();
        result = result * PRIME + ($unspentOuts == null ? 43 : $unspentOuts.hashCode());
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "SpendableOutputResult(accumulated=" + this.getAccumulated() + ", unspentOuts=" + this.getUnspentOuts() + ")";
    }

    @SuppressWarnings("all")
    public SpendableOutputResult(final int accumulated, final Map<String, int[]> unspentOuts) {
        this.accumulated = accumulated;
        this.unspentOuts = unspentOuts;
    }

    @SuppressWarnings("all")
    public SpendableOutputResult() {
    }
}
