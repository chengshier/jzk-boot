package com.zbkj.service.service.impl.jiuzhoukang.trade;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkAgentRelation;
import com.zbkj.common.model.jiuzhoukang.JkBusinessEvent;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkOfflineSale;
import com.zbkj.common.model.jiuzhoukang.JkOfflineSaleAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkOfflineSaleItem;
import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockBatch;
import com.zbkj.common.model.jiuzhoukang.JkStockBatchFlow;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleTrialRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleCreateRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleReturnRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockActionRequest;
import com.zbkj.service.dao.jiuzhoukang.JkAgentRelationDao;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessEventDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkOfflineSaleAuditLogDao;
import com.zbkj.service.dao.jiuzhoukang.JkOfflineSaleDao;
import com.zbkj.service.dao.jiuzhoukang.JkOfflineSaleItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockBatchDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockBatchFlowDao;
import com.zbkj.service.service.StoreProductAttrValueService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.impl.jiuzhoukang.commission.JkCommissionV31Service;
import com.zbkj.service.service.impl.jiuzhoukang.performance.JkOperationProfitLedgerService;
import com.zbkj.service.service.impl.jiuzhoukang.performance.JkPerformanceLedgerService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionReverseService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.stock.StockFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** V3.1 线下终端销售。登记不是业绩，只有审核通过或免审确认并真实出库后才产生业绩。 */
@Service
public class JkOfflineSaleService {
    private static final String BUSINESS_TYPE = "OFFLINE_SALE";

    @Autowired private JkOfflineSaleDao saleDao;
    @Autowired private JkOfflineSaleItemDao itemDao;
    @Autowired private JkOfflineSaleAuditLogDao auditDao;
    @Autowired private JkStockAccountDao stockAccountDao;
    @Autowired private JkStockBatchFlowDao batchFlowDao;
    @Autowired private JkStockBatchDao batchDao;
    @Autowired private JkAgentRelationDao relationDao;
    @Autowired private JkBusinessEventDao businessEventDao;
    @Autowired private JkCommissionRecordDao commissionRecordDao;
    @Autowired private StoreProductService productService;
    @Autowired private StoreProductAttrValueService skuService;
    @Autowired private JkUserContextService contextService;
    @Autowired private StockFlowService stockFlowService;
    @Autowired private JkPerformanceLedgerService performanceService;
    @Autowired private JkOperationProfitLedgerService profitService;
    @Autowired private JkCommissionV31Service commissionService;
    @Autowired private CommissionReverseService commissionReverseService;

    @Value("${jk.offline-sale.audit-amount-threshold:1000}") private BigDecimal auditAmountThreshold;
    @Value("${jk.offline-sale.voucher-amount-threshold:500}") private BigDecimal voucherAmountThreshold;

    @Transactional(rollbackFor = Exception.class)
    public JkOfflineSale create(Long sellerUserId, JkOfflineSaleCreateRequest request) {
        JkOfflineSale old = saleDao.selectOne(new LambdaQueryWrapper<JkOfflineSale>()
                .eq(JkOfflineSale::getRequestNo, request.getRequestNo()).last("limit 1"));
        if (old != null) {
            if (!sellerUserId.equals(old.getSellerUserId())) throw new CrmebException("requestNo 已被其他用户使用");
            return detail(old.getId());
        }
        JkUserContext context = contextService.getFrontContext(sellerUserId);
        validateSeller(context);
        Date now = new Date();
        Date saleTime = request.getSaleTime() == null ? now : request.getSaleTime();
        String maskedPhone = maskPhone(request.getCustomerPhone());
        BigDecimal total = BigDecimal.ZERO;
        for (JkOfflineSaleCreateRequest.Item line : request.getItems()) {
            total = total.add(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
        }
        boolean repeated = repeatedCustomer(sellerUserId, maskedPhone, saleTime);
        boolean auditRequired = total.compareTo(auditAmountThreshold) >= 0 || repeated
                || (total.compareTo(voucherAmountThreshold) >= 0 && StrUtil.isBlank(request.getVoucherUrl()));
        JkAgentRelation relation = currentRelation(sellerUserId);
        String relationSnapshot = relationSnapshot(context, relation);
        JkOfflineSale sale = new JkOfflineSale()
                .setSaleNo("OS" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr())
                .setRequestNo(request.getRequestNo()).setSellerUserId(sellerUserId)
                .setSellerRoleCode(context.getPrimaryRoleCode()).setCountyAgentUserId(context.getBelongCountyAgentId())
                .setRegionCode(context.getRegionCode()).setCustomerType(request.getCustomerType())
                .setCustomerUserId(request.getCustomerUserId()).setCustomerNameMasked(maskName(request.getCustomerName()))
                .setCustomerPhoneMasked(maskedPhone).setRegisteredCustomer(Boolean.TRUE.equals(request.getRegisteredCustomer()))
                .setPaymentMethod(request.getPaymentMethod()).setVoucherUrl(request.getVoucherUrl())
                .setPromotionSource(request.getPromotionSource()).setSaleTime(saleTime)
                .setTotalAmount(total.setScale(2, RoundingMode.HALF_UP)).setTotalCostAmount(BigDecimal.ZERO)
                .setTotalProfitAmount(BigDecimal.ZERO).setAuditRequired(auditRequired)
                .setAuditStatus(auditRequired ? "PENDING" : "NOT_REQUIRED")
                .setStatus(auditRequired ? "PENDING_AUDIT" : "CREATED")
                .setRelationSnapshotJson(relationSnapshot)
                .setSourceSnapshotJson("{\"repeatedCustomer\":" + repeated + ",\"voucherPresent\":" + (StrUtil.isNotBlank(request.getVoucherUrl())) + "}")
                .setIsDeleted(false).setCreateUserId(sellerUserId).setUpdateUserId(sellerUserId)
                .setCreateTime(now).setUpdateTime(now);
        saleDao.insert(sale);
        for (JkOfflineSaleCreateRequest.Item line : request.getItems()) insertLine(sale, line, now);
        log(sale, "CREATE", null, sale.getStatus(), sellerUserId, "FRONT", auditRequired ? "进入风险审核" : "免审待确认", null);
        if (!auditRequired) confirm(sale, sellerUserId, false, "系统免审确认");
        return detail(sale.getId());
    }

    public PageInfo<JkOfflineSale> list(Long sellerUserId, String status, String auditStatus, PageParamRequest pageParam) {
        Page<JkOfflineSale> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkOfflineSale> query = new LambdaQueryWrapper<JkOfflineSale>()
                .eq(JkOfflineSale::getIsDeleted, false).orderByDesc(JkOfflineSale::getSaleTime).orderByDesc(JkOfflineSale::getId);
        if (sellerUserId != null) query.eq(JkOfflineSale::getSellerUserId, sellerUserId);
        if (StrUtil.isNotBlank(status)) query.eq(JkOfflineSale::getStatus, status);
        if (StrUtil.isNotBlank(auditStatus)) query.eq(JkOfflineSale::getAuditStatus, auditStatus);
        List<JkOfflineSale> rows = saleDao.selectList(query);
        return CommonPage.copyPageInfo(page, rows);
    }

    public JkOfflineSale detail(Long id) {
        JkOfflineSale sale = require(id);
        sale.setItems(itemDao.selectList(new LambdaQueryWrapper<JkOfflineSaleItem>()
                .eq(JkOfflineSaleItem::getSaleId, id).eq(JkOfflineSaleItem::getIsDeleted, false).orderByAsc(JkOfflineSaleItem::getId)));
        sale.setAuditLogs(auditDao.selectList(new LambdaQueryWrapper<JkOfflineSaleAuditLog>()
                .eq(JkOfflineSaleAuditLog::getSaleId, id).orderByAsc(JkOfflineSaleAuditLog::getId)));
        return sale;
    }

    public JkOfflineSale detailMine(Long userId, Long id) {
        JkOfflineSale sale = require(id);
        if (!userId.equals(sale.getSellerUserId())) throw new CrmebException("无权查看该线下销售单");
        return detail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public JkOfflineSale audit(Long operatorId, JkOfflineSaleAuditRequest request) {
        JkOfflineSale sale = require(request.getSaleId());
        if (!"PENDING_AUDIT".equals(sale.getStatus()) || !"PENDING".equals(sale.getAuditStatus())) {
            throw new CrmebException("当前状态不能审核");
        }
        if (!Boolean.TRUE.equals(request.getApproved())) {
            sale.setAuditStatus("REJECTED").setStatus("AUDIT_REJECTED").setCancelReason(request.getRemark())
                    .setUpdateUserId(operatorId).setUpdateTime(new Date());
            saleDao.updateById(sale);
            log(sale, "AUDIT_REJECT", "PENDING_AUDIT", "AUDIT_REJECTED", operatorId, "ADMIN", request.getRemark(), null);
            return detail(sale.getId());
        }
        sale.setAuditStatus("APPROVED").setUpdateUserId(operatorId).setUpdateTime(new Date());
        saleDao.updateById(sale);
        confirm(sale, operatorId, true, request.getRemark());
        return detail(sale.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public JkOfflineSale cancel(Long userId, Long id, String reason) {
        JkOfflineSale sale = require(id);
        if (!userId.equals(sale.getSellerUserId())) throw new CrmebException("无权取消该销售单");
        if (!("PENDING_AUDIT".equals(sale.getStatus()) || "AUDIT_REJECTED".equals(sale.getStatus()) || "CREATED".equals(sale.getStatus()))) {
            throw new CrmebException("已确认销售不能直接取消，请提交退货");
        }
        String before = sale.getStatus();
        sale.setStatus("CANCELLED").setCancelReason(reason).setUpdateUserId(userId).setUpdateTime(new Date());
        saleDao.updateById(sale);
        log(sale, "CANCEL", before, "CANCELLED", userId, "FRONT", reason, null);
        return detail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public JkOfflineSale returnSale(Long userId, Long id, JkOfflineSaleReturnRequest request) {
        JkOfflineSale sale = require(id);
        if (!userId.equals(sale.getSellerUserId())) throw new CrmebException("无权操作该销售单");
        if (!("CONFIRMED".equals(sale.getStatus()) || "PARTIALLY_RETURNED".equals(sale.getStatus()))) {
            throw new CrmebException("当前销售单不能退货");
        }
        String eventKey = "OFFLINE_SALE_RETURN:" + request.getRequestNo();
        if (businessEventDao.selectOne(new LambdaQueryWrapper<JkBusinessEvent>().eq(JkBusinessEvent::getEventKey, eventKey).last("limit 1")) != null) {
            return detail(id);
        }
        JkOfflineSaleAuditLog returnLog = log(sale, "RETURN", sale.getStatus(), sale.getStatus(), userId, "FRONT", request.getReason(), request.getRequestNo());
        BigDecimal returnAmount = BigDecimal.ZERO;
        int returnQty = 0;
        for (JkOfflineSaleReturnRequest.Item returnLine : request.getItems()) {
            JkOfflineSaleItem item = itemDao.selectById(returnLine.getSaleItemId());
            if (item == null || !sale.getId().equals(item.getSaleId()) || Boolean.TRUE.equals(item.getIsDeleted())) throw new CrmebException("退货明细不存在");
            int available = item.getQuantity() - nvl(item.getReturnedQty());
            if (returnLine.getQuantity() > available) throw new CrmebException("退货数量超过可退数量");
            JkStockActionRequest action = action(sale, item, returnLine.getQuantity(), userId, "线下销售退货回补库存")
                    .setBusinessType("OFFLINE_SALE_RETURN").setBusinessId(returnLog.getId())
                    .setUnitCost(item.getUnitCost()).setBatchNo("RETURN-" + sale.getSaleNo() + "-" + returnLog.getId() + "-" + item.getId());
            stockFlowService.inboundStock(action);
            item.setReturnedQty(nvl(item.getReturnedQty()) + returnLine.getQuantity()).setUpdateTime(new Date());
            itemDao.updateById(item);
            returnAmount = returnAmount.add(item.getUnitPrice().multiply(BigDecimal.valueOf(returnLine.getQuantity())));
            returnQty += returnLine.getQuantity();
        }
        BigDecimal ratio = sale.getTotalAmount().signum() <= 0 ? BigDecimal.ONE
                : returnAmount.divide(sale.getTotalAmount(), 8, RoundingMode.HALF_UP).min(BigDecimal.ONE);
        performanceService.reverseBySource(BUSINESS_TYPE, sale.getId(), ratio, request.getReason());
        profitService.reverseBySource(BUSINESS_TYPE, sale.getId(), ratio, request.getReason());
        reverseCommissions(sale, ratio, request.getRequestNo(), request.getReason());
        boolean full = allReturned(sale.getId());
        sale.setStatus(full ? "RETURNED" : "PARTIALLY_RETURNED").setUpdateUserId(userId).setUpdateTime(new Date());
        saleDao.updateById(sale);
        Date now = new Date();
        try {
            businessEventDao.insert(new JkBusinessEvent().setEventKey(eventKey).setEventType("OFFLINE_SALE_RETURNED")
                    .setBusinessId(sale.getId()).setBusinessNo(sale.getSaleNo())
                    .setPayloadJson("{\"returnAmount\":" + returnAmount + ",\"returnQty\":" + returnQty + "}")
                    .setEventStatus("SUCCESS").setRetryCount(0).setMaxRetryCount(8).setOccurredTime(now).setProcessedTime(now)
                    .setCreateTime(now).setUpdateTime(now));
        } catch (DuplicateKeyException ignored) { throw new CrmebException("退货请求正在处理，请勿重复提交"); }
        return detail(id);
    }

    private void confirm(JkOfflineSale sale, Long operatorId, boolean audited, String remark) {
        if ("CONFIRMED".equals(sale.getStatus())) return;
        List<JkOfflineSaleItem> items = itemDao.selectList(new LambdaQueryWrapper<JkOfflineSaleItem>()
                .eq(JkOfflineSaleItem::getSaleId, sale.getId()).eq(JkOfflineSaleItem::getIsDeleted, false));
        JkStockAccount account = stockAccount(sale.getSellerUserId(), sale.getSellerRoleCode());
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        JkAgentRelation relation = currentRelation(sale.getSellerUserId());
        for (JkOfflineSaleItem item : items) {
            JkStockActionRequest action = action(sale, item, item.getQuantity(), operatorId, "线下终端销售确认出库").setStockAccountId(account.getId());
            stockFlowService.freezeStock(action);
            stockFlowService.outboundFrozenStock(action);
            CostSnapshot cost = costOf(sale.getId(), item);
            if (cost.quantity != item.getQuantity() || cost.unitCost == null) {
                throw new CrmebException("库存批次成本缺失或数量不完整，不能确认线下销售");
            }
            BigDecimal costAmount = cost.unitCost.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal profitAmount = item.getTotalAmount().subtract(costAmount).setScale(2, RoundingMode.HALF_UP);
            item.setUnitCost(cost.unitCost).setCostAmount(costAmount).setProfitAmount(profitAmount)
                    .setCostMethod("FIFO_BATCH").setCostSnapshotJson(cost.json).setUpdateTime(new Date());
            itemDao.updateById(item);
            totalCost = totalCost.add(costAmount);
            totalProfit = totalProfit.add(profitAmount);
            performanceService.record(new JkPerformanceRecord().setSourceType(BUSINESS_TYPE).setSourceId(sale.getId())
                    .setSourceNo(sale.getSaleNo()).setSourceItemId(item.getId()).setPerformanceType("RETAIL_OFFLINE")
                    .setOwnerUserId(sale.getSellerUserId()).setOwnerRoleCode(sale.getSellerRoleCode())
                    .setSourceUserId(sale.getCustomerUserId()).setDirectParentUserId(relation == null ? null : relation.getParentUserId())
                    .setCountyAgentUserId(sale.getCountyAgentUserId()).setRegionCode(sale.getRegionCode())
                    .setProductId(item.getProductId()).setSkuId(item.getSkuId()).setQuantity(item.getQuantity())
                    .setBaseAmount(item.getTotalAmount()).setPerformanceAmount(item.getTotalAmount())
                    .setRequestNo("PERFORMANCE:OFFLINE_SALE:" + sale.getId() + ":" + item.getId())
                    .setRelationSnapshotJson(sale.getRelationSnapshotJson()).setSourceSnapshotJson(sale.getSourceSnapshotJson()));
            profitService.record(new JkOperationProfitRecord().setUserId(sale.getSellerUserId()).setRoleCode(sale.getSellerRoleCode())
                    .setIncomeNature("OFFLINE_REALIZED").setSourceType(BUSINESS_TYPE).setSourceId(sale.getId())
                    .setSourceNo(sale.getSaleNo()).setSourceItemId(item.getId()).setProductId(item.getProductId()).setSkuId(item.getSkuId())
                    .setQuantity(item.getQuantity()).setRevenueAmount(item.getTotalAmount()).setCostAmount(costAmount).setProfitAmount(profitAmount)
                    .setCostSnapshotJson(cost.json).setRelationSnapshotJson(sale.getRelationSnapshotJson())
                    .setRequestNo("PROFIT:OFFLINE_SALE:" + sale.getId() + ":" + item.getId()));
            commissionService.createForScenario(new JkCommissionRuleTrialRequest()
                    .setSourceType("RETAIL_SALE").setSourceId(sale.getId()).setSourceItemId(item.getId()).setSourceNo(sale.getSaleNo())
                    .setOwnerUserId(sale.getSellerUserId()).setOwnerRoleCode(sale.getSellerRoleCode())
                    .setDirectParentUserId(relation == null ? null : relation.getParentUserId())
                    .setCountyAgentUserId(sale.getCountyAgentUserId()).setSellerUserId(sale.getSellerUserId())
                    .setPurchaserUserId(sale.getCustomerUserId()).setRegionCode(sale.getRegionCode())
                    .setProductId(item.getProductId()).setSkuId(item.getSkuId()).setQuantity(item.getQuantity())
                    .setBaseAmount(item.getTotalAmount()).setCostAmount(costAmount)
                    .setRegisteredCustomer(sale.getRegisteredCustomer()).setVoucherPresent(StrUtil.isNotBlank(sale.getVoucherUrl()))
                    .setAudited(audited || !Boolean.TRUE.equals(sale.getAuditRequired()))
                    .setRelationSnapshotJson(sale.getRelationSnapshotJson()).setSourceSnapshotJson(sale.getSourceSnapshotJson()),
                    "COMMISSION:OFFLINE_SALE:" + sale.getId() + ":" + item.getId());
        }
        String before = sale.getStatus();
        sale.setTotalCostAmount(totalCost).setTotalProfitAmount(totalProfit).setStatus("CONFIRMED")
                .setAuditStatus(Boolean.TRUE.equals(sale.getAuditRequired()) ? "APPROVED" : "NOT_REQUIRED")
                .setUpdateUserId(operatorId).setUpdateTime(new Date());
        saleDao.updateById(sale);
        log(sale, "CONFIRM", before, "CONFIRMED", operatorId, audited ? "ADMIN" : "SYSTEM", remark, null);
    }

    private void insertLine(JkOfflineSale sale, JkOfflineSaleCreateRequest.Item line, Date now) {
        StoreProduct product = productService.getById(line.getProductId());
        if (product == null || Boolean.TRUE.equals(product.getIsDel())) throw new CrmebException("商品不存在或已删除");
        StoreProductAttrValue sku = line.getSkuId() == null ? null : skuService.getById(line.getSkuId());
        if (sku != null && (!line.getProductId().equals(sku.getProductId()) || Boolean.TRUE.equals(sku.getIsDel()))) throw new CrmebException("商品规格不匹配");
        itemDao.insert(new JkOfflineSaleItem().setSaleId(sale.getId()).setProductId(line.getProductId()).setSkuId(line.getSkuId())
                .setProductName(product.getStoreName()).setSkuName(sku == null ? null : sku.getSuk()).setSkuCode(sku == null ? null : sku.getUnique())
                .setQuantity(line.getQuantity()).setUnitPrice(line.getUnitPrice().setScale(2, RoundingMode.HALF_UP))
                .setTotalAmount(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())).setScale(2, RoundingMode.HALF_UP))
                .setCostMethod("FIFO_BATCH").setReturnedQty(0).setIsDeleted(false).setCreateTime(now).setUpdateTime(now));
    }

    private CostSnapshot costOf(Long saleId, JkOfflineSaleItem item) {
        LambdaQueryWrapper<JkStockBatchFlow> query = new LambdaQueryWrapper<JkStockBatchFlow>()
                .eq(JkStockBatchFlow::getBusinessType, BUSINESS_TYPE).eq(JkStockBatchFlow::getBusinessId, saleId)
                .eq(JkStockBatchFlow::getProductId, item.getProductId()).eq(JkStockBatchFlow::getFlowType, "OUTBOUND")
                .eq(JkStockBatchFlow::getIsDeleted, false).orderByAsc(JkStockBatchFlow::getId);
        if (item.getSkuId() == null) query.isNull(JkStockBatchFlow::getSkuId); else query.eq(JkStockBatchFlow::getSkuId, item.getSkuId());
        int qty = 0; BigDecimal cost = BigDecimal.ZERO; StringBuilder json = new StringBuilder("[" ); boolean first = true;
        for (JkStockBatchFlow flow : batchFlowDao.selectList(query)) {
            JkStockBatch batch = batchDao.selectById(flow.getBatchId());
            if (batch == null || batch.getUnitCost() == null) return new CostSnapshot(null, qty, "{\"error\":\"COST_MISSING\"}");
            qty += flow.getChangeQty(); cost = cost.add(batch.getUnitCost().multiply(BigDecimal.valueOf(flow.getChangeQty())));
            if (!first) json.append(','); first = false;
            json.append("{\"batchId\":").append(batch.getId()).append(",\"batchNo\":\"").append(escape(batch.getBatchNo()))
                    .append("\",\"qty\":").append(flow.getChangeQty()).append(",\"unitCost\":").append(batch.getUnitCost()).append('}');
        }
        json.append(']');
        BigDecimal unit = qty == 0 ? null : cost.divide(BigDecimal.valueOf(qty), 6, RoundingMode.HALF_UP);
        return new CostSnapshot(unit, qty, json.toString());
    }

    private JkStockActionRequest action(JkOfflineSale sale, JkOfflineSaleItem item, int qty, Long operatorId, String remark) {
        return new JkStockActionRequest().setBusinessType(BUSINESS_TYPE).setBusinessId(sale.getId()).setBusinessNo(sale.getSaleNo())
                .setProductId(item.getProductId()).setSkuId(item.getSkuId()).setSkuCode(item.getSkuCode())
                .setQuantity(qty).setOperatorUserId(operatorId).setRemark(remark)
                .setStockAccountId(stockAccount(sale.getSellerUserId(), sale.getSellerRoleCode()).getId());
    }

    private JkStockAccount stockAccount(Long userId, String roleCode) {
        String type = JkBizConstants.ROLE_COUNTY_AGENT.equals(roleCode) ? JkBizConstants.STOCK_ACCOUNT_COUNTY_AGENT
                : JkBizConstants.ROLE_PARTNER.equals(roleCode) ? JkBizConstants.STOCK_ACCOUNT_PARTNER : JkBizConstants.STOCK_ACCOUNT_MAKER;
        JkStockAccount account = stockAccountDao.selectOne(new LambdaQueryWrapper<JkStockAccount>()
                .eq(JkStockAccount::getAccountType, type).eq(JkStockAccount::getOwnerUserId, userId)
                .eq(JkStockAccount::getStatus, true).eq(JkStockAccount::getIsDeleted, false).last("limit 1"));
        if (account == null) throw new CrmebException("销售人库存账户不存在");
        return account;
    }

    private void validateSeller(JkUserContext context) {
        if (context == null || Boolean.TRUE.equals(context.getFreezeStatus())
                || !JkBizConstants.AUDIT_STATUS_EFFECTIVE.equals(context.getAuditStatus())
                || !(JkBizConstants.ROLE_MAKER.equals(context.getPrimaryRoleCode())
                || JkBizConstants.ROLE_PARTNER.equals(context.getPrimaryRoleCode())
                || JkBizConstants.ROLE_COUNTY_AGENT.equals(context.getPrimaryRoleCode()))) {
            throw new CrmebException("当前身份不能登记线下销售");
        }
    }

    private JkAgentRelation currentRelation(Long userId) {
        return relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>()
                .eq(JkAgentRelation::getUserId, userId).eq(JkAgentRelation::getStatus, true)
                .eq(JkAgentRelation::getIsDeleted, false).last("limit 1"));
    }

    private boolean repeatedCustomer(Long sellerUserId, String maskedPhone, Date saleTime) {
        if (StrUtil.isBlank(maskedPhone)) return false;
        Calendar start = Calendar.getInstance(); start.setTime(saleTime); start.add(Calendar.MINUTE, -10);
        Calendar end = Calendar.getInstance(); end.setTime(saleTime); end.add(Calendar.MINUTE, 10);
        return saleDao.selectCount(new LambdaQueryWrapper<JkOfflineSale>()
                .eq(JkOfflineSale::getSellerUserId, sellerUserId).eq(JkOfflineSale::getCustomerPhoneMasked, maskedPhone)
                .between(JkOfflineSale::getSaleTime, start.getTime(), end.getTime()).eq(JkOfflineSale::getIsDeleted, false)) > 0;
    }

    private void reverseCommissions(JkOfflineSale sale, BigDecimal ratio, String requestNo, String reason) {
        List<JkCommissionRecord> records = commissionRecordDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>()
                .in(JkCommissionRecord::getSourceType, java.util.Arrays.asList("RETAIL_SALE", BUSINESS_TYPE))
                .eq(JkCommissionRecord::getSourceId, sale.getId()).eq(JkCommissionRecord::getIsDeleted, false));
        for (JkCommissionRecord record : records) {
            BigDecimal remaining = money(record.getCommissionAmount()).subtract(money(record.getReversedAmount())).max(BigDecimal.ZERO);
            BigDecimal amount = remaining.multiply(ratio).setScale(2, RoundingMode.HALF_UP).min(remaining);
            if (amount.signum() > 0) commissionReverseService.reverse(record.getId(), "OFFLINE_SALE_RETURN", sale.getId(), sale.getSaleNo(),
                    "OFFLINE_RETURN", amount, requestNo + ":" + record.getId(), null, reason);
        }
    }

    private boolean allReturned(Long saleId) {
        for (JkOfflineSaleItem item : itemDao.selectList(new LambdaQueryWrapper<JkOfflineSaleItem>()
                .eq(JkOfflineSaleItem::getSaleId, saleId).eq(JkOfflineSaleItem::getIsDeleted, false))) {
            if (nvl(item.getReturnedQty()) < item.getQuantity()) return false;
        }
        return true;
    }

    private JkOfflineSale require(Long id) {
        JkOfflineSale sale = saleDao.selectById(id);
        if (sale == null || Boolean.TRUE.equals(sale.getIsDeleted())) throw new CrmebException("线下销售单不存在");
        return sale;
    }

    private JkOfflineSaleAuditLog log(JkOfflineSale sale, String action, String before, String after, Long operator,
                                       String operatorType, String remark, String snapshot) {
        JkOfflineSaleAuditLog log = new JkOfflineSaleAuditLog().setSaleId(sale.getId()).setAction(action)
                .setBeforeStatus(before).setAfterStatus(after).setOperatorUserId(operator).setOperatorType(operatorType)
                .setRemark(remark).setSnapshotJson(snapshot).setCreateTime(new Date());
        auditDao.insert(log); return log;
    }

    private String relationSnapshot(JkUserContext context, JkAgentRelation relation) {
        return "{\"sellerUserId\":" + context.getUserId() + ",\"sellerRoleCode\":\"" + escape(context.getPrimaryRoleCode())
                + "\",\"directParentUserId\":" + (relation == null ? "null" : relation.getParentUserId())
                + ",\"countyAgentUserId\":" + context.getBelongCountyAgentId() + ",\"regionCode\":\"" + escape(context.getRegionCode()) + "\"}";
    }
    private String maskPhone(String value) { if (StrUtil.isBlank(value)) return null; String phone = value.trim(); return phone.length() >= 7 ? phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4) : "***"; }
    private String maskName(String value) { if (StrUtil.isBlank(value)) return null; String name = value.trim(); return name.length() <= 1 ? "*" : name.substring(0, 1) + "**"; }
    private int nvl(Integer value) { return value == null ? 0 : value; }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }

    private static final class CostSnapshot {
        private final BigDecimal unitCost; private final int quantity; private final String json;
        private CostSnapshot(BigDecimal unitCost, int quantity, String json) { this.unitCost = unitCost; this.quantity = quantity; this.json = json; }
    }
}
