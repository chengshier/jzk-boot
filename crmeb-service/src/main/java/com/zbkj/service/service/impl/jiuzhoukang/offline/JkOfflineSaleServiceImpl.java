package com.zbkj.service.service.impl.jiuzhoukang.offline;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkAgentRelation;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkOfflineSale;
import com.zbkj.common.model.jiuzhoukang.JkOfflineSaleAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkOfflineSaleItem;
import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockBatch;
import com.zbkj.common.model.jiuzhoukang.JkStockBatchReservation;
import com.zbkj.common.model.jiuzhoukang.JkStockItem;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleTrialRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleCreateRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleItemRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockActionRequest;
import com.zbkj.service.dao.jiuzhoukang.JkAgentRelationDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkOfflineSaleAuditLogDao;
import com.zbkj.service.dao.jiuzhoukang.JkOfflineSaleDao;
import com.zbkj.service.dao.jiuzhoukang.JkOfflineSaleItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockBatchDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockBatchReservationDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkUserBusinessRoleDao;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionReverseService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionScenarioService;
import com.zbkj.service.service.jiuzhoukang.offline.JkOfflineSaleService;
import com.zbkj.service.service.jiuzhoukang.performance.JkPerformanceService;
import com.zbkj.service.service.jiuzhoukang.profit.JkOperationProfitService;
import com.zbkj.service.service.jiuzhoukang.stock.StockFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 线下销售真实闭环：库存扣减、批次成本、业绩、经营收益和可配置平台奖励。
 * 匿名客户允许登记，但高额、无凭证或规则要求审核时不会直接确认。
 */
@Service
public class JkOfflineSaleServiceImpl implements JkOfflineSaleService {
    @Autowired private JkOfflineSaleDao saleDao;
    @Autowired private JkOfflineSaleItemDao itemDao;
    @Autowired private JkOfflineSaleAuditLogDao logDao;
    @Autowired private JkUserBusinessRoleDao roleDao;
    @Autowired private JkAgentRelationDao relationDao;
    @Autowired private JkStockAccountDao stockAccountDao;
    @Autowired private JkStockItemDao stockItemDao;
    @Autowired private JkStockBatchReservationDao reservationDao;
    @Autowired private JkStockBatchDao batchDao;
    @Autowired private JkCommissionRecordDao commissionRecordDao;
    @Autowired private StockFlowService stockFlowService;
    @Autowired private JkPerformanceService performanceService;
    @Autowired private JkOperationProfitService profitService;
    @Autowired private CommissionScenarioService commissionScenarioService;
    @Autowired private CommissionReverseService commissionReverseService;

    @Value("${jk.offline-sale.audit-threshold:5000}") private BigDecimal auditThreshold;
    @Value("${jk.offline-sale.anonymous-enabled:true}") private boolean anonymousEnabled;
    @Value("${jk.offline-sale.voucher-required-threshold:1000}") private BigDecimal voucherRequiredThreshold;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkOfflineSale create(Long sellerUserId, JkOfflineSaleCreateRequest request) {
        JkOfflineSale old = saleDao.selectOne(new LambdaQueryWrapper<JkOfflineSale>()
                .eq(JkOfflineSale::getRequestNo, request.getRequestNo()).last("limit 1"));
        if (old != null) {
            if (!sellerUserId.equals(old.getSellerUserId())) throw new CrmebException("requestNo 已被其他用户使用");
            return detail(sellerUserId, old.getId(), false);
        }
        validateCustomer(request);
        JkUserBusinessRole role = requireSellerRole(sellerUserId);
        JkStockAccount account = requireStockAccount(sellerUserId);
        JkAgentRelation relation = currentRelation(sellerUserId);
        Long directParentId = relation == null ? null : relation.getParentUserId();
        Long countyAgentId = JkBizConstants.ROLE_COUNTY_AGENT.equals(role.getRoleCode())
                ? sellerUserId : role.getBelongCountyAgentId();
        if (countyAgentId == null && directParentId != null) countyAgentId = resolveCountyAgent(directParentId);

        Date now = new Date();
        JkOfflineSale sale = new JkOfflineSale().setSaleNo("OS" + IdWorker.getIdStr())
                .setRequestNo(request.getRequestNo()).setSellerUserId(sellerUserId).setSellerRoleCode(role.getRoleCode())
                .setCountyAgentUserId(countyAgentId).setDirectParentUserId(directParentId).setRegionCode(role.getRegionCode())
                .setCustomerType(request.getCustomerType()).setCustomerUserId(request.getCustomerUserId())
                .setCustomerNameMasked(maskName(request.getCustomerName())).setCustomerPhoneMasked(maskPhone(request.getCustomerPhone()))
                .setRegisteredCustomer(Boolean.TRUE.equals(request.getRegisteredCustomer()) || request.getCustomerUserId() != null)
                .setPayMethod(request.getPayMethod()).setSaleTime(request.getSaleTime())
                .setVoucherUrls(JSONUtil.toJsonStr(request.getVoucherUrls() == null ? new ArrayList<String>() : request.getVoucherUrls()))
                .setPromotionSource(request.getPromotionSource()).setIsDeleted(false).setCreateUserId(sellerUserId)
                .setUpdateUserId(sellerUserId).setCreateTime(now).setUpdateTime(now).setVersion(0);

        Set<String> skuKeys = new HashSet<String>();
        int totalQty = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (JkOfflineSaleItemRequest item : request.getItems()) {
            String key = item.getProductId() + ":" + item.getSkuId();
            if (!skuKeys.add(key)) throw new CrmebException("同一商品规格不能重复添加，请合并数量");
            JkStockItem stock = requireStockItem(account.getId(), item.getProductId(), item.getSkuId(), item.getQuantity());
            totalQty += item.getQuantity();
            totalAmount = totalAmount.add(item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())));
            if (StrUtil.isBlank(stock.getSkuCode())) throw new CrmebException("库存规格编码缺失，请先修复库存主数据");
        }
        boolean voucherMissing = request.getVoucherUrls() == null || request.getVoucherUrls().isEmpty();
        boolean requiresAudit = totalAmount.compareTo(auditThreshold) >= 0
                || (voucherMissing && totalAmount.compareTo(voucherRequiredThreshold) >= 0)
                || "ANONYMOUS".equals(request.getCustomerType());
        sale.setTotalQuantity(totalQty).setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP))
                .setAuditRequired(requiresAudit).setStatus(requiresAudit ? "PENDING_AUDIT" : "PENDING_CONFIRM")
                .setRelationSnapshotJson(relationSnapshot(sale, relation))
                .setRiskSnapshotJson(riskSnapshot(totalAmount, voucherMissing, requiresAudit));
        saleDao.insert(sale);

        for (JkOfflineSaleItemRequest item : request.getItems()) {
            JkStockItem stock = requireStockItem(account.getId(), item.getProductId(), item.getSkuId(), item.getQuantity());
            itemDao.insert(new JkOfflineSaleItem().setSaleId(sale.getId()).setProductId(item.getProductId()).setSkuId(item.getSkuId())
                    .setSkuCode(stock.getSkuCode()).setQuantity(item.getQuantity()).setUnitPrice(item.getUnitPrice().setScale(2, RoundingMode.HALF_UP))
                    .setTotalAmount(item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())).setScale(2, RoundingMode.HALF_UP))
                    .setStockAccountId(account.getId()).setIsDeleted(false).setCreateTime(now).setUpdateTime(now));
        }
        writeLog(sale.getId(), "CREATE", null, sale.getStatus(), sellerUserId, "APP", "登记线下销售", request.getRequestNo());
        if (!requiresAudit) confirmSale(sale, account, sellerUserId, request.getRequestNo(), "低风险销售自动确认");
        return detail(sellerUserId, sale.getId(), false);
    }

    @Override
    public PageInfo<JkOfflineSale> list(Long sellerUserId, String status, PageParamRequest pageParam) {
        Page<JkOfflineSale> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkOfflineSale> query = new LambdaQueryWrapper<JkOfflineSale>()
                .eq(JkOfflineSale::getIsDeleted, false).orderByDesc(JkOfflineSale::getId);
        if (sellerUserId != null) query.eq(JkOfflineSale::getSellerUserId, sellerUserId);
        if (StrUtil.isNotBlank(status)) query.eq(JkOfflineSale::getStatus, status);
        List<JkOfflineSale> rows = saleDao.selectList(query);
        rows.forEach(this::enrichStatus);
        return CommonPage.copyPageInfo(page, rows);
    }

    @Override
    public JkOfflineSale detail(Long viewerUserId, Long id, boolean admin) {
        JkOfflineSale sale = requireSale(id);
        if (!admin && !viewerUserId.equals(sale.getSellerUserId())) throw new CrmebException("无权查看该线下销售单");
        sale.setItems(itemDao.selectList(new LambdaQueryWrapper<JkOfflineSaleItem>()
                .eq(JkOfflineSaleItem::getSaleId, id).eq(JkOfflineSaleItem::getIsDeleted, false).orderByAsc(JkOfflineSaleItem::getId)));
        sale.setAuditLogs(logDao.selectList(new LambdaQueryWrapper<JkOfflineSaleAuditLog>()
                .eq(JkOfflineSaleAuditLog::getSaleId, id).orderByAsc(JkOfflineSaleAuditLog::getId)));
        enrichStatus(sale);
        return sale;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkOfflineSale audit(Long operatorId, JkOfflineSaleAuditRequest request) {
        JkOfflineSale sale = requireSale(request.getSaleId());
        JkOfflineSaleAuditLog oldLog = logDao.selectOne(new LambdaQueryWrapper<JkOfflineSaleAuditLog>()
                .eq(JkOfflineSaleAuditLog::getRequestNo, request.getRequestNo()).last("limit 1"));
        if (oldLog != null) return detail(operatorId, sale.getId(), true);
        if (!"PENDING_AUDIT".equals(sale.getStatus())) throw new CrmebException("当前状态不能审核");
        if (!Boolean.TRUE.equals(request.getApproved())) {
            String before = sale.getStatus();
            sale.setStatus("REJECTED").setAuditUserId(operatorId).setAuditTime(new Date()).setAuditRemark(request.getRemark())
                    .setUpdateUserId(operatorId).setUpdateTime(new Date());
            saleDao.updateById(sale);
            writeLog(sale.getId(), "AUDIT_REJECT", before, sale.getStatus(), operatorId, "ADMIN", request.getRemark(), request.getRequestNo());
            return detail(operatorId, sale.getId(), true);
        }
        sale.setAuditUserId(operatorId).setAuditTime(new Date()).setAuditRemark(request.getRemark());
        confirmSale(sale, requireStockAccount(sale.getSellerUserId()), operatorId, request.getRequestNo(), request.getRemark());
        return detail(operatorId, sale.getId(), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkOfflineSale cancel(Long sellerUserId, Long id, JkOfflineSaleActionRequest request) {
        JkOfflineSale sale = requireOwner(sellerUserId, id);
        if (!Arrays.asList("PENDING_AUDIT", "PENDING_CONFIRM").contains(sale.getStatus())) throw new CrmebException("当前状态不能取消，请使用退货流程");
        String before = sale.getStatus();
        sale.setStatus("CANCELLED").setCancelReason(request.getReason()).setUpdateUserId(sellerUserId).setUpdateTime(new Date());
        saleDao.updateById(sale);
        writeLog(id, "CANCEL", before, sale.getStatus(), sellerUserId, "APP", request.getReason(), request.getRequestNo());
        return detail(sellerUserId, id, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkOfflineSale returnSale(Long sellerUserId, Long id, JkOfflineSaleActionRequest request) {
        JkOfflineSale sale = requireOwner(sellerUserId, id);
        if ("RETURNED".equals(sale.getStatus())) return detail(sellerUserId, id, false);
        if (!"CONFIRMED".equals(sale.getStatus())) throw new CrmebException("只有已确认销售单可以退货");
        JkStockAccount account = requireStockAccount(sellerUserId);
        List<JkOfflineSaleItem> items = itemDao.selectList(new LambdaQueryWrapper<JkOfflineSaleItem>()
                .eq(JkOfflineSaleItem::getSaleId, id).eq(JkOfflineSaleItem::getIsDeleted, false));
        for (JkOfflineSaleItem item : items) {
            stockFlowService.inboundStock(stockAction("OFFLINE_SALE_RETURN", sale, item, account.getId(), sellerUserId,
                    "线下销售退货回补库存：" + request.getReason()).setUnitCost(item.getUnitCost()));
        }
        performanceService.reverse("OFFLINE_SALE", id, null, null, request.getRequestNo(), request.getReason());
        profitService.reverse("OFFLINE_SALE", id, null, null, request.getRequestNo(), request.getReason());
        reverseCommissions(sale, request.getRequestNo(), request.getReason(), sellerUserId);
        String before = sale.getStatus();
        sale.setStatus("RETURNED").setUpdateUserId(sellerUserId).setUpdateTime(new Date());
        saleDao.updateById(sale);
        writeLog(id, "RETURN", before, sale.getStatus(), sellerUserId, "APP", request.getReason(), request.getRequestNo());
        return detail(sellerUserId, id, false);
    }

    private void confirmSale(JkOfflineSale sale, JkStockAccount account, Long operatorId, String requestNo, String remark) {
        if ("CONFIRMED".equals(sale.getStatus())) return;
        String before = sale.getStatus();
        List<JkOfflineSaleItem> items = itemDao.selectList(new LambdaQueryWrapper<JkOfflineSaleItem>()
                .eq(JkOfflineSaleItem::getSaleId, sale.getId()).eq(JkOfflineSaleItem::getIsDeleted, false).orderByAsc(JkOfflineSaleItem::getId));
        for (JkOfflineSaleItem item : items) {
            requireStockItem(account.getId(), item.getProductId(), item.getSkuId(), item.getQuantity());
            JkStockActionRequest action = stockAction("OFFLINE_SALE", sale, item, account.getId(), operatorId, "线下终端销售出库");
            stockFlowService.freezeStock(action);
            CostSnapshot cost = calculateReservedCost(sale.getId(), account.getId(), item);
            stockFlowService.outboundFrozenStock(action);
            BigDecimal profit = item.getTotalAmount().subtract(cost.costAmount).setScale(2, RoundingMode.HALF_UP);
            item.setUnitCost(cost.unitCost).setCostAmount(cost.costAmount).setProfitAmount(profit)
                    .setCostSnapshotJson(cost.snapshotJson).setUpdateTime(new Date());
            itemDao.updateById(item);

            String relationJson = sale.getRelationSnapshotJson();
            performanceService.record(new JkPerformanceRecord().setSourceType("OFFLINE_SALE").setSourceId(sale.getId())
                    .setSourceNo(sale.getSaleNo()).setSourceItemId(item.getId()).setPerformanceType("RETAIL_OFFLINE")
                    .setOwnerUserId(sale.getSellerUserId()).setOwnerRoleCode(sale.getSellerRoleCode())
                    .setSourceUserId(sale.getCustomerUserId()).setDirectParentUserId(sale.getDirectParentUserId())
                    .setCountyAgentUserId(sale.getCountyAgentUserId()).setRegionCode(sale.getRegionCode())
                    .setProductId(item.getProductId()).setSkuId(item.getSkuId()).setQuantity(item.getQuantity())
                    .setBaseAmount(item.getTotalAmount()).setPerformanceAmount(item.getTotalAmount())
                    .setRequestNo(requestNo).setRelationSnapshotJson(relationJson).setSourceSnapshotJson(sourceSnapshot(sale, item))
                    .setActionKey("PERF:OFFLINE_SALE:" + item.getId() + ":" + sale.getSellerUserId()));
            profitService.record(new JkOperationProfitRecord().setUserId(sale.getSellerUserId()).setRoleCode(sale.getSellerRoleCode())
                    .setSourceType("OFFLINE_SALE").setSourceId(sale.getId()).setSourceNo(sale.getSaleNo()).setSourceItemId(item.getId())
                    .setProductId(item.getProductId()).setSkuId(item.getSkuId()).setQuantity(item.getQuantity())
                    .setRevenueAmount(item.getTotalAmount()).setCostAmount(cost.costAmount).setProfitAmount(profit)
                    .setCostSnapshotJson(cost.snapshotJson).setRelationSnapshotJson(relationJson).setRequestNo(requestNo)
                    .setActionKey("PROFIT:OFFLINE_SALE:" + item.getId() + ":" + sale.getSellerUserId()));

            JkCommissionRuleTrialRequest scenario = new JkCommissionRuleTrialRequest();
            scenario.setScenario("RETAIL_OFFLINE_CONFIRMED");
            scenario.setSourceType("OFFLINE_SALE");
            scenario.setSourceId(sale.getId());
            scenario.setSourceItemId(item.getId());
            scenario.setBuyerUserId(sale.getCustomerUserId());
            scenario.setSellerUserId(sale.getSellerUserId());
            scenario.setDirectParentUserId(sale.getDirectParentUserId());
            scenario.setCountyAgentUserId(sale.getCountyAgentUserId());
            scenario.setRegionCode(sale.getRegionCode());
            scenario.setProductId(item.getProductId());
            scenario.setSkuId(item.getSkuId());
            scenario.setQuantity(item.getQuantity());
            scenario.setBaseAmount(item.getTotalAmount());
            scenario.setRealGrossProfit(profit);
            scenario.setRegisteredCustomer(sale.getRegisteredCustomer());
            scenario.setVoucherPresent(hasVoucher(sale));
            scenario.setAudited(Boolean.TRUE.equals(sale.getAuditRequired()) ? sale.getAuditTime() != null : true);
            commissionScenarioService.dispatch(scenario, "COMMISSION:OFFLINE_SALE:" + item.getId(), sale.getSaleNo(), requestNo);
        }
        sale.setStatus("CONFIRMED").setUpdateUserId(operatorId).setUpdateTime(new Date());
        saleDao.updateById(sale);
        writeLog(sale.getId(), "CONFIRM", before, sale.getStatus(), operatorId,
                operatorId.equals(sale.getSellerUserId()) ? "APP" : "ADMIN", remark, requestNo);
    }

    private CostSnapshot calculateReservedCost(Long saleId, Long accountId, JkOfflineSaleItem item) {
        LambdaQueryWrapper<JkStockBatchReservation> query = new LambdaQueryWrapper<JkStockBatchReservation>()
                .eq(JkStockBatchReservation::getBusinessType, "OFFLINE_SALE").eq(JkStockBatchReservation::getBusinessId, saleId)
                .eq(JkStockBatchReservation::getStockAccountId, accountId).eq(JkStockBatchReservation::getProductId, item.getProductId())
                .eq(JkStockBatchReservation::getSkuId, item.getSkuId()).eq(JkStockBatchReservation::getIsDeleted, false);
        List<JkStockBatchReservation> reservations = reservationDao.selectList(query);
        if (reservations.isEmpty()) throw new CrmebException("未生成销售批次预占，无法核算成本");
        BigDecimal totalCost = BigDecimal.ZERO;
        int totalQty = 0;
        List<Object> snapshots = new ArrayList<Object>();
        for (JkStockBatchReservation reservation : reservations) {
            JkStockBatch batch = batchDao.selectById(reservation.getBatchId());
            if (batch == null || batch.getUnitCost() == null) throw new CrmebException("库存批次成本缺失，不能确认线下销售");
            int qty = reservation.getFrozenQty() == null ? 0 : reservation.getFrozenQty();
            totalQty += qty;
            totalCost = totalCost.add(batch.getUnitCost().multiply(new BigDecimal(qty)));
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<String, Object>();
            row.put("batchId", batch.getId()); row.put("batchNo", batch.getBatchNo()); row.put("quantity", qty); row.put("unitCost", batch.getUnitCost());
            snapshots.add(row);
        }
        if (totalQty != item.getQuantity()) throw new CrmebException("销售批次预占数量与销售数量不一致");
        BigDecimal unitCost = totalCost.divide(new BigDecimal(totalQty), 6, RoundingMode.HALF_UP);
        return new CostSnapshot(unitCost, totalCost.setScale(2, RoundingMode.HALF_UP), JSONUtil.toJsonStr(snapshots));
    }

    private void reverseCommissions(JkOfflineSale sale, String requestNo, String reason, Long operatorId) {
        List<JkCommissionRecord> rows = commissionRecordDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>()
                .eq(JkCommissionRecord::getSourceType, "OFFLINE_SALE").eq(JkCommissionRecord::getSourceId, sale.getId())
                .eq(JkCommissionRecord::getIsDeleted, false));
        for (JkCommissionRecord row : rows) {
            BigDecimal remain = money(row.getCommissionAmount()).subtract(money(row.getReversedAmount())).max(BigDecimal.ZERO);
            if (remain.signum() <= 0) continue;
            commissionReverseService.reverse(row.getId(), "OFFLINE_SALE_RETURN", sale.getId(), sale.getSaleNo(),
                    "OFFLINE_SALE_RETURN", remain, requestNo + ":" + row.getId(), operatorId, reason);
        }
    }

    private JkStockActionRequest stockAction(String businessType, JkOfflineSale sale, JkOfflineSaleItem item,
                                              Long accountId, Long operatorId, String remark) {
        return new JkStockActionRequest().setBusinessType(businessType).setBusinessId(sale.getId()).setBusinessNo(sale.getSaleNo())
                .setStockAccountId(accountId).setProductId(item.getProductId()).setSkuId(item.getSkuId()).setSkuCode(item.getSkuCode())
                .setQuantity(item.getQuantity()).setOperatorUserId(operatorId).setRemark(remark);
    }

    private JkUserBusinessRole requireSellerRole(Long userId) {
        JkUserBusinessRole role = roleDao.selectOne(new LambdaQueryWrapper<JkUserBusinessRole>()
                .eq(JkUserBusinessRole::getUserId, userId).eq(JkUserBusinessRole::getAuditStatus, JkBizConstants.AUDIT_STATUS_EFFECTIVE)
                .eq(JkUserBusinessRole::getEffectiveStatus, JkBizConstants.EFFECTIVE_STATUS_ENABLED)
                .eq(JkUserBusinessRole::getFreezeStatus, false).eq(JkUserBusinessRole::getStatus, true)
                .eq(JkUserBusinessRole::getIsDeleted, false).in(JkUserBusinessRole::getRoleCode,
                        Arrays.asList(JkBizConstants.ROLE_MAKER, JkBizConstants.ROLE_PARTNER, JkBizConstants.ROLE_COUNTY_AGENT))
                .orderByDesc(JkUserBusinessRole::getIsPrimary).orderByDesc(JkUserBusinessRole::getId).last("limit 1"));
        if (role == null) throw new CrmebException("当前身份无效、已冻结或不允许登记线下销售");
        return role;
    }

    private JkStockAccount requireStockAccount(Long userId) {
        JkStockAccount account = stockAccountDao.selectOne(new LambdaQueryWrapper<JkStockAccount>()
                .eq(JkStockAccount::getOwnerUserId, userId).eq(JkStockAccount::getStatus, true)
                .eq(JkStockAccount::getIsDeleted, false).orderByDesc(JkStockAccount::getId).last("limit 1"));
        if (account == null) throw new CrmebException("当前用户没有可用库存账户");
        return account;
    }

    private JkStockItem requireStockItem(Long accountId, Integer productId, Integer skuId, Integer quantity) {
        JkStockItem item = stockItemDao.selectOne(new LambdaQueryWrapper<JkStockItem>()
                .eq(JkStockItem::getStockAccountId, accountId).eq(JkStockItem::getProductId, productId)
                .eq(JkStockItem::getSkuId, skuId).eq(JkStockItem::getIsDeleted, false).last("limit 1"));
        if (item == null || item.getAvailableQty() == null || item.getAvailableQty() < quantity) throw new CrmebException("销售库存不足");
        return item;
    }

    private JkAgentRelation currentRelation(Long userId) {
        return relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>().eq(JkAgentRelation::getUserId, userId)
                .eq(JkAgentRelation::getStatus, true).eq(JkAgentRelation::getIsDeleted, false).last("limit 1"));
    }

    private Long resolveCountyAgent(Long parentUserId) {
        JkUserBusinessRole role = roleDao.selectOne(new LambdaQueryWrapper<JkUserBusinessRole>()
                .eq(JkUserBusinessRole::getUserId, parentUserId).eq(JkUserBusinessRole::getAuditStatus, JkBizConstants.AUDIT_STATUS_EFFECTIVE)
                .eq(JkUserBusinessRole::getIsDeleted, false).orderByDesc(JkUserBusinessRole::getIsPrimary).last("limit 1"));
        if (role == null) return null;
        return JkBizConstants.ROLE_COUNTY_AGENT.equals(role.getRoleCode()) ? parentUserId : role.getBelongCountyAgentId();
    }

    private JkOfflineSale requireSale(Long id) {
        JkOfflineSale sale = saleDao.selectById(id);
        if (sale == null || Boolean.TRUE.equals(sale.getIsDeleted())) throw new CrmebException("线下销售单不存在");
        return sale;
    }
    private JkOfflineSale requireOwner(Long userId, Long id) { JkOfflineSale sale = requireSale(id); if (!userId.equals(sale.getSellerUserId())) throw new CrmebException("无权操作该销售单"); return sale; }

    private void validateCustomer(JkOfflineSaleCreateRequest request) {
        if (!Arrays.asList("REGISTERED_OLD", "REGISTERED_NEW", "ANONYMOUS").contains(request.getCustomerType())) throw new CrmebException("客户类型非法");
        if ("ANONYMOUS".equals(request.getCustomerType()) && !anonymousEnabled) throw new CrmebException("当前未开放匿名客户线下销售登记");
        if (!"ANONYMOUS".equals(request.getCustomerType()) && request.getCustomerUserId() == null) throw new CrmebException("已注册客户必须关联用户");
        if ("ANONYMOUS".equals(request.getCustomerType()) && StrUtil.isBlank(request.getCustomerPhone())) throw new CrmebException("匿名客户至少填写可脱敏手机号用于重复风险检查");
    }

    private void writeLog(Long saleId, String action, String before, String after, Long operatorId, String operatorType, String remark, String requestNo) {
        logDao.insert(new JkOfflineSaleAuditLog().setSaleId(saleId).setAction(action).setBeforeStatus(before).setAfterStatus(after)
                .setOperatorUserId(operatorId).setOperatorType(operatorType).setRemark(remark).setRequestNo(requestNo).setCreateTime(new Date()));
    }

    private void enrichStatus(JkOfflineSale sale) {
        if (sale == null) return;
        String status = sale.getStatus();
        if ("PENDING_AUDIT".equals(status)) { sale.setStatusText("待审核").setStatusTag("warning"); }
        else if ("PENDING_CONFIRM".equals(status)) { sale.setStatusText("待确认").setStatusTag("warning"); }
        else if ("CONFIRMED".equals(status)) { sale.setStatusText("已确认").setStatusTag("success"); }
        else if ("RETURNED".equals(status)) { sale.setStatusText("已退货").setStatusTag("info"); }
        else if ("REJECTED".equals(status)) { sale.setStatusText("已驳回").setStatusTag("danger"); }
        else if ("CANCELLED".equals(status)) { sale.setStatusText("已取消").setStatusTag("info"); }
        else { sale.setStatusText(status).setStatusTag("info"); }
    }

    private String relationSnapshot(JkOfflineSale sale, JkAgentRelation relation) {
        java.util.Map<String, Object> value = new java.util.LinkedHashMap<String, Object>();
        value.put("sellerUserId", sale.getSellerUserId()); value.put("sellerRoleCode", sale.getSellerRoleCode());
        value.put("directParentUserId", sale.getDirectParentUserId()); value.put("countyAgentUserId", sale.getCountyAgentUserId());
        value.put("regionCode", sale.getRegionCode()); value.put("relationId", relation == null ? null : relation.getId());
        return JSONUtil.toJsonStr(value);
    }
    private String sourceSnapshot(JkOfflineSale sale, JkOfflineSaleItem item) {
        java.util.Map<String, Object> value = new java.util.LinkedHashMap<String, Object>();
        value.put("saleNo", sale.getSaleNo()); value.put("customerType", sale.getCustomerType()); value.put("customerUserId", sale.getCustomerUserId());
        value.put("registeredCustomer", sale.getRegisteredCustomer()); value.put("productId", item.getProductId()); value.put("skuId", item.getSkuId());
        value.put("quantity", item.getQuantity()); value.put("paidAmount", item.getTotalAmount()); value.put("costAmount", item.getCostAmount());
        return JSONUtil.toJsonStr(value);
    }
    private String riskSnapshot(BigDecimal amount, boolean voucherMissing, boolean audit) { return "{\"totalAmount\":" + amount + ",\"voucherMissing\":" + voucherMissing + ",\"auditRequired\":" + audit + "}"; }
    private boolean hasVoucher(JkOfflineSale sale) { return StrUtil.isNotBlank(sale.getVoucherUrls()) && !"[]".equals(sale.getVoucherUrls()); }
    private String maskName(String name) { if (StrUtil.isBlank(name)) return null; return name.length() <= 1 ? "*" : name.substring(0, 1) + "**"; }
    private String maskPhone(String phone) { if (StrUtil.isBlank(phone)) return null; String value = phone.trim(); return value.length() >= 7 ? value.substring(0, 3) + "****" + value.substring(value.length() - 4) : "***"; }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private static class CostSnapshot {
        private final BigDecimal unitCost;
        private final BigDecimal costAmount;
        private final String snapshotJson;
        private CostSnapshot(BigDecimal unitCost, BigDecimal costAmount, String snapshotJson) { this.unitCost = unitCost; this.costAmount = costAmount; this.snapshotJson = snapshotJson; }
    }
}
