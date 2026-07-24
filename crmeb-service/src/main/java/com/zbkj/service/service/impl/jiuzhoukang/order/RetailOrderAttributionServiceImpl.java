package com.zbkj.service.service.impl.jiuzhoukang.order;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.JkAgentRelation;
import com.zbkj.common.model.jiuzhoukang.JkRegionAgent;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttribution;
import com.zbkj.common.model.jiuzhoukang.JkRetailRefundAdjustment;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.order.StoreOrderInfo;
import com.zbkj.service.dao.jiuzhoukang.JkAgentRelationDao;
import com.zbkj.service.dao.jiuzhoukang.JkRegionAgentDao;
import com.zbkj.service.dao.jiuzhoukang.JkRetailOrderAttributionDao;
import com.zbkj.service.dao.jiuzhoukang.JkRetailRefundAdjustmentDao;
import com.zbkj.service.service.StoreOrderInfoService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.order.RetailOrderAttributionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * CRMEB 普通零售订单的归属与最终实付快照服务。
 * <p>快照在订单创建后固化；订单完成和退款只能读取快照，禁止重新查询当前上下级关系，
 * 从而保证用户换绑、区域调整和规则变更不会改写历史订单归属。</p>
 */
@Service
public class RetailOrderAttributionServiceImpl implements RetailOrderAttributionService {
    private static final String TYPE_DIRECT_PARENT = "DIRECT_PARENT";
    private static final String TYPE_REGION_AGENT = "REGION_AGENT";
    private static final String TYPE_PLATFORM = "PLATFORM";

    @Autowired private JkRetailOrderAttributionDao attributionDao;
    @Autowired private JkRetailRefundAdjustmentDao refundAdjustmentDao;
    @Autowired private JkAgentRelationDao relationDao;
    @Autowired private JkRegionAgentDao regionAgentDao;
    @Autowired private JkUserContextService contextService;
    @Autowired private StoreOrderInfoService orderInfoService;

    /**
     * 在订单创建后固化逐明细归属和实付分摊。扩展快照失败由上层记录，但不能让后续分佣回退到“查询当前关系”。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<JkRetailOrderAttribution> snapshot(StoreOrder order) {
        if (order == null || order.getId() == null || order.getUid() == null || order.getOrderId() == null) {
            throw new IllegalArgumentException("零售订单归属快照参数不完整");
        }
        List<JkRetailOrderAttribution> existing = listByOrder(order.getId().longValue(), order.getOrderId());
        if (!existing.isEmpty()) return existing;

        List<StoreOrderInfo> items = orderInfoService.getListByOrderNo(order.getOrderId());
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("零售订单明细不存在，不能生成归属快照");

        AttributionReceiver receiver = resolveReceiver(order.getUid().longValue());
        List<BigDecimal> originals = new ArrayList<>();
        BigDecimal originalTotal = BigDecimal.ZERO;
        for (StoreOrderInfo item : items) {
            BigDecimal original = safe(item.getPrice()).multiply(BigDecimal.valueOf(item.getPayNum() == null ? 0 : item.getPayNum())).setScale(2, RoundingMode.HALF_UP);
            originals.add(original);
            originalTotal = originalTotal.add(original);
        }
        BigDecimal productPaid = safe(order.getPayPrice()).subtract(safe(order.getPayPostage())).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        if (originalTotal.signum() > 0 && productPaid.compareTo(originalTotal) > 0) productPaid = originalTotal;

        Date now = new Date();
        BigDecimal allocated = BigDecimal.ZERO;
        List<JkRetailOrderAttribution> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            StoreOrderInfo item = items.get(i);
            BigDecimal original = originals.get(i);
            BigDecimal paid;
            if (i == items.size() - 1) {
                paid = productPaid.subtract(allocated).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            } else if (originalTotal.signum() == 0) {
                paid = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                paid = productPaid.multiply(original).divide(originalTotal, 2, RoundingMode.HALF_UP);
                allocated = allocated.add(paid);
            }
            String key = "RETAIL_ATTRIBUTION:" + order.getId() + ":" + item.getId();
            JSONObject snapshot = new JSONObject(true);
            snapshot.put("orderCreateTime", order.getCreateTime());
            snapshot.put("orderPayPrice", order.getPayPrice());
            snapshot.put("orderPayPostage", order.getPayPostage());
            snapshot.put("relationId", receiver.relationId);
            snapshot.put("relationSource", receiver.relationSource);
            snapshot.put("attributionType", receiver.attributionType);
            snapshot.put("receiverUserId", receiver.receiverUserId);
            snapshot.put("receiverRoleCode", receiver.receiverRoleCode);

            JkRetailOrderAttribution row = new JkRetailOrderAttribution()
                    .setOrderId(order.getId().longValue()).setOrderNo(order.getOrderId()).setOrderInfoId(item.getId().longValue())
                    .setBuyerUserId(order.getUid().longValue()).setDirectParentUserId(receiver.directParentUserId)
                    .setCountyAgentUserId(receiver.countyAgentUserId).setRegionCode(receiver.regionCode)
                    .setAttributionType(receiver.attributionType).setRelationId(receiver.relationId).setRelationSource(receiver.relationSource)
                    .setReceiverUserId(receiver.receiverUserId).setReceiverRoleCode(receiver.receiverRoleCode)
                    .setItemOriginalAmount(original).setItemDiscountAmount(original.subtract(paid).max(BigDecimal.ZERO))
                    .setItemPaidAmount(paid).setRefundedAmount(BigDecimal.ZERO).setCommissionBaseAmount(paid)
                    .setSnapshotJson(snapshot.toJSONString()).setRequestNo("RETAIL_ORDER_CREATED:" + order.getId())
                    .setIdempotencyKey(key).setVersion(0).setStatus(true).setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
            attributionDao.insert(row);
            result.add(row);
        }
        return result;
    }

    @Override
    public List<JkRetailOrderAttribution> listByOrder(Long orderId, String orderNo) {
        LambdaQueryWrapper<JkRetailOrderAttribution> query = new LambdaQueryWrapper<JkRetailOrderAttribution>()
                .eq(JkRetailOrderAttribution::getIsDeleted, false)
                .orderByAsc(JkRetailOrderAttribution::getOrderInfoId);
        if (orderId != null) query.eq(JkRetailOrderAttribution::getOrderId, orderId);
        else query.eq(JkRetailOrderAttribution::getOrderNo, orderNo);
        return attributionDao.selectList(query);
    }

    /**
     * 按原实付快照分摊退款，最后一条承接舍入尾差；返回结果供冲正服务定位原佣金。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RefundAllocation> allocateRefund(String orderNo, BigDecimal refundAmount, String requestNo) {
        if (orderNo == null || requestNo == null || refundAmount == null || refundAmount.signum() <= 0) {
            throw new IllegalArgumentException("退款归属分摊参数非法");
        }
        List<JkRetailOrderAttribution> rows = listByOrder(null, orderNo);
        if (rows.isEmpty()) return new ArrayList<>();
        boolean alreadyProcessed = true;
        for (JkRetailOrderAttribution row : rows) {
            if (!requestNo.equals(row.getLastRefundRequestNo())) { alreadyProcessed = false; break; }
        }
        if (alreadyProcessed) return new ArrayList<>();

        BigDecimal remainingTotal = BigDecimal.ZERO;
        for (JkRetailOrderAttribution row : rows) {
            remainingTotal = remainingTotal.add(safe(row.getItemPaidAmount()).subtract(safe(row.getRefundedAmount())).max(BigDecimal.ZERO));
        }
        BigDecimal toAllocate = refundAmount.min(remainingTotal).setScale(2, RoundingMode.HALF_UP);
        if (toAllocate.signum() <= 0) return new ArrayList<>();

        int lastEligibleIndex = -1;
        for (int i = 0; i < rows.size(); i++) {
            JkRetailOrderAttribution row = rows.get(i);
            if (safe(row.getItemPaidAmount()).subtract(safe(row.getRefundedAmount())).signum() > 0) {
                lastEligibleIndex = i;
            }
        }

        List<RefundAllocation> result = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < rows.size(); i++) {
            JkRetailOrderAttribution row = rows.get(i);
            BigDecimal beforeRefunded = safe(row.getRefundedAmount());
            BigDecimal remaining = safe(row.getItemPaidAmount()).subtract(beforeRefunded).max(BigDecimal.ZERO);
            BigDecimal part;
            if (remaining.signum() == 0) {
                part = BigDecimal.ZERO;
            } else if (i == lastEligibleIndex) {
                part = toAllocate.subtract(allocated).min(remaining).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            } else {
                part = remainingTotal.signum() == 0 ? BigDecimal.ZERO : toAllocate.multiply(remaining).divide(remainingTotal, 2, RoundingMode.HALF_UP).min(remaining);
                allocated = allocated.add(part);
            }
            int version = row.getVersion() == null ? 0 : row.getVersion();
            BigDecimal afterRefunded = beforeRefunded.add(part);
            int updated = attributionDao.update(null, new LambdaUpdateWrapper<JkRetailOrderAttribution>()
                    .eq(JkRetailOrderAttribution::getId, row.getId())
                    .eq(JkRetailOrderAttribution::getVersion, version)
                    .set(JkRetailOrderAttribution::getRefundedAmount, afterRefunded)
                    .set(JkRetailOrderAttribution::getLastRefundRequestNo, requestNo)
                    .set(JkRetailOrderAttribution::getVersion, version + 1)
                    .set(JkRetailOrderAttribution::getUpdateTime, new Date()));
            if (updated != 1) throw new IllegalStateException("零售退款分摊版本冲突，请重试");
            row.setRefundedAmount(afterRefunded).setLastRefundRequestNo(requestNo).setVersion(version + 1);
            if (part.signum() > 0) {
                // 累计退款值用于佣金冲正上限；独立发生额用于当期报表。两者必须在同一事务内写入。
                Date occurred = new Date();
                String adjustmentKey = requestNo + ":" + row.getId();
                JkRetailRefundAdjustment adjustment = new JkRetailRefundAdjustment()
                        .setAdjustmentNo("RRA" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr())
                        .setRequestNo(requestNo).setOrderId(row.getOrderId()).setOrderNo(row.getOrderNo())
                        .setOrderInfoId(row.getOrderInfoId()).setAttributionId(row.getId())
                        .setBuyerUserId(row.getBuyerUserId()).setReceiverUserId(row.getReceiverUserId())
                        .setCountyAgentUserId(row.getCountyAgentUserId()).setRegionCode(row.getRegionCode())
                        .setAdjustmentAmount(part).setOccurredTime(occurred).setOriginalBusinessTime(row.getCreateTime())
                        .setIdempotencyKey(adjustmentKey).setIsDeleted(false).setCreateTime(occurred).setUpdateTime(occurred);
                refundAdjustmentDao.insert(adjustment);
                result.add(new RefundAllocation().setAttribution(row).setBeforeRefundedAmount(beforeRefunded).setRefundBaseAmount(part));
            }
        }
        return result;
    }

    private AttributionReceiver resolveReceiver(Long buyerUserId) {
        JkUserContext buyerContext = contextService.getFrontContext(buyerUserId);
        JkAgentRelation relation = relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>()
                .eq(JkAgentRelation::getUserId, buyerUserId).eq(JkAgentRelation::getStatus, true)
                .eq(JkAgentRelation::getIsDeleted, false).isNotNull(JkAgentRelation::getParentUserId)
                .orderByDesc(JkAgentRelation::getId).last("limit 1"));
        if (relation != null) {
            JkUserContext parent = contextService.getFrontContext(relation.getParentUserId());
            if (isCommissionReceiver(parent)) {
                return new AttributionReceiver(TYPE_DIRECT_PARENT, relation.getParentUserId(),
                        JkBizConstants.ROLE_COUNTY_AGENT.equals(parent.getPrimaryRoleCode()) ? relation.getParentUserId() : parent.getBelongCountyAgentId(),
                        parent.getRegionCode(), relation.getId(), relation.getRelationType(), relation.getParentUserId(), parent.getPrimaryRoleCode());
            }
        }
        String regionCode = buyerContext == null ? null : buyerContext.getRegionCode();
        if (regionCode != null && !regionCode.trim().isEmpty()) {
            JkRegionAgent regionAgent = regionAgentDao.selectOne(new LambdaQueryWrapper<JkRegionAgent>()
                    .eq(JkRegionAgent::getRegionCode, regionCode).eq(JkRegionAgent::getStatus, true)
                    .eq(JkRegionAgent::getIsDeleted, false).isNotNull(JkRegionAgent::getCountyAgentUserId)
                    .orderByDesc(JkRegionAgent::getId).last("limit 1"));
            if (regionAgent != null && isEffectiveBind(regionAgent.getBindStatus())) {
                JkUserContext county = contextService.getFrontContext(regionAgent.getCountyAgentUserId());
                if (isCommissionReceiver(county) && JkBizConstants.ROLE_COUNTY_AGENT.equals(county.getPrimaryRoleCode())) {
                    return new AttributionReceiver(TYPE_REGION_AGENT, null, regionAgent.getCountyAgentUserId(), regionCode,
                            null, "REGION_AGENT", regionAgent.getCountyAgentUserId(), county.getPrimaryRoleCode());
                }
            }
        }
        return new AttributionReceiver(TYPE_PLATFORM, null, null, regionCode, null, "PLATFORM_DEFAULT", null, null);
    }

    private boolean isCommissionReceiver(JkUserContext context) {
        if (context == null || Boolean.TRUE.equals(context.getFreezeStatus()) || context.getPrimaryRoleCode() == null) return false;
        String role = context.getPrimaryRoleCode();
        return JkBizConstants.ROLE_MAKER.equals(role) || JkBizConstants.ROLE_PARTNER.equals(role) || JkBizConstants.ROLE_COUNTY_AGENT.equals(role);
    }

    private boolean isEffectiveBind(String bindStatus) {
        if (bindStatus == null) return true;
        return !("UNBOUND".equalsIgnoreCase(bindStatus) || "DISABLED".equalsIgnoreCase(bindStatus)
                || "INVALID".equalsIgnoreCase(bindStatus) || "EXPIRED".equalsIgnoreCase(bindStatus));
    }

    private BigDecimal safe(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private static class AttributionReceiver {
        private final String attributionType;
        private final Long directParentUserId;
        private final Long countyAgentUserId;
        private final String regionCode;
        private final Long relationId;
        private final String relationSource;
        private final Long receiverUserId;
        private final String receiverRoleCode;

        private AttributionReceiver(String attributionType, Long directParentUserId, Long countyAgentUserId, String regionCode,
                                    Long relationId, String relationSource, Long receiverUserId, String receiverRoleCode) {
            this.attributionType = attributionType; this.directParentUserId = directParentUserId;
            this.countyAgentUserId = countyAgentUserId; this.regionCode = regionCode; this.relationId = relationId;
            this.relationSource = relationSource; this.receiverUserId = receiverUserId; this.receiverRoleCode = receiverRoleCode;
        }
    }
}
