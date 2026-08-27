package com.zbkj.service.service.impl.jiuzhoukang.order;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.JkAgentRelation;
import com.zbkj.common.model.jiuzhoukang.JkRegionAgent;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttribution;
import com.zbkj.common.model.jiuzhoukang.JkRetailRefundAdjustment;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.order.StoreOrderInfo;
import com.zbkj.common.model.user.User;
import com.zbkj.common.response.jiuzhoukang.JkRegionPathResponse;
import com.zbkj.service.dao.jiuzhoukang.JkAgentRelationDao;
import com.zbkj.service.dao.jiuzhoukang.JkRegionAgentDao;
import com.zbkj.service.dao.jiuzhoukang.JkRetailOrderAttributionDao;
import com.zbkj.service.dao.jiuzhoukang.JkRetailRefundAdjustmentDao;
import com.zbkj.service.service.StoreOrderInfoService;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.order.RetailOrderAttributionService;
import com.zbkj.service.service.jiuzhoukang.region.JkRegionService;
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
 * 优先级固定为：下单时有效直属关系 -> 用户个人资料标准区域 -> 本单标准收货区域 -> 平台默认。
 * 佣金、业绩和退款只能读取这里写入的快照，禁止重新查询当前关系替代历史快照。
 */
@Service
public class RetailOrderAttributionServiceImpl implements RetailOrderAttributionService {
    private static final String TYPE_DIRECT_PARENT = "DIRECT_PARENT";
    private static final String TYPE_REGION_AGENT = "REGION_AGENT";
    private static final String TYPE_REGION_ONLY = "REGION_ONLY";
    private static final String TYPE_PLATFORM = "PLATFORM";

    private static final String SOURCE_RELATION = "RELATION";
    private static final String SOURCE_PROFILE = "USER_PROFILE";
    private static final String SOURCE_SHIPPING = "ORDER_ADDRESS_FALLBACK";
    private static final String SOURCE_PLATFORM = "PLATFORM_DEFAULT";

    @Autowired private JkRetailOrderAttributionDao attributionDao;
    @Autowired private JkRetailRefundAdjustmentDao refundAdjustmentDao;
    @Autowired private JkAgentRelationDao relationDao;
    @Autowired private JkRegionAgentDao regionAgentDao;
    @Autowired private JkUserContextService contextService;
    @Autowired private StoreOrderInfoService orderInfoService;
    @Autowired private UserService userService;
    @Autowired private JkRegionService regionService;

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

        AttributionReceiver receiver = resolveReceiver(order);
        List<BigDecimal> originals = new ArrayList<BigDecimal>();
        BigDecimal originalTotal = BigDecimal.ZERO;
        for (StoreOrderInfo item : items) {
            BigDecimal original = safe(item.getPrice()).multiply(BigDecimal.valueOf(item.getPayNum() == null ? 0 : item.getPayNum()))
                    .setScale(2, RoundingMode.HALF_UP);
            originals.add(original);
            originalTotal = originalTotal.add(original);
        }
        BigDecimal productPaid = safe(order.getPayPrice()).subtract(safe(order.getPayPostage())).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        if (originalTotal.signum() > 0 && productPaid.compareTo(originalTotal) > 0) productPaid = originalTotal;

        Date now = new Date();
        BigDecimal allocated = BigDecimal.ZERO;
        List<JkRetailOrderAttribution> result = new ArrayList<JkRetailOrderAttribution>();
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

            JSONObject relationSnapshot = new JSONObject(true);
            relationSnapshot.put("relationId", receiver.relationId);
            relationSnapshot.put("relationSource", receiver.relationSource);
            relationSnapshot.put("directParentUserId", receiver.directParentUserId);
            relationSnapshot.put("directParentRoleCode", receiver.directParentRoleCode);
            relationSnapshot.put("countyAgentUserId", receiver.countyAgentUserId);

            JSONObject profileSnapshot = new JSONObject(true);
            profileSnapshot.put("buyerUserId", order.getUid());
            profileSnapshot.put("profileRegionCode", receiver.profileRegionCode);

            JSONObject shippingSnapshot = new JSONObject(true);
            shippingSnapshot.put("shippingAddressId", order.getJkShippingAddressId());
            shippingSnapshot.put("shippingRegionCode", receiver.shippingRegionCode);
            shippingSnapshot.put("receiverName", order.getRealName());
            shippingSnapshot.put("receiverPhone", order.getUserPhone());
            shippingSnapshot.put("addressText", order.getUserAddress());

            JSONObject resolutionSnapshot = new JSONObject(true);
            resolutionSnapshot.put("priority", "RELATION>USER_PROFILE>ORDER_ADDRESS_FALLBACK>PLATFORM_DEFAULT");
            resolutionSnapshot.put("profileRegionCode", receiver.profileRegionCode);
            resolutionSnapshot.put("shippingRegionCode", receiver.shippingRegionCode);
            resolutionSnapshot.put("finalRegionCode", receiver.finalRegionCode);
            resolutionSnapshot.put("finalRegionName", receiver.finalRegionName);
            resolutionSnapshot.put("regionSourceType", receiver.regionSourceType);
            resolutionSnapshot.put("reason", receiver.reason);

            JSONObject priceSnapshot = new JSONObject(true);
            priceSnapshot.put("unitPrice", item.getPrice());
            priceSnapshot.put("quantity", item.getPayNum());
            priceSnapshot.put("itemOriginalAmount", original);
            priceSnapshot.put("itemPaidAmount", paid);
            priceSnapshot.put("freightExcluded", true);

            JSONObject aggregate = new JSONObject(true);
            aggregate.put("orderCreateTime", order.getCreateTime());
            aggregate.put("relation", relationSnapshot);
            aggregate.put("profile", profileSnapshot);
            aggregate.put("shipping", shippingSnapshot);
            aggregate.put("regionResolution", resolutionSnapshot);
            aggregate.put("price", priceSnapshot);

            String key = "RETAIL_ATTRIBUTION:" + order.getId() + ":" + item.getId();
            JkRetailOrderAttribution row = new JkRetailOrderAttribution()
                    .setAttributionNo("RA" + IdWorker.getIdStr())
                    .setOrderId(order.getId().longValue()).setOrderNo(order.getOrderId())
                    .setOrderInfoId(item.getId().longValue()).setBuyerUserId(order.getUid().longValue())
                    .setProductId(item.getProductId() == null ? null : item.getProductId().longValue())
                    .setSkuId(item.getAttrValueId() == null ? null : item.getAttrValueId().longValue())
                    .setQuantity(item.getPayNum())
                    .setDirectParentUserId(receiver.directParentUserId).setDirectParentRoleCode(receiver.directParentRoleCode)
                    .setCountyAgentUserId(receiver.countyAgentUserId).setReceiverUserId(receiver.receiverUserId)
                    .setReceiverRoleCode(receiver.receiverRoleCode)
                    .setProfileRegionCode(receiver.profileRegionCode).setShippingRegionCode(receiver.shippingRegionCode)
                    .setFinalRegionCode(receiver.finalRegionCode).setFinalRegionNameSnapshot(receiver.finalRegionName)
                    .setRegionCode(receiver.finalRegionCode).setRegionSourceType(receiver.regionSourceType)
                    .setAttributionType(receiver.attributionType).setRelationId(receiver.relationId)
                    .setRelationSource(receiver.relationSource).setShippingAddressId(order.getJkShippingAddressId() == null ? null : order.getJkShippingAddressId().longValue())
                    .setItemOriginalAmount(original).setItemDiscountAmount(original.subtract(paid).max(BigDecimal.ZERO))
                    .setItemPaidAmount(paid).setFreightAllocatedAmount(BigDecimal.ZERO)
                    .setRefundedAmount(BigDecimal.ZERO).setRefundAmount(BigDecimal.ZERO).setCommissionBaseAmount(paid)
                    .setRelationSnapshotJson(relationSnapshot.toJSONString()).setProfileSnapshotJson(profileSnapshot.toJSONString())
                    .setShippingAddressSnapshotJson(shippingSnapshot.toJSONString())
                    .setRegionResolutionSnapshotJson(resolutionSnapshot.toJSONString())
                    .setPriceSnapshotJson(priceSnapshot.toJSONString()).setRuleContextSnapshotJson("{}")
                    .setSnapshotJson(aggregate.toJSONString()).setAttributionStatus("RESOLVED").setLockStatus("UNLOCKED")
                    .setRequestNo("RETAIL_ORDER_CREATED:" + order.getId()).setIdempotencyKey(key)
                    .setVersion(0).setStatus(true).setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
            attributionDao.insert(row);
            result.add(row);
        }
        return result;
    }

    @Override
    public void lockByOrder(Long orderId, String orderNo) {
        if (orderId == null || orderNo == null) return;
        attributionDao.update(null, new LambdaUpdateWrapper<JkRetailOrderAttribution>()
                .eq(JkRetailOrderAttribution::getOrderId, orderId)
                .eq(JkRetailOrderAttribution::getOrderNo, orderNo)
                .eq(JkRetailOrderAttribution::getIsDeleted, false)
                .ne(JkRetailOrderAttribution::getLockStatus, "LOCKED")
                .set(JkRetailOrderAttribution::getLockStatus, "LOCKED")
                .set(JkRetailOrderAttribution::getUpdateTime, new Date()));
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RefundAllocation> allocateRefund(String orderNo, BigDecimal refundAmount, String requestNo) {
        if (orderNo == null || requestNo == null || refundAmount == null || refundAmount.signum() <= 0) {
            throw new IllegalArgumentException("退款归属分摊参数非法");
        }
        List<JkRetailOrderAttribution> rows = listByOrder(null, orderNo);
        if (rows.isEmpty()) return new ArrayList<RefundAllocation>();
        boolean alreadyProcessed = true;
        for (JkRetailOrderAttribution row : rows) {
            if (!requestNo.equals(row.getLastRefundRequestNo())) { alreadyProcessed = false; break; }
        }
        if (alreadyProcessed) return new ArrayList<RefundAllocation>();

        BigDecimal remainingTotal = BigDecimal.ZERO;
        for (JkRetailOrderAttribution row : rows) {
            remainingTotal = remainingTotal.add(safe(row.getItemPaidAmount()).subtract(safe(row.getRefundedAmount())).max(BigDecimal.ZERO));
        }
        BigDecimal toAllocate = refundAmount.min(remainingTotal).setScale(2, RoundingMode.HALF_UP);
        if (toAllocate.signum() <= 0) return new ArrayList<RefundAllocation>();

        int lastEligibleIndex = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (safe(rows.get(i).getItemPaidAmount()).subtract(safe(rows.get(i).getRefundedAmount())).signum() > 0) lastEligibleIndex = i;
        }

        List<RefundAllocation> result = new ArrayList<RefundAllocation>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < rows.size(); i++) {
            JkRetailOrderAttribution row = rows.get(i);
            BigDecimal beforeRefunded = safe(row.getRefundedAmount());
            BigDecimal remaining = safe(row.getItemPaidAmount()).subtract(beforeRefunded).max(BigDecimal.ZERO);
            BigDecimal part;
            if (remaining.signum() == 0) part = BigDecimal.ZERO;
            else if (i == lastEligibleIndex) part = toAllocate.subtract(allocated).min(remaining).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            else {
                part = remainingTotal.signum() == 0 ? BigDecimal.ZERO
                        : toAllocate.multiply(remaining).divide(remainingTotal, 2, RoundingMode.HALF_UP).min(remaining);
                allocated = allocated.add(part);
            }
            int version = row.getVersion() == null ? 0 : row.getVersion();
            BigDecimal afterRefunded = beforeRefunded.add(part);
            int updated = attributionDao.update(null, new LambdaUpdateWrapper<JkRetailOrderAttribution>()
                    .eq(JkRetailOrderAttribution::getId, row.getId())
                    .eq(JkRetailOrderAttribution::getVersion, version)
                    .set(JkRetailOrderAttribution::getRefundedAmount, afterRefunded)
                    .set(JkRetailOrderAttribution::getRefundAmount, afterRefunded)
                    .set(JkRetailOrderAttribution::getLastRefundRequestNo, requestNo)
                    .set(JkRetailOrderAttribution::getVersion, version + 1)
                    .set(JkRetailOrderAttribution::getUpdateTime, new Date()));
            if (updated != 1) throw new IllegalStateException("零售退款分摊版本冲突，请重试");
            row.setRefundedAmount(afterRefunded).setRefundAmount(afterRefunded)
                    .setLastRefundRequestNo(requestNo).setVersion(version + 1);
            if (part.signum() > 0) {
                Date occurred = new Date();
                String adjustmentKey = requestNo + ":" + row.getId();
                JkRetailRefundAdjustment adjustment = new JkRetailRefundAdjustment()
                        .setAdjustmentNo("RRA" + IdWorker.getIdStr()).setRequestNo(requestNo)
                        .setOrderId(row.getOrderId()).setOrderNo(row.getOrderNo()).setOrderInfoId(row.getOrderInfoId())
                        .setAttributionId(row.getId()).setBuyerUserId(row.getBuyerUserId())
                        .setReceiverUserId(row.getReceiverUserId()).setCountyAgentUserId(row.getCountyAgentUserId())
                        .setRegionCode(row.getFinalRegionCode() == null ? row.getRegionCode() : row.getFinalRegionCode())
                        .setAdjustmentAmount(part).setOccurredTime(occurred).setOriginalBusinessTime(row.getCreateTime())
                        .setIdempotencyKey(adjustmentKey).setIsDeleted(false).setCreateTime(occurred).setUpdateTime(occurred);
                refundAdjustmentDao.insert(adjustment);
                result.add(new RefundAllocation().setAttribution(row).setBeforeRefundedAmount(beforeRefunded).setRefundBaseAmount(part));
            }
        }
        return result;
    }

    private AttributionReceiver resolveReceiver(StoreOrder order) {
        Long buyerUserId = order.getUid().longValue();
        User buyer = userService.getById(order.getUid());
        String profileRegionCode = buyer == null ? null : trim(buyer.getJkRegionCode());
        String shippingRegionCode = trim(order.getJkShippingRegionCode());

        JkAgentRelation relation = relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>()
                .eq(JkAgentRelation::getUserId, buyerUserId).eq(JkAgentRelation::getStatus, true)
                .eq(JkAgentRelation::getIsDeleted, false).isNotNull(JkAgentRelation::getParentUserId)
                .orderByDesc(JkAgentRelation::getId).last("limit 1"));
        if (relation != null) {
            JkUserContext parent = contextService.getFrontContext(relation.getParentUserId());
            if (isCommissionReceiver(parent)) {
                String finalRegion = trim(parent.getRegionCode());
                return new AttributionReceiver(TYPE_DIRECT_PARENT, SOURCE_RELATION,
                        relation.getParentUserId(), parent.getPrimaryRoleCode(),
                        JkBizConstants.ROLE_COUNTY_AGENT.equals(parent.getPrimaryRoleCode())
                                ? relation.getParentUserId() : parent.getBelongCountyAgentId(),
                        finalRegion, regionName(finalRegion), profileRegionCode, shippingRegionCode,
                        relation.getId(), relation.getRelationType(), relation.getParentUserId(), parent.getPrimaryRoleCode(),
                        "采用下单时有效直属关系快照");
            }
        }

        AttributionReceiver byProfile = resolveByRegion(SOURCE_PROFILE, profileRegionCode, profileRegionCode, shippingRegionCode);
        if (byProfile != null) return byProfile;

        AttributionReceiver byShipping = resolveByRegion(SOURCE_SHIPPING, shippingRegionCode, profileRegionCode, shippingRegionCode);
        if (byShipping != null) return byShipping;

        return new AttributionReceiver(TYPE_PLATFORM, SOURCE_PLATFORM, null, null, null,
                null, null, profileRegionCode, shippingRegionCode, null, "PLATFORM_DEFAULT", null, null,
                "直属关系、个人资料区域和本单标准收货区域均无法解析，订单继续归平台默认");
    }

    private AttributionReceiver resolveByRegion(String source, String regionCode, String profileRegionCode, String shippingRegionCode) {
        String validCode = validRegionCode(regionCode);
        if (validCode == null) return null;
        JkRegionAgent regionAgent = regionAgentDao.selectOne(new LambdaQueryWrapper<JkRegionAgent>()
                .eq(JkRegionAgent::getRegionCode, validCode).eq(JkRegionAgent::getStatus, true)
                .eq(JkRegionAgent::getIsDeleted, false).isNotNull(JkRegionAgent::getCountyAgentUserId)
                .orderByDesc(JkRegionAgent::getId).last("limit 1"));
        if (regionAgent != null && isEffectiveBind(regionAgent.getBindStatus())) {
            JkUserContext county = contextService.getFrontContext(regionAgent.getCountyAgentUserId());
            if (isCommissionReceiver(county) && JkBizConstants.ROLE_COUNTY_AGENT.equals(county.getPrimaryRoleCode())) {
                return new AttributionReceiver(TYPE_REGION_AGENT, source, null, null, regionAgent.getCountyAgentUserId(),
                        validCode, regionName(validCode), profileRegionCode, shippingRegionCode, null, "REGION_AGENT",
                        regionAgent.getCountyAgentUserId(), county.getPrimaryRoleCode(),
                        SOURCE_PROFILE.equals(source) ? "无有效直属关系，采用个人资料标准区域匹配区县代理"
                                : "无有效直属关系和个人资料区域，采用本单标准收货区域兜底匹配区县代理");
            }
        }
        return new AttributionReceiver(TYPE_REGION_ONLY, source, null, null, null,
                validCode, regionName(validCode), profileRegionCode, shippingRegionCode, null, source,
                null, null, SOURCE_PROFILE.equals(source) ? "个人资料区域已确定，但当前未配置有效区县代理"
                        : "本单收货区域已确定，但当前未配置有效区县代理");
    }

    private String validRegionCode(String regionCode) {
        String code = trim(regionCode);
        if (code == null) return null;
        try {
            JkRegionPathResponse path = regionService.getRegionPath(code);
            return path == null || path.getCurrent() == null ? null : path.getCurrent().getRegionCode();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String regionName(String regionCode) {
        if (regionCode == null) return null;
        try {
            JkRegionPathResponse path = regionService.getRegionPath(regionCode);
            return path == null ? null : path.getFullPathName();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isCommissionReceiver(JkUserContext context) {
        if (context == null || Boolean.TRUE.equals(context.getFreezeStatus()) || context.getPrimaryRoleCode() == null) return false;
        String role = context.getPrimaryRoleCode();
        return JkBizConstants.ROLE_MAKER.equals(role) || JkBizConstants.ROLE_PARTNER.equals(role)
                || JkBizConstants.ROLE_COUNTY_AGENT.equals(role);
    }

    private boolean isEffectiveBind(String bindStatus) {
        if (bindStatus == null) return true;
        return !("UNBOUND".equalsIgnoreCase(bindStatus) || "DISABLED".equalsIgnoreCase(bindStatus)
                || "INVALID".equalsIgnoreCase(bindStatus) || "EXPIRED".equalsIgnoreCase(bindStatus));
    }

    private String trim(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private BigDecimal safe(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private static class AttributionReceiver {
        private final String attributionType;
        private final String regionSourceType;
        private final Long directParentUserId;
        private final String directParentRoleCode;
        private final Long countyAgentUserId;
        private final String finalRegionCode;
        private final String finalRegionName;
        private final String profileRegionCode;
        private final String shippingRegionCode;
        private final Long relationId;
        private final String relationSource;
        private final Long receiverUserId;
        private final String receiverRoleCode;
        private final String reason;

        private AttributionReceiver(String attributionType, String regionSourceType,
                                    Long directParentUserId, String directParentRoleCode, Long countyAgentUserId,
                                    String finalRegionCode, String finalRegionName,
                                    String profileRegionCode, String shippingRegionCode,
                                    Long relationId, String relationSource, Long receiverUserId,
                                    String receiverRoleCode, String reason) {
            this.attributionType = attributionType;
            this.regionSourceType = regionSourceType;
            this.directParentUserId = directParentUserId;
            this.directParentRoleCode = directParentRoleCode;
            this.countyAgentUserId = countyAgentUserId;
            this.finalRegionCode = finalRegionCode;
            this.finalRegionName = finalRegionName;
            this.profileRegionCode = profileRegionCode;
            this.shippingRegionCode = shippingRegionCode;
            this.relationId = relationId;
            this.relationSource = relationSource;
            this.receiverUserId = receiverUserId;
            this.receiverRoleCode = receiverRoleCode;
            this.reason = reason;
        }
    }
}
