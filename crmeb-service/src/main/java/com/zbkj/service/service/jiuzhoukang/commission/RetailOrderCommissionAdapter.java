package com.zbkj.service.service.jiuzhoukang.commission;

import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttribution;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.service.service.jiuzhoukang.order.RetailOrderAttributionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * CRMEB 普通零售订单完成适配层。
 *
 * <p>只读取订单创建时的归属与实付分摊快照。快照缺失时宁可停止分佣并告警，
 * 也不能在订单完成时查询当前代理关系，避免用户换绑导致历史佣金错归属。</p>
 */
@Service
public class RetailOrderCommissionAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetailOrderCommissionAdapter.class);

    @Autowired private RetailOrderAttributionService attributionService;
    @Autowired private CommissionTriggerService triggerService;

    public void afterCrmebOrderCompleted(StoreOrder order) {
        if (order == null || order.getId() == null || order.getOrderId() == null) return;
        List<JkRetailOrderAttribution> snapshots = attributionService.listByOrder(order.getId().longValue(), order.getOrderId());
        if (snapshots == null || snapshots.isEmpty()) {
            LOGGER.error("九州康零售订单缺少下单归属快照，已停止分佣以避免错归属，orderId={}, orderNo={}", order.getId(), order.getOrderId());
            return;
        }
        int triggered = 0;
        for (JkRetailOrderAttribution snapshot : snapshots) {
            BigDecimal baseAmount = snapshot.getCommissionBaseAmount();
            if (snapshot.getReceiverUserId() == null || snapshot.getReceiverRoleCode() == null
                    || baseAmount == null || baseAmount.signum() <= 0) {
                continue;
            }
            triggerService.onRetailOrderCompleted(order.getId().longValue(), order.getOrderId(), snapshot.getOrderInfoId(),
                    snapshot.getReceiverUserId(), snapshot.getReceiverRoleCode(), baseAmount,
                    "RETAIL_ORDER_COMPLETED:" + order.getId() + ":" + snapshot.getOrderInfoId());
            triggered++;
        }
        LOGGER.info("九州康零售订单佣金事件已按下单快照提交，orderId={}, triggered={}", order.getId(), triggered);
    }
}
