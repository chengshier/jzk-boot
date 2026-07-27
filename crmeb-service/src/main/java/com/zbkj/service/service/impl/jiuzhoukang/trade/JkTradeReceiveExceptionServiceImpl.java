package com.zbkj.service.service.impl.jiuzhoukang.trade;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrderItem;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferItem;
import com.zbkj.common.model.jiuzhoukang.JkTradeReceiveException;
import com.zbkj.common.model.jiuzhoukang.JkTradeReceiveExceptionItem;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeReceiveExceptionCreateRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeReceiveExceptionHandleRequest;
import com.zbkj.common.response.jiuzhoukang.JkTradeReceiveExceptionDetailResponse;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkTradeReceiveExceptionDao;
import com.zbkj.service.dao.jiuzhoukang.JkTradeReceiveExceptionItemDao;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.trade.JkTradeReceiveExceptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 异常收货 V1。
 *
 * <p>异常上报后将业务单切换为 RECEIVE_EXCEPTION，禁止继续正常入库。后台完成补发、核对或线下处理后，
 * 将业务单恢复为原待收货状态，再由收货人重新确认正常收货。V1 不直接按实收数量入库，避免在金额、
 * 业绩、索赔和补发规则尚未完整建模时产生不可逆的库存差错。</p>
 */
@Service
public class JkTradeReceiveExceptionServiceImpl implements JkTradeReceiveExceptionService {
    private static final String TYPE_PLATFORM_ORDER = "PLATFORM_ORDER";
    private static final String TYPE_STOCK_TRANSFER = "STOCK_TRANSFER";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_REJECTED = "REJECTED";

    @Autowired private JkTradeReceiveExceptionDao exceptionDao;
    @Autowired private JkTradeReceiveExceptionItemDao exceptionItemDao;
    @Autowired private JkPlatformOrderDao platformOrderDao;
    @Autowired private JkPlatformOrderItemDao platformOrderItemDao;
    @Autowired private JkStockTransferDao stockTransferDao;
    @Autowired private JkStockTransferItemDao stockTransferItemDao;
    @Autowired private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkTradeReceiveExceptionDetailResponse create(Long userId, JkTradeReceiveExceptionCreateRequest request) {
        String businessType = normalizeBusinessType(request.getBusinessType());
        JkTradeReceiveException old = exceptionDao.selectOne(new LambdaQueryWrapper<JkTradeReceiveException>()
                .eq(JkTradeReceiveException::getRequestNo, request.getRequestNo()).last("limit 1"));
        if (old != null) {
            if (!userId.equals(old.getReceiverUserId())) throw new CrmebException("requestNo 已被其他用户使用");
            return buildDetail(enrich(old));
        }

        BusinessSnapshot business = loadBusiness(businessType, request.getBusinessId(), userId, true);
        Integer openCount = exceptionDao.selectCount(new LambdaQueryWrapper<JkTradeReceiveException>()
                .eq(JkTradeReceiveException::getBusinessType, businessType)
                .eq(JkTradeReceiveException::getBusinessId, request.getBusinessId())
                .in(JkTradeReceiveException::getStatus, Arrays.asList(STATUS_PENDING, STATUS_PROCESSING))
                .eq(JkTradeReceiveException::getIsDeleted, false));
        if (openCount != null && openCount > 0) throw new CrmebException("该业务单已有处理中收货异常，请勿重复上报");

        Map<Long, JkTradeReceiveExceptionCreateRequest.Item> requestItems = new HashMap<>();
        for (JkTradeReceiveExceptionCreateRequest.Item item : request.getItems()) {
            if (requestItems.put(item.getBusinessItemId(), item) != null) throw new CrmebException("收货商品明细不能重复");
        }
        if (requestItems.size() != business.items.size()) throw new CrmebException("请核对并提交全部商品的实收数量");

        int expectedTotal = 0;
        int receivedTotal = 0;
        int shortageTotal = 0;
        int damagedTotal = 0;
        List<JkTradeReceiveExceptionItem> itemEntities = new ArrayList<>();
        for (ExpectedItem expected : business.items) {
            JkTradeReceiveExceptionCreateRequest.Item actual = requestItems.get(expected.id);
            if (actual == null) throw new CrmebException("缺少商品明细：" + expected.productName);
            int received = safe(actual.getReceivedQty());
            int damaged = safe(actual.getDamagedQty());
            if (received > expected.quantity) throw new CrmebException(expected.productName + "的实收数量不能大于应收数量");
            if (damaged > received) throw new CrmebException(expected.productName + "的破损数量不能大于实收数量");
            int shortage = expected.quantity - received;
            expectedTotal += expected.quantity;
            receivedTotal += received;
            shortageTotal += shortage;
            damagedTotal += damaged;
            itemEntities.add(new JkTradeReceiveExceptionItem()
                    .setBusinessItemId(expected.id).setProductId(expected.productId).setSkuId(expected.skuId)
                    .setProductName(expected.productName).setSkuName(expected.skuName).setSkuCode(expected.skuCode)
                    .setExpectedQty(expected.quantity).setReceivedQty(received).setDamagedQty(damaged)
                    .setShortageQty(shortage).setItemRemark(actual.getItemRemark())
                    .setIsDeleted(false).setVersion(0));
        }
        if (shortageTotal <= 0 && damagedTotal <= 0 && "OTHER".equalsIgnoreCase(request.getExceptionType())) {
            if (StrUtil.isBlank(request.getExceptionReason())) throw new CrmebException("请填写异常原因");
        } else if (shortageTotal <= 0 && damagedTotal <= 0) {
            throw new CrmebException("实际收货数量无差异，请使用正常收货");
        }

        markBusinessException(businessType, request.getBusinessId(), business.expectedStatus, userId);
        Date now = new Date();
        JkTradeReceiveException entity = new JkTradeReceiveException()
                .setExceptionNo("RE" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr())
                .setRequestNo(request.getRequestNo()).setBusinessType(businessType)
                .setBusinessId(request.getBusinessId()).setBusinessNo(business.businessNo)
                .setReceiverUserId(userId).setStatus(STATUS_PENDING)
                .setExceptionType(normalizeExceptionType(request.getExceptionType()))
                .setExpectedTotalQty(expectedTotal).setReceivedTotalQty(receivedTotal)
                .setShortageTotalQty(shortageTotal).setDamagedTotalQty(damagedTotal)
                .setExceptionReason(request.getExceptionReason())
                .setEvidenceJson(JSONUtil.toJsonStr(request.getEvidenceUrls() == null ? Collections.emptyList() : request.getEvidenceUrls()))
                .setIsDeleted(false).setCreateUserId(userId).setUpdateUserId(userId)
                .setCreateTime(now).setUpdateTime(now).setVersion(0);
        exceptionDao.insert(entity);
        for (JkTradeReceiveExceptionItem item : itemEntities) {
            item.setExceptionId(entity.getId()).setCreateTime(now).setUpdateTime(now);
            exceptionItemDao.insert(item);
        }
        return buildDetail(enrich(entity));
    }

    @Override
    public PageInfo<JkTradeReceiveException> listMine(Long userId, String status, PageParamRequest pageParam) {
        Page<JkTradeReceiveException> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkTradeReceiveException> query = new LambdaQueryWrapper<JkTradeReceiveException>()
                .eq(JkTradeReceiveException::getReceiverUserId, userId)
                .eq(JkTradeReceiveException::getIsDeleted, false)
                .orderByDesc(JkTradeReceiveException::getId);
        if (StrUtil.isNotBlank(status)) query.eq(JkTradeReceiveException::getStatus, status.trim().toUpperCase());
        List<JkTradeReceiveException> rows = exceptionDao.selectList(query);
        rows.forEach(this::enrich);
        return CommonPage.copyPageInfo(page, rows);
    }

    @Override
    public JkTradeReceiveExceptionDetailResponse detailMine(Long userId, Long id) {
        JkTradeReceiveException entity = require(id);
        if (!userId.equals(entity.getReceiverUserId())) throw new CrmebException("无权查看该收货异常");
        return buildDetail(enrich(entity));
    }

    @Override
    public JkTradeReceiveExceptionDetailResponse detailByBusiness(Long userId, String businessType, Long businessId) {
        JkTradeReceiveException entity = exceptionDao.selectOne(new LambdaQueryWrapper<JkTradeReceiveException>()
                .eq(JkTradeReceiveException::getBusinessType, normalizeBusinessType(businessType))
                .eq(JkTradeReceiveException::getBusinessId, businessId)
                .eq(JkTradeReceiveException::getReceiverUserId, userId)
                .eq(JkTradeReceiveException::getIsDeleted, false)
                .orderByDesc(JkTradeReceiveException::getId).last("limit 1"));
        if (entity == null) throw new CrmebException("未找到该业务单的收货异常记录");
        return buildDetail(enrich(entity));
    }

    @Override
    public PageInfo<JkTradeReceiveException> listAdmin(String status, String businessType, Long receiverUserId, PageParamRequest pageParam) {
        Page<JkTradeReceiveException> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkTradeReceiveException> query = new LambdaQueryWrapper<JkTradeReceiveException>()
                .eq(JkTradeReceiveException::getIsDeleted, false).orderByDesc(JkTradeReceiveException::getId);
        if (StrUtil.isNotBlank(status)) query.eq(JkTradeReceiveException::getStatus, status.trim().toUpperCase());
        if (StrUtil.isNotBlank(businessType)) query.eq(JkTradeReceiveException::getBusinessType, normalizeBusinessType(businessType));
        if (receiverUserId != null) query.eq(JkTradeReceiveException::getReceiverUserId, receiverUserId);
        List<JkTradeReceiveException> rows = exceptionDao.selectList(query);
        rows.forEach(this::enrich);
        return CommonPage.copyPageInfo(page, rows);
    }

    @Override
    public JkTradeReceiveExceptionDetailResponse detailAdmin(Long id) {
        return buildDetail(enrich(require(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkTradeReceiveExceptionDetailResponse handle(Long operatorId, JkTradeReceiveExceptionHandleRequest request) {
        JkTradeReceiveException entity = require(request.getExceptionId());
        if (STATUS_RESOLVED.equals(entity.getStatus()) || STATUS_REJECTED.equals(entity.getStatus())) return buildDetail(enrich(entity));
        String action = request.getAction().trim().toUpperCase();
        if (!Arrays.asList(STATUS_PROCESSING, STATUS_RESOLVED, STATUS_REJECTED).contains(action)) {
            throw new CrmebException("不支持的异常处理动作");
        }
        Date now = new Date();
        entity.setStatus(action).setHandleAction(action).setHandleRemark(request.getRemark())
                .setHandleUserId(operatorId).setHandleTime(now).setUpdateUserId(operatorId).setUpdateTime(now);
        exceptionDao.updateById(entity);
        if (STATUS_RESOLVED.equals(action) || STATUS_REJECTED.equals(action)) restoreBusiness(entity, operatorId);
        return buildDetail(enrich(entity));
    }

    private void markBusinessException(String businessType, Long businessId, String expectedStatus, Long userId) {
        int updated;
        if (TYPE_PLATFORM_ORDER.equals(businessType)) {
            updated = platformOrderDao.update(null, new UpdateWrapper<JkPlatformOrder>()
                    .eq("id", businessId).eq("user_id", userId).eq("status", expectedStatus).eq("is_deleted", false)
                    .set("status", "RECEIVE_EXCEPTION").set("receive_status", "EXCEPTION")
                    .set("update_user_id", userId).set("update_time", new Date()));
        } else {
            updated = stockTransferDao.update(null, new UpdateWrapper<JkStockTransfer>()
                    .eq("id", businessId).eq("user_id", userId).eq("status", expectedStatus).eq("is_deleted", false)
                    .set("status", "RECEIVE_EXCEPTION").set("receive_status", "EXCEPTION")
                    .set("update_user_id", userId).set("update_time", new Date()));
        }
        if (updated != 1) throw new CrmebException("业务单状态已变化，请刷新后重试");
    }

    private void restoreBusiness(JkTradeReceiveException entity, Long operatorId) {
        String originalStatus = TYPE_PLATFORM_ORDER.equals(entity.getBusinessType()) ? "SHIPPED" : "TRANSFERRED";
        int updated;
        if (TYPE_PLATFORM_ORDER.equals(entity.getBusinessType())) {
            updated = platformOrderDao.update(null, new UpdateWrapper<JkPlatformOrder>()
                    .eq("id", entity.getBusinessId()).eq("status", "RECEIVE_EXCEPTION").eq("is_deleted", false)
                    .set("status", originalStatus).set("receive_status", "UNRECEIVED")
                    .set("update_user_id", operatorId).set("update_time", new Date()));
        } else {
            updated = stockTransferDao.update(null, new UpdateWrapper<JkStockTransfer>()
                    .eq("id", entity.getBusinessId()).eq("status", "RECEIVE_EXCEPTION").eq("is_deleted", false)
                    .set("status", originalStatus).set("receive_status", "UNRECEIVED")
                    .set("update_user_id", operatorId).set("update_time", new Date()));
        }
        if (updated != 1) {
            throw new CrmebException("原业务单状态已变化，异常处理未生效，请刷新后核对");
        }
    }

    private BusinessSnapshot loadBusiness(String businessType, Long businessId, Long userId, boolean requireReceivable) {
        BusinessSnapshot result = new BusinessSnapshot();
        result.items = new ArrayList<>();
        if (TYPE_PLATFORM_ORDER.equals(businessType)) {
            JkPlatformOrder order = platformOrderDao.selectById(businessId);
            if (order == null || Boolean.TRUE.equals(order.getIsDeleted())) throw new CrmebException("订货单不存在");
            if (!userId.equals(order.getUserId())) throw new CrmebException("无权操作该订货单");
            if (requireReceivable && !"SHIPPED".equals(order.getStatus())) throw new CrmebException("当前订货单不能上报收货异常");
            result.businessNo = order.getPlatformOrderNo();
            result.expectedStatus = "SHIPPED";
            for (JkPlatformOrderItem item : platformOrderItemDao.selectList(new LambdaQueryWrapper<JkPlatformOrderItem>()
                    .eq(JkPlatformOrderItem::getPlatformOrderId, businessId).eq(JkPlatformOrderItem::getIsDeleted, false))) {
                result.items.add(ExpectedItem.of(item.getId(), item.getProductId(), item.getSkuId(), item.getProductName(), item.getSkuName(), item.getSkuCode(), item.getQuantity()));
            }
        } else {
            JkStockTransfer transfer = stockTransferDao.selectById(businessId);
            if (transfer == null || Boolean.TRUE.equals(transfer.getIsDeleted())) throw new CrmebException("调拨单不存在");
            if (!userId.equals(transfer.getUserId())) throw new CrmebException("无权操作该调拨单");
            if (requireReceivable && !"TRANSFERRED".equals(transfer.getStatus())) throw new CrmebException("当前调拨单不能上报收货异常");
            result.businessNo = transfer.getTransferNo();
            result.expectedStatus = "TRANSFERRED";
            for (JkStockTransferItem item : stockTransferItemDao.selectList(new LambdaQueryWrapper<JkStockTransferItem>()
                    .eq(JkStockTransferItem::getTransferId, businessId).eq(JkStockTransferItem::getIsDeleted, false))) {
                result.items.add(ExpectedItem.of(item.getId(), item.getProductId(), item.getSkuId(), item.getProductName(), item.getSkuName(), item.getSkuCode(), item.getQuantity()));
            }
        }
        if (result.items.isEmpty()) throw new CrmebException("业务单没有可核对的商品明细");
        return result;
    }

    private JkTradeReceiveException require(Long id) {
        JkTradeReceiveException entity = exceptionDao.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) throw new CrmebException("收货异常记录不存在");
        return entity;
    }

    private JkTradeReceiveExceptionDetailResponse buildDetail(JkTradeReceiveException entity) {
        List<JkTradeReceiveExceptionItem> items = exceptionItemDao.selectList(new LambdaQueryWrapper<JkTradeReceiveExceptionItem>()
                .eq(JkTradeReceiveExceptionItem::getExceptionId, entity.getId())
                .eq(JkTradeReceiveExceptionItem::getIsDeleted, false).orderByAsc(JkTradeReceiveExceptionItem::getId));
        return new JkTradeReceiveExceptionDetailResponse().setException(entity).setItems(items);
    }

    private JkTradeReceiveException enrich(JkTradeReceiveException entity) {
        User user = entity.getReceiverUserId() == null ? null : userService.getById(entity.getReceiverUserId().intValue());
        if (user != null) {
            entity.setReceiverName(StrUtil.blankToDefault(user.getRealName(), user.getNickname()));
            entity.setReceiverPhone(user.getPhone());
        }
        entity.setBusinessTypeText(TYPE_PLATFORM_ORDER.equals(entity.getBusinessType()) ? "平台订货" : "库存调拨");
        entity.setExceptionTypeText(exceptionTypeText(entity.getExceptionType()));
        entity.setStatusText(statusText(entity.getStatus()));
        entity.setStatusTag(statusTag(entity.getStatus()));
        return entity;
    }

    private String normalizeBusinessType(String value) {
        String type = StrUtil.blankToDefault(value, "").trim().toUpperCase();
        if (!TYPE_PLATFORM_ORDER.equals(type) && !TYPE_STOCK_TRANSFER.equals(type)) throw new CrmebException("不支持的收货业务类型");
        return type;
    }

    private String normalizeExceptionType(String value) {
        String type = StrUtil.blankToDefault(value, "OTHER").trim().toUpperCase();
        Set<String> allowed = new HashSet<>(Arrays.asList("SHORTAGE", "DAMAGED", "MIXED", "OTHER"));
        if (!allowed.contains(type)) throw new CrmebException("不支持的收货异常类型");
        return type;
    }

    private String statusText(String status) {
        if (STATUS_PENDING.equals(status)) return "待处理";
        if (STATUS_PROCESSING.equals(status)) return "处理中";
        if (STATUS_RESOLVED.equals(status)) return "已处理，可重新收货";
        if (STATUS_REJECTED.equals(status)) return "已驳回，可重新核对";
        return StrUtil.blankToDefault(status, "状态未配置");
    }

    private String statusTag(String status) {
        if (STATUS_RESOLVED.equals(status)) return "success";
        if (STATUS_REJECTED.equals(status)) return "danger";
        if (STATUS_PROCESSING.equals(status)) return "info";
        return "warning";
    }

    private String exceptionTypeText(String type) {
        if ("SHORTAGE".equals(type)) return "数量短缺";
        if ("DAMAGED".equals(type)) return "商品破损";
        if ("MIXED".equals(type)) return "短缺并破损";
        return "其他异常";
    }

    private int safe(Integer value) { return value == null ? 0 : value; }

    private static class BusinessSnapshot {
        private String businessNo;
        private String expectedStatus;
        private List<ExpectedItem> items;
    }

    private static class ExpectedItem {
        private Long id;
        private Integer productId;
        private Integer skuId;
        private String productName;
        private String skuName;
        private String skuCode;
        private Integer quantity;

        private static ExpectedItem of(Long id, Integer productId, Integer skuId, String productName, String skuName, String skuCode, Integer quantity) {
            ExpectedItem item = new ExpectedItem();
            item.id = id;
            item.productId = productId;
            item.skuId = skuId;
            item.productName = StrUtil.blankToDefault(productName, "商品已删除");
            item.skuName = skuName;
            item.skuCode = skuCode;
            item.quantity = quantity == null ? 0 : quantity;
            return item;
        }
    }
}
