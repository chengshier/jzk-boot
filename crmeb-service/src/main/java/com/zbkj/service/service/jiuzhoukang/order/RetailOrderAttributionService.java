package com.zbkj.service.service.jiuzhoukang.order;

import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttribution;
import com.zbkj.common.model.order.StoreOrder;

import java.math.BigDecimal;
import java.util.List;

public interface RetailOrderAttributionService {
    /** 在订单创建完成后固化归属与订单明细实付分摊。 */
    List<JkRetailOrderAttribution> snapshot(StoreOrder order);

    /** 读取订单创建时快照；没有快照时不得使用当前关系兜底分佣。 */
    List<JkRetailOrderAttribution> listByOrder(Long orderId, String orderNo);

    /** 订单最终完成后锁定创建时归属，之后只能走补偿调整，不能直接改写。 */
    void lockByOrder(Long orderId, String orderNo);

    /** 按退款金额在仍可退款的明细实付基数之间分摊，并累计退款基数。 */
    List<RefundAllocation> allocateRefund(String orderNo, BigDecimal refundAmount, String requestNo);

    class RefundAllocation {
        private JkRetailOrderAttribution attribution;
        private BigDecimal refundBaseAmount;
        private BigDecimal beforeRefundedAmount;

        public JkRetailOrderAttribution getAttribution() { return attribution; }
        public RefundAllocation setAttribution(JkRetailOrderAttribution attribution) { this.attribution = attribution; return this; }
        public BigDecimal getRefundBaseAmount() { return refundBaseAmount; }
        public RefundAllocation setRefundBaseAmount(BigDecimal refundBaseAmount) { this.refundBaseAmount = refundBaseAmount; return this; }
        public BigDecimal getBeforeRefundedAmount() { return beforeRefundedAmount; }
        public RefundAllocation setBeforeRefundedAmount(BigDecimal beforeRefundedAmount) { this.beforeRefundedAmount = beforeRefundedAmount; return this; }
    }
}
