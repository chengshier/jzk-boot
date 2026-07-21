package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRuleItem;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.common.model.order.StoreOrderInfo;
import com.zbkj.service.service.StoreOrderInfoService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionAccountService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionCalculateService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionCalculateSupport;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class CommissionCalculateServiceImpl implements CommissionCalculateService {
    @Autowired private JkCommissionRecordDao recordDao;
    @Autowired private CommissionRuleService commissionRuleService;
    @Autowired private CommissionAccountService commissionAccountService;
    @Autowired private StoreOrderInfoService orderInfoService;

    @Override @Transactional
    public List<JkCommissionRecord> calculateRetailOrder(Long orderId, String orderNo, Long orderInfoId, Long receiverUserId,
                                                           String receiverRoleCode, BigDecimal orderAmount, String requestNo) {
        if (orderId == null || receiverUserId == null || orderAmount == null || orderAmount.signum() < 0) {
            throw new IllegalArgumentException("零售订单佣金参数非法");
        }
        List<JkCommissionRecord> result = new ArrayList<>();
        for (JkCommissionRule rule : commissionRuleService.listActiveRules("RETAIL_ORDER", receiverRoleCode)) {
            Long sourceId = orderInfoId == null ? orderId : orderInfoId;
            String key = CommissionCalculateSupport.buildIdempotencyKey("RETAIL_ORDER", sourceId, receiverUserId, rule.getId());
            JkCommissionRecord old = recordDao.selectOne(new LambdaQueryWrapper<JkCommissionRecord>().eq(JkCommissionRecord::getIdempotencyKey, key));
            if (old != null) { result.add(old); continue; }
List<JkCommissionRuleItem> items = commissionRuleService.listItems(rule.getId());
            JkCommissionRuleItem item = null;
StoreOrderInfo orderInfo = orderInfoId == null ? null : orderInfoService.getById(orderInfoId);
            for (JkCommissionRuleItem candidate : items) {
                if (!Boolean.TRUE.equals(candidate.getStatus()) || (candidate.getReceiverRoleCode() != null && !candidate.getReceiverRoleCode().equals(receiverRoleCode))) continue;
                if (orderInfo != null && candidate.getProductId() != null && !candidate.getProductId().equals(orderInfo.getProductId())) continue;
                if (orderInfo != null && candidate.getSkuId() != null && !candidate.getSkuId().equals(orderInfo.getAttrValueId())) continue;
                if (orderInfo == null && (candidate.getProductId() != null || candidate.getSkuId() != null)) continue;
                item = candidate; break;
            }
            BigDecimal amount = item == null ? CommissionCalculateSupport.calculateFromRuleConfig(orderAmount, rule.getRuleConfigJson()) : ("PERCENT".equals(item.getCalculationType()) ? CommissionCalculateSupport.calculatePercent(orderAmount, item.getCommissionRate()) : item.getFixedAmount().setScale(2, java.math.RoundingMode.HALF_UP));
            if (amount.signum() == 0) continue;
            Date now = new Date();
            Date freezeEnd = freezeEnd(now, rule.getFreezeDays());
            JkCommissionRecord record = new JkCommissionRecord().setCommissionNo("CM" + id()).setSourceType("RETAIL_ORDER")
                    .setSourceId(sourceId).setSourceNo(orderNo).setReceiverUserId(receiverUserId).setReceiverRoleCode(receiverRoleCode)
                    .setRuleId(rule.getId()).setRuleVersion(rule.getRuleVersion()).setBaseAmount(orderAmount)
                    .setCommissionAmount(amount).setSettledAmount(BigDecimal.ZERO).setStatus("PENDING_SETTLE").setFreezeEndTime(freezeEnd)
                    .setRuleSnapshotJson("{\"orderId\":" + orderId + ",\"orderInfoId\":" + (orderInfoId == null ? "null" : orderInfoId) + ",\"rule\":" + rule.getRuleConfigJson() + "}").setIdempotencyKey(key).setRequestNo(requestNo).setIsDeleted(false)
                    .setCreateTime(now).setUpdateTime(now);
            recordDao.insert(record);
            commissionAccountService.creditPending(receiverUserId, receiverRoleCode, amount, requestNo, "COMMISSION_RECORD:" + key);
            result.add(record);
        }
        return result;
    }

    private Date freezeEnd(Date now, Integer days) {
        if (days == null || days <= 0) return now;
        Calendar calendar = Calendar.getInstance(); calendar.setTime(now); calendar.add(Calendar.DAY_OF_MONTH, days); return calendar.getTime();
    }
    private String id() { return UUID.randomUUID().toString().replace("-", "").substring(0, 16); }
}