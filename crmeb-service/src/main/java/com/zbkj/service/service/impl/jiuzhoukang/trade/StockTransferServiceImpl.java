package com.zbkj.service.service.impl.jiuzhoukang.trade;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.exception.jiuzhoukang.JkForbiddenException;
import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkOfflinePaymentVoucher;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferItem;
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
import com.zbkj.common.response.jiuzhoukang.JkProductTradeViewResponse;
import com.zbkj.common.response.jiuzhoukang.JkStockTransferDetailResponse;
import com.zbkj.service.dao.jiuzhoukang.JkOfflinePaymentVoucherDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferItemDao;
import com.zbkj.service.service.StoreProductAttrValueService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.jiuzhoukang.audit.JkAuditLogService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.price.PriceCalculateService;
import com.zbkj.service.service.jiuzhoukang.stock.StockFlowService;
import com.zbkj.service.service.jiuzhoukang.support.JkDisplayEnrichmentSupport;
import com.zbkj.service.service.jiuzhoukang.trade.JkTradeStatusSupport;
import com.zbkj.service.service.jiuzhoukang.trade.StockTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class StockTransferServiceImpl implements StockTransferService {
    private static final String BUSINESS_TYPE = "STOCK_TRANSFER";

    @Autowired
    private JkStockTransferDao transferDao;
    @Autowired
    private JkStockTransferItemDao itemDao;
    @Autowired
    private JkOfflinePaymentVoucherDao voucherDao;
    @Autowired
    private JkStockAccountDao accountDao;
    @Autowired
    private StoreProductService productService;
    @Autowired
    private StoreProductAttrValueService skuService;
    @Autowired
    private PriceCalculateService priceService;
    @Autowired
    private JkUserContextService contextService;
    @Autowired
    private StockFlowService stockFlowService;
    @Autowired
    private JkAuditLogService auditLogService;
    @Autowired
    private JkDisplayEnrichmentSupport displayEnrichmentSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockTransfer create(Long userId, JkTradeCreateRequest request) {
        JkStockTransfer exists = transferDao.selectOne(new LambdaQueryWrapper<JkStockTransfer>()
                .eq(JkStockTransfer::getRequestNo, request.getRequestNo())
                .last("limit 1"));
        if (exists != null) {
            if (!userId.equals(exists.getUserId())) {
                throw new CrmebException("requestNo 已存在");
            }
            return exists;
        }
        JkUserContext context = contextService.getFrontContext(userId);
        validApplicant(context);
        JkStockAccount from = account(JkBizConstants.STOCK_ACCOUNT_COUNTY_AGENT, context.getBelongCountyAgentId());
        if (!context.getRegionCode().equals(from.getRegionCode())) {
            throw new CrmebException("所属区县代区域不匹配");
        }
        JkStockAccount to = account(context.getPrimaryRoleCode().equals(JkBizConstants.ROLE_MAKER)
                ? JkBizConstants.STOCK_ACCOUNT_MAKER : JkBizConstants.STOCK_ACCOUNT_PARTNER, userId);
        JkStockTransfer transfer = new JkStockTransfer()
                .setTransferNo("ST" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr())
                .setRequestNo(request.getRequestNo())
                .setUserId(userId)
                .setRoleCode(context.getPrimaryRoleCode())
                .setCountyAgentId(context.getBelongCountyAgentId())
                .setRegionCode(context.getRegionCode())
                .setStatus("SUBMITTED")
                .setPayStatus("UNPAID")
                .setAuditStatus("PENDING")
                .setReceiveStatus("UNRECEIVED")
                .setTotalAmount(BigDecimal.ZERO)
                .setIsDeleted(false)
                .setCreateUserId(userId)
                .setUpdateUserId(userId)
                .setVersion(0);
        transferDao.insert(transfer);
        BigDecimal total = BigDecimal.ZERO;
        for (JkTradeLineRequest line : request.getItems()) {
            StoreProduct product = productService.getById(line.getProductId());
            if (product == null) {
                throw new CrmebException("商品不存在");
            }
            StoreProductAttrValue sku = line.getSkuId() == null ? null : skuService.getById(line.getSkuId());
            if (sku != null && !product.getId().equals(sku.getProductId())) {
                throw new CrmebException("商品规格不匹配");
            }
            JkProductTradeViewResponse.PriceInfo price = priceService.calculatePrice(product, sku, context);
            if (price == null || price.getAmount() == null) {
                throw new CrmebException("价格规则失效");
            }
            BigDecimal amount = price.getAmount().multiply(BigDecimal.valueOf(line.getQuantity()));
            itemDao.insert(new JkStockTransferItem()
                    .setTransferId(transfer.getId())
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
                    .setFromStockAccountId(from.getId())
                    .setToStockAccountId(to.getId())
                    .setIsDeleted(false)
                    .setVersion(0));
            total = total.add(amount);
        }
        transfer.setTotalAmount(total);
        transferDao.updateById(transfer);
        return enrichDisplay(transfer);
    }

    @Override
    public PageInfo<JkStockTransfer> getFrontList(Long userId, JkTradeDocumentSearchRequest request, PageParamRequest pageParamRequest) {
        Page<JkStockTransfer> page = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<JkStockTransfer> lqw = baseListWrapper(request);
        lqw.eq(JkStockTransfer::getUserId, userId);
        List<JkStockTransfer> list = transferDao.selectList(lqw);
        displayEnrichmentSupport.enrichStockTransfers(list);
        return CommonPage.copyPageInfo(page, list);
    }

    @Override
    public PageInfo<JkStockTransfer> getAdminList(Long countyUserId, JkTradeDocumentSearchRequest request, PageParamRequest pageParamRequest) {
        JkUserContext context = contextService.getFrontContext(countyUserId);
        requireCountyHandler(countyUserId, context);
        Page<JkStockTransfer> page = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<JkStockTransfer> lqw = baseListWrapper(request);
        lqw.eq(JkStockTransfer::getCountyAgentId, countyUserId).eq(JkStockTransfer::getRegionCode, context.getRegionCode());
        List<JkStockTransfer> list = transferDao.selectList(lqw);
        displayEnrichmentSupport.enrichStockTransfers(list);
        return CommonPage.copyPageInfo(page, list);
    }

    @Override
    public JkStockTransferDetailResponse getFrontDetail(Long userId, Long transferId) {
        JkStockTransfer transfer = one(transferId);
        if (!userId.equals(transfer.getUserId())) {
            throw new JkForbiddenException("无权查看该调拨单");
        }
        return buildDetail(transfer);
    }

    @Override
    public JkStockTransferDetailResponse getAdminDetail(Long countyUserId, Long transferId) {
        JkStockTransfer transfer = one(transferId);
        requireCountyOwner(countyUserId, transfer);
        return buildDetail(transfer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockTransfer audit(Long countyId, JkPaymentAuditRequest request) {
        JkStockTransfer transfer = one(request.getBusinessId());
        JkUserContext countyContext = contextService.getFrontContext(countyId);
        requireCountyAuditScope(countyId, countyContext, transfer);
        if (!"SUBMITTED".equals(transfer.getStatus())) {
            throw new CrmebException("当前状态不能审核调拨");
        }
        if (!Boolean.TRUE.equals(request.getApproved())) {
            transfer.setStatus("AUDIT_REJECTED")
                    .setAuditStatus("REJECTED")
                    .setAuditUserId(countyId)
                    .setAuditTime(new Date())
                    .setRejectReason(request.getRemark())
                    .setUpdateUserId(countyId);
            transferDao.updateById(transfer);
            log(transfer, countyId, "SUBMITTED", "AUDIT_REJECTED", "REJECT", request.getRemark(), request.getRemark(), "ADMIN");
            return enrichDisplay(transfer);
        }
        JkStockAccount from = account(JkBizConstants.STOCK_ACCOUNT_COUNTY_AGENT, countyId);
        for (JkStockTransferItem item : items(transfer.getId())) {
            stockFlowService.freezeStock(action(transfer, from.getId(), item, countyId, "区县代审核调拨冻结"));
        }
        transfer.setStatus("AUDIT_APPROVED")
                .setAuditStatus("APPROVED")
                .setAuditUserId(countyId)
                .setAuditTime(new Date())
                .setAuditRemark(request.getRemark())
                .setRejectReason(null)
                .setUpdateUserId(countyId);
        transferDao.updateById(transfer);
        log(transfer, countyId, "SUBMITTED", "AUDIT_APPROVED", "PASS", request.getRemark(), null, "ADMIN");
        return enrichDisplay(transfer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockTransfer submitVoucher(Long userId, Long transferId, JkPaymentVoucherRequest request) {
        JkStockTransfer transfer = one(transferId);
        if (!userId.equals(transfer.getUserId())) {
            throw new CrmebException("无权提交付款凭证");
        }
        if (!("AUDIT_APPROVED".equals(transfer.getStatus()) || "PAYMENT_REJECTED".equals(transfer.getStatus()))) {
            throw new CrmebException("当前状态不能提交付款凭证");
        }
        deactivateCurrentVoucher(transferId);
        voucherDao.insert(new JkOfflinePaymentVoucher()
                .setVoucherNo("PV" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr())
                .setBusinessType(BUSINESS_TYPE)
                .setBusinessId(transferId)
                .setVoucherUrl(request.getVoucherUrl())
                .setSubmitUserId(userId)
                .setAuditStatus("PENDING")
                .setIsCurrent(true)
                .setIsDeleted(false)
                .setVersion(0));
        String beforeStatus = transfer.getStatus();
        transfer.setStatus("PAYMENT_SUBMITTED")
                .setPayStatus("PAYMENT_SUBMITTED")
                .setRejectReason(null)
                .setUpdateUserId(userId);
        transferDao.updateById(transfer);
        log(transfer, userId, beforeStatus, "PAYMENT_SUBMITTED", "SUBMIT_VOUCHER", null, null, "FRONT");
        return enrichDisplay(transfer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockTransfer confirmPayment(Long countyId, JkPaymentAuditRequest request) {
        JkStockTransfer transfer = one(request.getBusinessId());
        requireCountyAuditScope(countyId, contextService.getFrontContext(countyId), transfer);
        if (!"PAYMENT_SUBMITTED".equals(transfer.getStatus())) {
            throw new CrmebException("当前状态不能确认付款");
        }
        JkOfflinePaymentVoucher voucher = voucher(transfer.getId());
        if (!Boolean.TRUE.equals(request.getApproved())) {
            releaseFrozenStock(transfer, countyId, "付款驳回释放冻结");
            voucher.setAuditStatus("REJECTED")
                    .setAuditUserId(countyId)
                    .setAuditTime(new Date())
                    .setRejectReason(request.getRemark());
            voucherDao.updateById(voucher);
            transfer.setStatus("PAYMENT_REJECTED")
                    .setPayStatus("REJECTED")
                    .setRejectReason(request.getRemark())
                    .setUpdateUserId(countyId);
            transferDao.updateById(transfer);
            log(transfer, countyId, "PAYMENT_SUBMITTED", "PAYMENT_REJECTED", "REJECT", request.getRemark(), request.getRemark(), "ADMIN");
            return enrichDisplay(transfer);
        }
        voucher.setAuditStatus("APPROVED")
                .setAuditUserId(countyId)
                .setAuditTime(new Date())
                .setRejectReason(null);
        voucherDao.updateById(voucher);
        transfer.setStatus("PAYMENT_APPROVED")
                .setPayStatus("APPROVED")
                .setRejectReason(null)
                .setUpdateUserId(countyId);
        transferDao.updateById(transfer);
        log(transfer, countyId, "PAYMENT_SUBMITTED", "PAYMENT_APPROVED", "PASS", request.getRemark(), null, "ADMIN");
        return enrichDisplay(transfer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockTransfer dispatch(Long countyId, JkBusinessActionRequest request) {
        JkStockTransfer transfer = one(request.getBusinessId());
        requireCountyAuditScope(countyId, contextService.getFrontContext(countyId), transfer);
        if (!"PAYMENT_APPROVED".equals(transfer.getStatus())) {
            throw new CrmebException("当前状态不能拨货");
        }
        JkStockAccount from = account(JkBizConstants.STOCK_ACCOUNT_COUNTY_AGENT, countyId);
        for (JkStockTransferItem item : items(transfer.getId())) {
            stockFlowService.outboundFrozenStock(action(transfer, from.getId(), item, countyId, "区县代调拨拨货出库"));
        }
        transfer.setStatus("TRANSFERRED")
                .setUpdateUserId(countyId);
        transferDao.updateById(transfer);
        log(transfer, countyId, "PAYMENT_APPROVED", "TRANSFERRED", "DISPATCH", request.getRemark(), null, "ADMIN");
        return enrichDisplay(transfer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockTransfer receive(Long userId, JkBusinessActionRequest request) {
        JkStockTransfer transfer = one(request.getBusinessId());
        if (!userId.equals(transfer.getUserId())) {
            throw new CrmebException("无权确认调拨收货");
        }
        if (!"TRANSFERRED".equals(transfer.getStatus())) {
            throw new CrmebException("当前状态不能确认收货");
        }
        int updated = transferDao.update(null, new UpdateWrapper<JkStockTransfer>()
                .eq("id", transfer.getId())
                .eq("status", "TRANSFERRED")
                .eq("is_deleted", false)
                .set("status", "STOCK_IN")
                .set("receive_status", "STOCK_IN")
                .set("update_user_id", userId)
                .set("update_time", new Date()));
        if (updated != 1) {
            throw new CrmebException("当前状态不能确认收货");
        }
        JkStockAccount to = account(transfer.getRoleCode().equals(JkBizConstants.ROLE_MAKER)
                ? JkBizConstants.STOCK_ACCOUNT_MAKER : JkBizConstants.STOCK_ACCOUNT_PARTNER, userId);
        for (JkStockTransferItem item : items(transfer.getId())) {
            stockFlowService.inboundStock(action(transfer, to.getId(), item, userId, "下级调拨确认收货入库"));
        }
        transfer.setStatus("STOCK_IN")
                .setReceiveStatus("STOCK_IN")
                .setUpdateUserId(userId);
        log(transfer, userId, "TRANSFERRED", "STOCK_IN", "RECEIVE", request.getRemark(), null, "FRONT");
        return enrichDisplay(transfer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockTransfer close(Long countyId, JkBusinessActionRequest request) {
        JkStockTransfer transfer = one(request.getBusinessId());
        requireCountyAuditScope(countyId, contextService.getFrontContext(countyId), transfer);
        String beforeStatus = transfer.getStatus();
        if (!("SUBMITTED".equals(beforeStatus)
                || "AUDIT_REJECTED".equals(beforeStatus)
                || "AUDIT_APPROVED".equals(beforeStatus)
                || "PAYMENT_SUBMITTED".equals(beforeStatus)
                || "PAYMENT_REJECTED".equals(beforeStatus)
                || "PAYMENT_APPROVED".equals(beforeStatus))) {
            throw new CrmebException("当前状态不能关闭");
        }
        if (JkTradeStatusSupport.transferRequiresFrozenRelease(beforeStatus)) {
            releaseFrozenStock(transfer, countyId, "调拨关闭释放冻结");
        }
        rejectCurrentVoucher(transfer.getId(), countyId, request.getRemark());
        transfer.setStatus("CLOSED")
                .setCancelReason(request.getRemark())
                .setUpdateUserId(countyId);
        transferDao.updateById(transfer);
        log(transfer, countyId, beforeStatus, "CLOSED", "CLOSE", request.getRemark(), null, "ADMIN");
        return transfer;
    }

    private void validApplicant(JkUserContext context) {
        if (context == null
                || Boolean.TRUE.equals(context.getFreezeStatus())
                || !(JkBizConstants.ROLE_MAKER.equals(context.getPrimaryRoleCode()) || JkBizConstants.ROLE_PARTNER.equals(context.getPrimaryRoleCode()))
                || context.getBelongCountyAgentId() == null
                || context.getRegionCode() == null
                || !JkBizConstants.AUDIT_STATUS_EFFECTIVE.equals(context.getAuditStatus())) {
            throw new CrmebException("创客/合伙人身份、所属区县代或区域无效");
        }
    }

    private JkStockTransfer one(Long id) {
        JkStockTransfer transfer = transferDao.selectById(id);
        if (transfer == null || Boolean.TRUE.equals(transfer.getIsDeleted())) {
            throw new CrmebException("调拨单不存在");
        }
        return transfer;
    }

    private void requireCountyOwner(Long countyId, JkStockTransfer transfer) {
        if (!countyId.equals(transfer.getCountyAgentId())) {
            throw new JkForbiddenException("无权操作非本区县代调拨单");
        }
    }

    private void requireCountyHandler(Long countyId, JkUserContext countyContext) {
        if (countyContext == null || Boolean.TRUE.equals(countyContext.getFreezeStatus())
                || !JkBizConstants.ROLE_COUNTY_AGENT.equals(countyContext.getPrimaryRoleCode())
                || !JkBizConstants.AUDIT_STATUS_EFFECTIVE.equals(countyContext.getAuditStatus())
                || countyContext.getRegionCode() == null
                || (!countyContext.getPermissions().contains(JkBizConstants.PERMISSION_STOCK_TRANSFER_CONFIRM)
                    && !countyContext.getPermissions().contains("platform.all"))) {
            throw new CrmebException("当前身份无区县代调拨处理权限");
        }
    }
    private void requireCountyAuditScope(Long countyId, JkUserContext countyContext, JkStockTransfer transfer) {
        requireCountyOwner(countyId, transfer);
        if (countyContext == null
                || Boolean.TRUE.equals(countyContext.getFreezeStatus())
                || !JkBizConstants.ROLE_COUNTY_AGENT.equals(countyContext.getPrimaryRoleCode())
                || !JkBizConstants.AUDIT_STATUS_EFFECTIVE.equals(countyContext.getAuditStatus())
                || !transfer.getRegionCode().equals(countyContext.getRegionCode())) {
            throw new CrmebException("无权审核非本区域调拨单");
        }
    }

    private JkStockAccount account(String type, Long ownerUserId) {
        JkStockAccount account = accountDao.selectOne(new LambdaQueryWrapper<JkStockAccount>()
                .eq(JkStockAccount::getAccountType, type)
                .eq(JkStockAccount::getOwnerUserId, ownerUserId)
                .eq(JkStockAccount::getStatus, true)
                .eq(JkStockAccount::getIsDeleted, false)
                .last("limit 1"));
        if (account == null) {
            throw new CrmebException("库存账户不存在");
        }
        return account;
    }

    private List<JkStockTransferItem> items(Long transferId) {
        return itemDao.selectList(new LambdaQueryWrapper<JkStockTransferItem>()
                .eq(JkStockTransferItem::getTransferId, transferId)
                .eq(JkStockTransferItem::getIsDeleted, false));
    }

    private JkOfflinePaymentVoucher voucher(Long transferId) {
        JkOfflinePaymentVoucher voucher = voucherDao.selectOne(new LambdaQueryWrapper<JkOfflinePaymentVoucher>()
                .eq(JkOfflinePaymentVoucher::getBusinessType, BUSINESS_TYPE)
                .eq(JkOfflinePaymentVoucher::getBusinessId, transferId)
                .eq(JkOfflinePaymentVoucher::getIsCurrent, true)
                .eq(JkOfflinePaymentVoucher::getIsDeleted, false)
                .last("limit 1"));
        if (voucher == null) {
            throw new CrmebException("付款凭证不存在");
        }
        return voucher;
    }

    private void deactivateCurrentVoucher(Long transferId) {
        voucherDao.update(null, new UpdateWrapper<JkOfflinePaymentVoucher>()
                .eq("business_type", BUSINESS_TYPE)
                .eq("business_id", transferId)
                .eq("is_current", true)
                .set("is_current", false));
    }

    private void rejectCurrentVoucher(Long transferId, Long countyId, String reason) {
        JkOfflinePaymentVoucher voucher = voucherDao.selectOne(new LambdaQueryWrapper<JkOfflinePaymentVoucher>()
                .eq(JkOfflinePaymentVoucher::getBusinessType, BUSINESS_TYPE)
                .eq(JkOfflinePaymentVoucher::getBusinessId, transferId)
                .eq(JkOfflinePaymentVoucher::getIsCurrent, true)
                .eq(JkOfflinePaymentVoucher::getIsDeleted, false)
                .last("limit 1"));
        if (voucher == null || "APPROVED".equals(voucher.getAuditStatus()) || "REJECTED".equals(voucher.getAuditStatus())) {
            return;
        }
        voucher.setAuditStatus("REJECTED")
                .setAuditUserId(countyId)
                .setAuditTime(new Date())
                .setRejectReason(reason);
        voucherDao.updateById(voucher);
    }

    private void releaseFrozenStock(JkStockTransfer transfer, Long countyId, String remark) {
        JkStockAccount county = account(JkBizConstants.STOCK_ACCOUNT_COUNTY_AGENT, countyId);
        for (JkStockTransferItem item : items(transfer.getId())) {
            stockFlowService.releaseFrozenStock(action(transfer, county.getId(), item, countyId, remark));
        }
    }

    private JkStockActionRequest action(JkStockTransfer transfer, Long stockAccountId, JkStockTransferItem item, Long userId, String remark) {
        return new JkStockActionRequest()
                .setBusinessType(BUSINESS_TYPE)
                .setBusinessId(transfer.getId())
                .setBusinessNo(transfer.getTransferNo())
                .setStockAccountId(stockAccountId)
                .setProductId(item.getProductId())
                .setSkuId(item.getSkuId())
                .setSkuCode(item.getSkuCode())
                .setQuantity(item.getQuantity())
                .setOperatorUserId(userId)
                .setRemark(remark);
    }

    private JkStockTransferDetailResponse buildDetail(JkStockTransfer transfer) {
        JkStockTransferDetailResponse response = new JkStockTransferDetailResponse();
        displayEnrichmentSupport.enrichStockTransfers(java.util.Collections.singletonList(transfer));
        List<JkOfflinePaymentVoucher> vouchers = voucherDao.selectList(new LambdaQueryWrapper<JkOfflinePaymentVoucher>()
                .eq(JkOfflinePaymentVoucher::getBusinessType, BUSINESS_TYPE)
                .eq(JkOfflinePaymentVoucher::getBusinessId, transfer.getId())
                .eq(JkOfflinePaymentVoucher::getIsDeleted, false)
                .orderByDesc(JkOfflinePaymentVoucher::getId));
        displayEnrichmentSupport.enrichOfflinePaymentVouchers(vouchers);
        response.setTransfer(transfer);
        response.setItems(items(transfer.getId()));
        response.setVouchers(vouchers);
        response.setAuditLogs(auditLogService.toResponses(auditLogService.list(new LambdaQueryWrapper<JkAuditLog>()
                .eq(JkAuditLog::getBusinessType, BUSINESS_TYPE)
                .eq(JkAuditLog::getBusinessId, transfer.getId())
                .eq(JkAuditLog::getIsDeleted, false)
                .orderByDesc(JkAuditLog::getId))));
        return response;
    }

    private LambdaQueryWrapper<JkStockTransfer> baseListWrapper(JkTradeDocumentSearchRequest request) {
        LambdaQueryWrapper<JkStockTransfer> lqw = new LambdaQueryWrapper<JkStockTransfer>()
                .eq(JkStockTransfer::getIsDeleted, false)
                .orderByDesc(JkStockTransfer::getId);
        if (request == null) {
            return lqw;
        }
        if (request.getStatus() != null && request.getStatus().trim().length() > 0) {
            lqw.eq(JkStockTransfer::getStatus, request.getStatus().trim());
        }
        if (request.getPayStatus() != null && request.getPayStatus().trim().length() > 0) {
            lqw.eq(JkStockTransfer::getPayStatus, request.getPayStatus().trim());
        }
        if (request.getAuditStatus() != null && request.getAuditStatus().trim().length() > 0) {
            lqw.eq(JkStockTransfer::getAuditStatus, request.getAuditStatus().trim());
        }
        if (request.getKeywords() != null && request.getKeywords().trim().length() > 0) {
            String keywords = request.getKeywords().trim();
            lqw.and(wrapper -> wrapper.like(JkStockTransfer::getTransferNo, keywords)
                    .or()
                    .like(JkStockTransfer::getRequestNo, keywords));
        }
        return lqw;
    }

    private String buildPriceSnapshot(JkProductTradeViewResponse.PriceInfo price) {
        return "{\"amount\":\"" + price.getAmount()
                + "\",\"priceType\":\"" + price.getPriceType()
                + "\",\"ruleId\":\"" + price.getRuleId()
                + "\",\"ruleVersion\":\"" + price.getRuleVersion() + "\"}";
    }

    private JkStockTransfer enrichDisplay(JkStockTransfer transfer) {
        if (transfer != null && displayEnrichmentSupport != null) {
            displayEnrichmentSupport.enrichStockTransfers(java.util.Collections.singletonList(transfer));
        }
        return transfer;
    }

    private void log(JkStockTransfer transfer, Long userId, String before, String after, String action, String remark, String rejectReason, String source) {
        auditLogService.saveAuditLog(new JkAuditLog()
                .setBusinessType(BUSINESS_TYPE)
                .setBusinessId(transfer.getId())
                .setBusinessNo(transfer.getTransferNo())
                .setRequestNo(transfer.getRequestNo())
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

