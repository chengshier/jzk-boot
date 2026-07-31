package com.zbkj.service.service.impl.jiuzhoukang.commission;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkOfflineSale;
import com.zbkj.common.model.jiuzhoukang.JkOfflineSaleItem;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttribution;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleTrialRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionSourceTrialRequest;
import com.zbkj.service.dao.jiuzhoukang.JkOfflineSaleDao;
import com.zbkj.service.dao.jiuzhoukang.JkOfflineSaleItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkPerformanceRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderDao;
import com.zbkj.service.dao.jiuzhoukang.JkRetailOrderAttributionDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferDao;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionScenarioService;
import com.zbkj.service.service.jiuzhoukang.commission.JkCommissionSourceTrialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 运营只选择真实业务单据；服务端从已固化快照构造试算上下文。 */
@Service
public class JkCommissionSourceTrialServiceImpl implements JkCommissionSourceTrialService {
    @Autowired private CommissionScenarioService scenarioService;
    @Autowired private JkRetailOrderAttributionDao attributionDao;
    @Autowired private JkOfflineSaleDao offlineSaleDao;
    @Autowired private JkOfflineSaleItemDao offlineSaleItemDao;
    @Autowired private JkPlatformOrderDao platformOrderDao;
    @Autowired private JkStockTransferDao stockTransferDao;
    @Autowired private JkPerformanceRecordDao performanceRecordDao;

    @Override
    public Map<String, Object> trial(JkCommissionSourceTrialRequest request) {
        JkCommissionRuleTrialRequest snapshot;
        String sourceLabel;
        if ("RETAIL_ORDER".equals(request.getSourceType())) {
            snapshot = retail(request); sourceLabel = "线上零售订单归属快照";
        } else if ("OFFLINE_SALE".equals(request.getSourceType())) {
            snapshot = offline(request); sourceLabel = "线下终端销售快照";
        } else if ("PLATFORM_ORDER".equals(request.getSourceType())) {
            snapshot = platformOrder(request); sourceLabel = "平台订货快照";
        } else if ("STOCK_TRANSFER".equals(request.getSourceType())) {
            snapshot = transfer(request); sourceLabel = "库存调拨快照";
        } else if ("PERFORMANCE_PERIOD".equals(request.getSourceType())) {
            snapshot = performance(request); sourceLabel = "业绩账本快照";
        } else {
            throw new IllegalArgumentException("不支持的真实业务来源");
        }
        snapshot.setRuleId(request.getRuleId());
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("sourceLabel", sourceLabel);
        result.put("sourceSnapshot", snapshot);
        result.put("trialResults", scenarioService.trial(snapshot));
        result.put("notice", "试算只读取真实业务发生时快照，不读取当前上下级关系，不写佣金和账户。");
        return result;
    }

    private JkCommissionRuleTrialRequest retail(JkCommissionSourceTrialRequest request) {
        LambdaQueryWrapper<JkRetailOrderAttribution> query = new LambdaQueryWrapper<JkRetailOrderAttribution>()
                .eq(JkRetailOrderAttribution::getIsDeleted, false)
                .orderByAsc(JkRetailOrderAttribution::getOrderInfoId);
        if (request.getSourceItemId() != null) query.eq(JkRetailOrderAttribution::getOrderInfoId, request.getSourceItemId());
        else if (request.getSourceId() != null) query.eq(JkRetailOrderAttribution::getOrderId, request.getSourceId());
        else if (StrUtil.isNotBlank(request.getSourceNo())) query.eq(JkRetailOrderAttribution::getOrderNo, request.getSourceNo().trim());
        else throw new IllegalArgumentException("请选择真实零售订单或订单明细");
        JkRetailOrderAttribution row = attributionDao.selectOne(query.last("limit 1"));
        if (row == null) throw new IllegalArgumentException("未找到零售订单归属快照，不能手工拼装试算数据");
        return base("RETAIL_ORDER_COMPLETED", "RETAIL_ORDER", row.getOrderId(), row.getOrderInfoId(), row.getOrderNo())
                .setBuyerUserId(row.getBuyerUserId()).setSellerUserId(row.getDirectParentUserId())
                .setDirectParentUserId(row.getDirectParentUserId()).setCountyAgentUserId(row.getCountyAgentUserId())
                .setRegionCode(first(row.getFinalRegionCode(), row.getRegionCode())).setProductId(intValue(row.getProductId()))
                .setSkuId(intValue(row.getSkuId())).setQuantity(row.getQuantity())
                .setBaseAmount(money(row.getCommissionBaseAmount())).setRegisteredCustomer(true)
                .setVoucherPresent(true).setAudited(true);
    }

    private JkCommissionRuleTrialRequest offline(JkCommissionSourceTrialRequest request) {
        JkOfflineSale sale = findOfflineSale(request);
        JkOfflineSaleItem item = null;
        if (request.getSourceItemId() != null) item = offlineSaleItemDao.selectById(request.getSourceItemId());
        if (item == null) item = offlineSaleItemDao.selectOne(new LambdaQueryWrapper<JkOfflineSaleItem>()
                .eq(JkOfflineSaleItem::getSaleId, sale.getId()).eq(JkOfflineSaleItem::getIsDeleted, false)
                .orderByAsc(JkOfflineSaleItem::getId).last("limit 1"));
        BigDecimal amount = item == null ? sale.getTotalAmount() : item.getTotalAmount();
        BigDecimal profit = item == null ? null : item.getProfitAmount();
        Integer quantity = item == null ? sale.getTotalQuantity() : item.getQuantity();
        return base("OFFLINE_SALE_AUDITED", "OFFLINE_SALE", sale.getId(), item == null ? null : item.getId(), sale.getSaleNo())
                .setBuyerUserId(sale.getCustomerUserId()).setSellerUserId(sale.getSellerUserId())
                .setDirectParentUserId(sale.getDirectParentUserId()).setCountyAgentUserId(sale.getCountyAgentUserId())
                .setRegionCode(sale.getRegionCode()).setProductId(item == null ? null : item.getProductId())
                .setSkuId(item == null ? null : item.getSkuId()).setQuantity(quantity)
                .setBaseAmount(money(amount)).setRealGrossProfit(money(profit))
                .setRegisteredCustomer(Boolean.TRUE.equals(sale.getRegisteredCustomer()))
                .setVoucherPresent(StrUtil.isNotBlank(sale.getVoucherUrls()))
                .setAudited("AUDITED".equals(sale.getStatus()) || "COMPLETED".equals(sale.getStatus()));
    }

    private JkCommissionRuleTrialRequest platformOrder(JkCommissionSourceTrialRequest request) {
        JkPlatformOrder order = findPlatformOrder(request);
        return base("PLATFORM_ORDER_RECEIVED", "PLATFORM_ORDER", order.getId(), null, order.getPlatformOrderNo())
                .setBuyerUserId(order.getUserId()).setSellerUserId(order.getCountyAgentId())
                .setCountyAgentUserId(order.getCountyAgentId()).setRegionCode(order.getRegionCode())
                .setQuantity(1).setBaseAmount(money(order.getTotalAmount()))
                .setRegisteredCustomer(true).setVoucherPresent("PAID".equals(order.getPayStatus()))
                .setAudited("APPROVED".equals(order.getAuditStatus()) || "RECEIVED".equals(order.getReceiveStatus()));
    }

    private JkCommissionRuleTrialRequest transfer(JkCommissionSourceTrialRequest request) {
        JkStockTransfer transfer = findTransfer(request);
        return base("STOCK_TRANSFER_RECEIVED", "STOCK_TRANSFER", transfer.getId(), null, transfer.getTransferNo())
                .setBuyerUserId(transfer.getUserId()).setSellerUserId(transfer.getCountyAgentId())
                .setCountyAgentUserId(transfer.getCountyAgentId()).setRegionCode(transfer.getRegionCode())
                .setQuantity(1).setBaseAmount(money(transfer.getTotalAmount()))
                .setRegisteredCustomer(true).setVoucherPresent("PAID".equals(transfer.getPayStatus()))
                .setAudited("APPROVED".equals(transfer.getAuditStatus()) || "RECEIVED".equals(transfer.getReceiveStatus()));
    }

    private JkCommissionRuleTrialRequest performance(JkCommissionSourceTrialRequest request) {
        LambdaQueryWrapper<JkPerformanceRecord> query = new LambdaQueryWrapper<JkPerformanceRecord>()
                .eq(JkPerformanceRecord::getIsDeleted, false).orderByDesc(JkPerformanceRecord::getId);
        if (request.getSourceId() != null) query.eq(JkPerformanceRecord::getId, request.getSourceId());
        else if (StrUtil.isNotBlank(request.getSourceNo())) query.eq(JkPerformanceRecord::getPerformanceNo, request.getSourceNo().trim());
        else throw new IllegalArgumentException("请选择真实业绩记录");
        JkPerformanceRecord row = performanceRecordDao.selectOne(query.last("limit 1"));
        if (row == null) throw new IllegalArgumentException("业绩记录不存在");
        return base("PERFORMANCE_PERIOD_CLOSED", "PERFORMANCE_PERIOD", row.getId(), row.getSourceItemId(), row.getPerformanceNo())
                .setBuyerUserId(row.getSourceUserId()).setSellerUserId(row.getOwnerUserId())
                .setDirectParentUserId(row.getDirectParentUserId()).setCountyAgentUserId(row.getCountyAgentUserId())
                .setRegionCode(row.getRegionCode()).setProductId(row.getProductId()).setSkuId(row.getSkuId())
                .setQuantity(row.getQuantity()).setBaseAmount(money(row.getPerformanceAmount()))
                .setRegisteredCustomer(true).setVoucherPresent(true).setAudited("VALID".equals(row.getStatus()) || "SETTLED".equals(row.getStatus()));
    }

    private JkOfflineSale findOfflineSale(JkCommissionSourceTrialRequest request) {
        LambdaQueryWrapper<JkOfflineSale> query = new LambdaQueryWrapper<JkOfflineSale>().eq(JkOfflineSale::getIsDeleted, false);
        if (request.getSourceId() != null) query.eq(JkOfflineSale::getId, request.getSourceId());
        else if (StrUtil.isNotBlank(request.getSourceNo())) query.eq(JkOfflineSale::getSaleNo, request.getSourceNo().trim());
        else throw new IllegalArgumentException("请选择真实线下销售单");
        JkOfflineSale sale = offlineSaleDao.selectOne(query.last("limit 1"));
        if (sale == null) throw new IllegalArgumentException("线下销售单不存在");
        return sale;
    }

    private JkPlatformOrder findPlatformOrder(JkCommissionSourceTrialRequest request) {
        LambdaQueryWrapper<JkPlatformOrder> query = new LambdaQueryWrapper<JkPlatformOrder>().eq(JkPlatformOrder::getIsDeleted, false);
        if (request.getSourceId() != null) query.eq(JkPlatformOrder::getId, request.getSourceId());
        else if (StrUtil.isNotBlank(request.getSourceNo())) query.eq(JkPlatformOrder::getPlatformOrderNo, request.getSourceNo().trim());
        else throw new IllegalArgumentException("请选择真实平台订货单");
        JkPlatformOrder row = platformOrderDao.selectOne(query.last("limit 1"));
        if (row == null) throw new IllegalArgumentException("平台订货单不存在");
        return row;
    }

    private JkStockTransfer findTransfer(JkCommissionSourceTrialRequest request) {
        LambdaQueryWrapper<JkStockTransfer> query = new LambdaQueryWrapper<JkStockTransfer>().eq(JkStockTransfer::getIsDeleted, false);
        if (request.getSourceId() != null) query.eq(JkStockTransfer::getId, request.getSourceId());
        else if (StrUtil.isNotBlank(request.getSourceNo())) query.eq(JkStockTransfer::getTransferNo, request.getSourceNo().trim());
        else throw new IllegalArgumentException("请选择真实库存调拨单");
        JkStockTransfer row = stockTransferDao.selectOne(query.last("limit 1"));
        if (row == null) throw new IllegalArgumentException("库存调拨单不存在");
        return row;
    }

    private JkCommissionRuleTrialRequest base(String scenario, String sourceType, Long sourceId, Long sourceItemId, String sourceNo) {
        return new JkCommissionRuleTrialRequest().setScenario(scenario).setSourceType(sourceType)
                .setSourceId(sourceId).setSourceItemId(sourceItemId).setSourceNo(sourceNo);
    }

    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private Integer intValue(Long value) { return value == null ? null : value.intValue(); }
    private String first(String first, String second) { return StrUtil.isNotBlank(first) ? first : second; }
}
