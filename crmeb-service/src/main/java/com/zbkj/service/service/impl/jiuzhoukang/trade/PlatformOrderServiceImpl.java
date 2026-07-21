package com.zbkj.service.service.impl.jiuzhoukang.trade;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkOfflinePaymentVoucher;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrderItem;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkBusinessActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkPaymentAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkPaymentVoucherRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeCreateRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeDocumentSearchRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeLineRequest;
import com.zbkj.common.response.jiuzhoukang.JkAuditLogResponse;
import com.zbkj.common.response.jiuzhoukang.JkPlatformOrderDetailResponse;
import com.zbkj.common.response.jiuzhoukang.JkProductTradeViewResponse;
import com.zbkj.service.dao.jiuzhoukang.JkOfflinePaymentVoucherDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.exception.jiuzhoukang.TradeAuditRejectedException;
import com.zbkj.service.service.StoreProductAttrValueService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.impl.jiuzhoukang.audit.JkAuditLogServiceImpl;
import com.zbkj.service.service.jiuzhoukang.audit.JkAuditLogService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.price.PriceCalculateService;
import com.zbkj.service.service.jiuzhoukang.stock.StockFlowService;
import com.zbkj.service.service.jiuzhoukang.support.JkDisplayEnrichmentSupport;
import com.zbkj.service.service.jiuzhoukang.trade.JkTradeStatusSupport;
import com.zbkj.service.service.jiuzhoukang.trade.PlatformOrderService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class PlatformOrderServiceImpl implements PlatformOrderService {
    private static final String BUSINESS_TYPE = "PLATFORM_ORDER";

    @Autowired
    private JkPlatformOrderDao orderDao;
    @Autowired
    private JkPlatformOrderItemDao itemDao;
    @Autowired
    private JkOfflinePaymentVoucherDao voucherDao;
    @Autowired
    private JkStockAccountDao stockAccountDao;
    @Autowired
    private StoreProductService productService;
    @Autowired
    private StoreProductAttrValueService skuService;
    @Autowired
    private PriceCalculateService priceCalculateService;
    @Autowired
    private JkUserContextService userContextService;
    @Autowired
    private StockFlowService stockFlowService;
    @Autowired
    private JkAuditLogService auditLogService;
    @Autowired
    private JkDisplayEnrichmentSupport displayEnrichmentSupport;
    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPlatformOrder create(Long userId, JkTradeCreateRequest request) {
        JkPlatformOrder exists = orderDao.selectOne(new LambdaQueryWrapper<JkPlatformOrder>()
                .eq(JkPlatformOrder::getRequestNo, request.getRequestNo())
                .last("limit 1"));
        if (exists != null) {
            if (!userId.equals(exists.getUserId())) {
                throw new CrmebException("requestNo 已存在");
            }
            return exists;
        }
        JkUserContext context = userContextService.getFrontContext(userId);
        assertCountyAgent(context);
        JkStockAccount platform = findAccount(JkBizConstants.STOCK_ACCOUNT_PLATFORM, null);
        JkStockAccount county = findAccount(JkBizConstants.STOCK_ACCOUNT_COUNTY_AGENT, userId);
        JkPlatformOrder order = new JkPlatformOrder()
                .setPlatformOrderNo("PO" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr())
                .setRequestNo(request.getRequestNo())
                .setUserId(userId)
                .setRoleCode(JkBizConstants.ROLE_COUNTY_AGENT)
                .setCountyAgentId(userId)
                .setRegionCode(context.getRegionCode())
                .setStatus("CREATED")
                .setPayStatus("UNPAID")
                .setAuditStatus("NONE")
                .setLogisticsStatus("UNSHIPPED")
                .setReceiveStatus("UNRECEIVED")
                .setTotalAmount(BigDecimal.ZERO)
                .setIsDeleted(false)
                .setCreateUserId(userId)
                .setUpdateUserId(userId)
                .setVersion(0);
        orderDao.insert(order);
        BigDecimal total = BigDecimal.ZERO;
        for (JkTradeLineRequest line : request.getItems()) {
            StoreProduct product = productService.getById(line.getProductId());
            if (product == null) {
                throw new CrmebException("商品不存在");
            }
            StoreProductAttrValue sku = line.getSkuId() == null ? null : skuService.getById(line.getSkuId());
            if (sku != null && !line.getProductId().equals(sku.getProductId())) {
                throw new CrmebException("商品规格不匹配");
            }
            JkProductTradeViewResponse.PriceInfo price = priceCalculateService.calculatePrice(product, sku, context);
            if (price == null || price.getAmount() == null) {
                throw new CrmebException("价格规则失效");
            }
            BigDecimal amount = price.getAmount().multiply(BigDecimal.valueOf(line.getQuantity()));
            itemDao.insert(new JkPlatformOrderItem()
                    .setPlatformOrderId(order.getId())
                    .setProductId(product.getId())
                    .setSkuId(sku == null ? null : sku.getId())
                    .setProductName(product.getStoreName())
                    .setSkuName(sku == null ? null : sku.getSuk())
                    .setSkuCode(sku == null ? null : sku.getUnique())
                    .setQuantity(line.getQuantity())
                    .setUnitPrice(price.getAmount())
                    .setTotalAmount(amount)
                    .setPriceRuleId(price.getRuleId())
                    .setPriceRuleVersion(price.getRuleVersion())
                    .setPriceType(price.getPriceType())
                    .setPriceSnapshotJson(buildPriceSnapshot(price))
                    .setFromStockAccountId(platform.getId())
                    .setToStockAccountId(county.getId())
                    .setIsDeleted(false)
                    .setVersion(0));
            total = total.add(amount);
        }
        order.setTotalAmount(total);
        orderDao.updateById(order);
        return enrichDisplay(order);
    }

    @Override
    public PageInfo<JkPlatformOrder> getFrontList(Long userId, JkTradeDocumentSearchRequest request, PageParamRequest pageParamRequest) {
        Page<JkPlatformOrder> page = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<JkPlatformOrder> lqw = baseListWrapper(request);
        lqw.eq(JkPlatformOrder::getUserId, userId);
        List<JkPlatformOrder> list = orderDao.selectList(lqw);
        displayEnrichmentSupport.enrichPlatformOrders(list);
        return CommonPage.copyPageInfo(page, list);
    }

    @Override
    public PageInfo<JkPlatformOrder> getAdminList(JkTradeDocumentSearchRequest request, PageParamRequest pageParamRequest) {
        Page<JkPlatformOrder> page = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        List<JkPlatformOrder> list = orderDao.selectList(baseListWrapper(request));
        displayEnrichmentSupport.enrichPlatformOrders(list);
        return CommonPage.copyPageInfo(page, list);
    }

    @Override
    public JkPlatformOrderDetailResponse getFrontDetail(Long userId, Long orderId) {
        JkPlatformOrder order = requireOrder(orderId);
        if (!userId.equals(order.getUserId())) {
            throw new CrmebException("无权查看该订货单");
        }
        return buildDetail(order);
    }

    @Override
    public JkPlatformOrderDetailResponse getAdminDetail(Long orderId) {
        return buildDetail(requireOrder(orderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPlatformOrder submitVoucher(Long userId, Long orderId, JkPaymentVoucherRequest request) {
        JkPlatformOrder order = requireOrder(orderId);
        if (!userId.equals(order.getUserId())) {
            throw new CrmebException("无权操作该订货单");
        }
        if (!("CREATED".equals(order.getStatus()) || "PAYMENT_REJECTED".equals(order.getStatus()))) {
            throw new CrmebException("当前状态不能提交付款凭证");
        }
        JkOfflinePaymentVoucher currentVoucher = findCurrentVoucher(orderId);
        if ("CREATED".equals(order.getStatus())
                && currentVoucher != null
                && !"REJECTED".equals(currentVoucher.getAuditStatus())) {
            throw new CrmebException("付款凭证已提交，请勿重复上传");
        }
        if (currentVoucher != null) {
            deactivateCurrentVoucher(orderId);
        }
        voucherDao.insert(new JkOfflinePaymentVoucher()
                .setVoucherNo("PV" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr())
                .setBusinessType(BUSINESS_TYPE)
                .setBusinessId(orderId)
                .setVoucherUrl(request.getVoucherUrl())
                .setSubmitUserId(userId)
                .setAuditStatus("PENDING")
                .setIsCurrent(true)
                .setIsDeleted(false)
                .setVersion(0));
        String beforeStatus = order.getStatus();
        order.setStatus("PAYMENT_SUBMITTED")
                .setPayStatus("PAYMENT_SUBMITTED")
                .setAuditStatus("PENDING")
                .setUpdateUserId(userId)
                .setRejectReason(null);
        orderDao.updateById(order);
        log(order, userId, beforeStatus, "PAYMENT_SUBMITTED", "SUBMIT_VOUCHER", null, null, "FRONT");
        return enrichDisplay(order);
    }

    @Override
    public JkPlatformOrder auditPayment(Long adminUserId, JkPaymentAuditRequest request) {
        JkPlatformOrder order = requireOrder(request.getBusinessId());
        if (!"PAYMENT_SUBMITTED".equals(order.getStatus())) {
            throw new CrmebException("当前状态不能审核付款");
        }
        JkOfflinePaymentVoucher voucher = requireCurrentVoucher(order.getId());
        if (!Boolean.TRUE.equals(request.getApproved())) {
            return executeInNewTransaction(() -> reject(requireOrder(order.getId()), requireCurrentVoucher(order.getId()), adminUserId, request.getRemark()));
        }
        try {
            freezePlatformStock(order, adminUserId);
        } catch (CrmebException e) {
            if (!isInventoryShortage(e)) {
                throw e;
            }
            String reason = JkTradeStatusSupport.inventoryRejectReason(request.getRemark());
            return rejectAfterInventoryShortage(order, voucher, adminUserId, reason);
        }
        return executeInNewTransaction(() -> approvePayment(requireOrder(order.getId()), requireCurrentVoucher(order.getId()), adminUserId, request.getRemark()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPlatformOrder ship(Long adminUserId, JkBusinessActionRequest request) {
        JkPlatformOrder order = requireOrder(request.getBusinessId());
        if (!"PAYMENT_APPROVED".equals(order.getStatus())) {
            throw new CrmebException("当前状态不能发货");
        }
        JkStockAccount platform = findAccount(JkBizConstants.STOCK_ACCOUNT_PLATFORM, null);
        for (JkPlatformOrderItem item : listItems(order.getId())) {
            stockFlowService.outboundFrozenStock(buildAction(order, platform.getId(), item, adminUserId, "平台订货发货出库"));
        }
        order.setStatus("SHIPPED")
                .setLogisticsStatus("SHIPPED")
                .setUpdateUserId(adminUserId);
        orderDao.updateById(order);
        log(order, adminUserId, "PAYMENT_APPROVED", "SHIPPED", "SHIP", request.getRemark(), null, "ADMIN");
        return enrichDisplay(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPlatformOrder receive(Long userId, JkBusinessActionRequest request) {
        JkPlatformOrder order = requireOrder(request.getBusinessId());
        if (!userId.equals(order.getUserId())) {
            throw new CrmebException("无权确认该订货单收货");
        }
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new CrmebException("当前状态不能确认收货");
        }
        JkStockAccount county = findAccount(JkBizConstants.STOCK_ACCOUNT_COUNTY_AGENT, userId);
        for (JkPlatformOrderItem item : listItems(order.getId())) {
            stockFlowService.inboundStock(buildAction(order, county.getId(), item, userId, "区县代订货确认收货入库"));
        }
        order.setStatus("STOCK_IN")
                .setReceiveStatus("STOCK_IN")
                .setUpdateUserId(userId);
        orderDao.updateById(order);
        log(order, userId, "SHIPPED", "STOCK_IN", "RECEIVE", request.getRemark(), null, "FRONT");
        return enrichDisplay(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPlatformOrder close(Long adminUserId, JkBusinessActionRequest request) {
        JkPlatformOrder order = requireOrder(request.getBusinessId());
        String beforeStatus = order.getStatus();
        if (!("CREATED".equals(beforeStatus)
                || "PAYMENT_SUBMITTED".equals(beforeStatus)
                || "PAYMENT_REJECTED".equals(beforeStatus)
                || "PAYMENT_APPROVED".equals(beforeStatus))) {
            throw new CrmebException("当前状态不能关闭");
        }
        if (JkTradeStatusSupport.platformOrderRequiresFrozenRelease(beforeStatus)) {
            releasePlatformFrozenStock(order, adminUserId, "平台订货关闭释放冻结库存");
        }
        rejectCurrentVoucher(order.getId(), adminUserId, request.getRemark());
        order.setStatus("CLOSED")
                .setCancelReason(request.getRemark())
                .setUpdateUserId(adminUserId);
        orderDao.updateById(order);
        log(order, adminUserId, beforeStatus, "CLOSED", "CLOSE", request.getRemark(), null, "ADMIN");
        return enrichDisplay(order);
    }

    private JkPlatformOrder reject(JkPlatformOrder order, JkOfflinePaymentVoucher voucher, Long adminUserId, String reason) {
        voucher.setAuditStatus("REJECTED")
                .setAuditUserId(adminUserId)
                .setAuditTime(new Date())
                .setRejectReason(reason);
        voucherDao.updateById(voucher);
        order.setStatus("PAYMENT_REJECTED")
                .setPayStatus("REJECTED")
                .setAuditStatus("REJECTED")
                .setAuditUserId(adminUserId)
                .setAuditTime(new Date())
                .setRejectReason(reason)
                .setAuditRemark(reason)
                .setUpdateUserId(adminUserId);
        orderDao.updateById(order);
        log(order, adminUserId, "PAYMENT_SUBMITTED", "PAYMENT_REJECTED", "REJECT", reason, reason, "ADMIN");
        return enrichDisplay(order);
    }

    private void freezePlatformStock(JkPlatformOrder order, Long adminUserId) {
        JkStockAccount platform = findAccount(JkBizConstants.STOCK_ACCOUNT_PLATFORM, null);
        for (JkPlatformOrderItem item : listItems(order.getId())) {
            stockFlowService.freezeStock(buildAction(order, platform.getId(), item, adminUserId, "平台订货付款审核通过冻结"));
        }
    }

    private void releasePlatformFrozenStock(JkPlatformOrder order, Long adminUserId, String remark) {
        JkStockAccount platform = findAccount(JkBizConstants.STOCK_ACCOUNT_PLATFORM, null);
        for (JkPlatformOrderItem item : listItems(order.getId())) {
            stockFlowService.releaseFrozenStock(buildAction(order, platform.getId(), item, adminUserId, remark));
        }
    }

    private JkPlatformOrder requireOrder(Long id) {
        JkPlatformOrder value = orderDao.selectById(id);
        if (value == null || Boolean.TRUE.equals(value.getIsDeleted())) {
            throw new CrmebException("订货单不存在");
        }
        return value;
    }

    private JkStockAccount findAccount(String type, Long ownerId) {
        LambdaQueryWrapper<JkStockAccount> lqw = new LambdaQueryWrapper<JkStockAccount>()
                .eq(JkStockAccount::getAccountType, type)
                .eq(JkStockAccount::getStatus, true)
                .eq(JkStockAccount::getIsDeleted, false)
                .last("limit 1");
        if (ownerId != null) {
            lqw.eq(JkStockAccount::getOwnerUserId, ownerId);
        }
        JkStockAccount value = stockAccountDao.selectOne(lqw);
        if (value == null) {
            throw new CrmebException("库存账户不存在");
        }
        return value;
    }

    private void assertCountyAgent(JkUserContext context) {
        if (context == null
                || Boolean.TRUE.equals(context.getFreezeStatus())
                || !JkBizConstants.ROLE_COUNTY_AGENT.equals(context.getPrimaryRoleCode())
                || !JkBizConstants.AUDIT_STATUS_EFFECTIVE.equals(context.getAuditStatus())) {
            throw new CrmebException("区县代身份无效或已冻结");
        }
    }

    private void deactivateCurrentVoucher(Long orderId) {
        voucherDao.update(null, new UpdateWrapper<JkOfflinePaymentVoucher>()
                .eq("business_type", BUSINESS_TYPE)
                .eq("business_id", orderId)
                .eq("is_current", true)
                .set("is_current", false));
    }

    private JkOfflinePaymentVoucher requireCurrentVoucher(Long orderId) {
        JkOfflinePaymentVoucher voucher = findCurrentVoucher(orderId);
        if (voucher == null) {
            throw new CrmebException("付款凭证不存在");
        }
        return voucher;
    }

    private JkOfflinePaymentVoucher findCurrentVoucher(Long orderId) {
        return voucherDao.selectOne(new LambdaQueryWrapper<JkOfflinePaymentVoucher>()
                .eq(JkOfflinePaymentVoucher::getBusinessType, BUSINESS_TYPE)
                .eq(JkOfflinePaymentVoucher::getBusinessId, orderId)
                .eq(JkOfflinePaymentVoucher::getIsCurrent, true)
                .eq(JkOfflinePaymentVoucher::getIsDeleted, false)
                .last("limit 1"));
    }

    private void rejectCurrentVoucher(Long orderId, Long adminUserId, String reason) {
        JkOfflinePaymentVoucher voucher = voucherDao.selectOne(new LambdaQueryWrapper<JkOfflinePaymentVoucher>()
                .eq(JkOfflinePaymentVoucher::getBusinessType, BUSINESS_TYPE)
                .eq(JkOfflinePaymentVoucher::getBusinessId, orderId)
                .eq(JkOfflinePaymentVoucher::getIsCurrent, true)
                .eq(JkOfflinePaymentVoucher::getIsDeleted, false)
                .last("limit 1"));
        if (voucher == null || "APPROVED".equals(voucher.getAuditStatus()) || "REJECTED".equals(voucher.getAuditStatus())) {
            return;
        }
        voucher.setAuditStatus("REJECTED")
                .setAuditUserId(adminUserId)
                .setAuditTime(new Date())
                .setRejectReason(reason);
        voucherDao.updateById(voucher);
    }

    private List<JkPlatformOrderItem> listItems(Long orderId) {
        return itemDao.selectList(new LambdaQueryWrapper<JkPlatformOrderItem>()
                .eq(JkPlatformOrderItem::getPlatformOrderId, orderId)
                .eq(JkPlatformOrderItem::getIsDeleted, false));
    }

    private List<JkOfflinePaymentVoucher> listVouchers(Long orderId) {
        return voucherDao.selectList(new LambdaQueryWrapper<JkOfflinePaymentVoucher>()
                .eq(JkOfflinePaymentVoucher::getBusinessType, BUSINESS_TYPE)
                .eq(JkOfflinePaymentVoucher::getBusinessId, orderId)
                .eq(JkOfflinePaymentVoucher::getIsDeleted, false)
                .orderByDesc(JkOfflinePaymentVoucher::getId));
    }

    private List<JkAuditLogResponse> listAuditLogs(Long orderId) {
        return auditLogService.toResponses(auditLogService.list(new LambdaQueryWrapper<JkAuditLog>()
                .eq(JkAuditLog::getBusinessType, BUSINESS_TYPE)
                .eq(JkAuditLog::getBusinessId, orderId)
                .eq(JkAuditLog::getIsDeleted, false)
                .orderByDesc(JkAuditLog::getId)));
    }

    private JkPlatformOrderDetailResponse buildDetail(JkPlatformOrder order) {
        JkPlatformOrderDetailResponse response = new JkPlatformOrderDetailResponse();
        displayEnrichmentSupport.enrichPlatformOrders(java.util.Collections.singletonList(order));
        List<JkOfflinePaymentVoucher> vouchers = listVouchers(order.getId());
        displayEnrichmentSupport.enrichOfflinePaymentVouchers(vouchers);
        response.setOrder(order);
        response.setItems(listItems(order.getId()));
        response.setVouchers(vouchers);
        response.setAuditLogs(listAuditLogs(order.getId()));
        return response;
    }

    private LambdaQueryWrapper<JkPlatformOrder> baseListWrapper(JkTradeDocumentSearchRequest request) {
        LambdaQueryWrapper<JkPlatformOrder> lqw = new LambdaQueryWrapper<JkPlatformOrder>()
                .eq(JkPlatformOrder::getIsDeleted, false)
                .orderByDesc(JkPlatformOrder::getId);
        if (request == null) {
            return lqw;
        }
        if (request.getStatus() != null && request.getStatus().trim().length() > 0) {
            lqw.eq(JkPlatformOrder::getStatus, request.getStatus().trim());
        }
        if (request.getPayStatus() != null && request.getPayStatus().trim().length() > 0) {
            lqw.eq(JkPlatformOrder::getPayStatus, request.getPayStatus().trim());
        }
        if (request.getAuditStatus() != null && request.getAuditStatus().trim().length() > 0) {
            lqw.eq(JkPlatformOrder::getAuditStatus, request.getAuditStatus().trim());
        }
        if (request.getKeywords() != null && request.getKeywords().trim().length() > 0) {
            String keywords = request.getKeywords().trim();
            lqw.and(wrapper -> wrapper.like(JkPlatformOrder::getPlatformOrderNo, keywords)
                    .or()
                    .like(JkPlatformOrder::getRequestNo, keywords));
        }
        return lqw;
    }

    private JkStockActionRequest buildAction(JkPlatformOrder order, Long stockAccountId, JkPlatformOrderItem item, Long operatorUserId, String remark) {
        return new JkStockActionRequest()
                .setBusinessType(BUSINESS_TYPE)
                .setBusinessId(order.getId())
                .setBusinessNo(order.getPlatformOrderNo())
                .setStockAccountId(stockAccountId)
                .setProductId(item.getProductId())
                .setSkuId(item.getSkuId())
                .setSkuCode(item.getSkuCode())
                .setQuantity(item.getQuantity())
                .setOperatorUserId(operatorUserId)
                .setRemark(remark);
    }

    private String buildPriceSnapshot(JkProductTradeViewResponse.PriceInfo price) {
        return "{\"amount\":\"" + price.getAmount()
                + "\",\"priceType\":\"" + price.getPriceType()
                + "\",\"ruleId\":\"" + price.getRuleId()
                + "\",\"ruleVersion\":\"" + price.getRuleVersion() + "\"}";
    }

    private JkPlatformOrder approvePayment(JkPlatformOrder order, JkOfflinePaymentVoucher voucher, Long adminUserId, String remark) {
        voucher.setAuditStatus("APPROVED")
                .setAuditUserId(adminUserId)
                .setAuditTime(new Date())
                .setRejectReason(null);
        voucherDao.updateById(voucher);
        order.setStatus("PAYMENT_APPROVED")
                .setPayStatus("APPROVED")
                .setAuditStatus("APPROVED")
                .setAuditUserId(adminUserId)
                .setAuditTime(new Date())
                .setAuditRemark(remark)
                .setRejectReason(null)
                .setUpdateUserId(adminUserId);
        orderDao.updateById(order);
        log(order, adminUserId, "PAYMENT_SUBMITTED", "PAYMENT_APPROVED", "PASS", remark, null, "ADMIN");
        return enrichDisplay(order);
    }

    private JkPlatformOrder executeInNewTransaction(java.util.concurrent.Callable<JkPlatformOrder> callable) {
        if (transactionManager == null) {
            try {
                return callable.call();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new CrmebException(e.getMessage());
            }
        }
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        JkPlatformOrder result = template.execute(status -> {
            try {
                return callable.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new CrmebException(e.getMessage());
            }
        });
        if (result == null) {
            throw new CrmebException("付款审核结果为空");
        }
        return result;
    }
    private JkPlatformOrder rejectAfterInventoryShortage(JkPlatformOrder order, JkOfflinePaymentVoucher voucher, Long adminUserId, String reason) {
        if (transactionManager == null) {
            return reject(order, voucher, adminUserId, reason);
        }
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        JkPlatformOrder result = template.execute(status -> {
            JkPlatformOrder latestOrder = requireOrder(order.getId());
            JkOfflinePaymentVoucher latestVoucher = requireCurrentVoucher(order.getId());
            return reject(latestOrder, latestVoucher, adminUserId, reason);
        });
        if (result == null) {
            throw new TradeAuditRejectedException(reason);
        }
        return result;
    }
    private boolean isInventoryShortage(CrmebException exception) {
        return exception.getMessage() != null && exception.getMessage().contains("库存不足");
    }

    private JkPlatformOrder enrichDisplay(JkPlatformOrder order) {
        if (order != null && displayEnrichmentSupport != null) {
            displayEnrichmentSupport.enrichPlatformOrders(java.util.Collections.singletonList(order));
        }
        return order;
    }

    private void log(JkPlatformOrder order, Long userId, String before, String after, String action, String remark, String rejectReason, String source) {
        auditLogService.saveAuditLog(new JkAuditLog()
                .setBusinessType(BUSINESS_TYPE)
                .setBusinessId(order.getId())
                .setBusinessNo(order.getPlatformOrderNo())
                .setRequestNo(order.getRequestNo())
                .setAuditUserId(userId)
                .setAuditUserType(source)
                .setAuditAction(action)
                .setBeforeStatus(before)
                .setAfterStatus(after)
                .setRejectReason(rejectReason)
                .setAuditRemark(remark)
                .setOperateSource(source)
                .setStatus(true)
                .setIsDeleted(false)
                .setCreateUserId(userId)
                .setUpdateUserId(userId));
    }
}







