package com.zbkj.service.service.impl.jiuzhoukang.report;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.pagehelper.*;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkReportExportCreateRequest;
import com.zbkj.common.response.jiuzhoukang.*;
import com.zbkj.service.dao.StoreOrderDao;
import com.zbkj.service.dao.StoreProductDao;
import com.zbkj.service.dao.jiuzhoukang.*;
import com.zbkj.service.service.jiuzhoukang.report.JkAdvancedReportService;
import com.zbkj.service.service.jiuzhoukang.stock.StockBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 第六阶段正式报表服务。
 * <p>经营口径：完成/入库计业绩，退款和退回在发生日记负数，区域与团队使用业务快照，金额不含运费。</p>
 */
@Service
public class JkAdvancedReportServiceImpl implements JkAdvancedReportService {
    public static final String METRIC_VERSION="V1-202607";
    @Autowired private JkReportDailySummaryDao summaryDao;
    @Autowired private JkReportExportTaskDao exportDao;
    @Autowired private JkPlatformOrderDao platformOrderDao;
    @Autowired private JkStockTransferDao transferDao;
    @Autowired private JkStockTransferReturnDao returnDao;
    @Autowired private JkRetailOrderAttributionDao attributionDao;
    @Autowired private JkRetailRefundAdjustmentDao refundAdjustmentDao;
    @Autowired private StoreOrderDao storeOrderDao;
    @Autowired private JkStockBatchDao batchDao;
    @Autowired private JkStockBatchFlowDao batchFlowDao;
    @Autowired private JkStockAccountDao stockAccountDao;
    @Autowired private JkStockItemDao stockItemDao;
    @Autowired private StoreProductDao productDao;
    @Autowired private JkCommissionRecordDao commissionDao;
    @Autowired private JkCommissionReverseDao reverseDao;
    @Autowired private JkFundAccountDao fundDao;
    @Autowired private JkWithdrawApplyDao withdrawDao;
    @Autowired private JkHealthDataDao healthDataDao;
    @Autowired private JkHealthAlertRecordDao alertDao;
    @Autowired private JkUserBusinessRoleDao roleDao;
    @Autowired private StockBatchService stockBatchService;
    @Value("${jk.report.export-directory:/tmp/jk-report-export}") private String exportDirectory;

    @Override @Transactional(rollbackFor=Exception.class)
    public int aggregateDay(Date date){Date day=start(date),end=nextDay(day);summaryDao.delete(new LambdaQueryWrapper<JkReportDailySummary>().eq(JkReportDailySummary::getMetricDate,day).eq(JkReportDailySummary::getMetricVersion,METRIC_VERSION));int count=0;
        Map<String,Metric> platform=new LinkedHashMap<String,Metric>();
        for(JkPlatformOrder o:platformOrderDao.selectList(new LambdaQueryWrapper<JkPlatformOrder>().eq(JkPlatformOrder::getStatus,"STOCK_IN").ge(JkPlatformOrder::getUpdateTime,day).lt(JkPlatformOrder::getUpdateTime,end).eq(JkPlatformOrder::getIsDeleted,false)))add(platform,"PLATFORM_ORDER_INBOUND",o.getRegionCode(),o.getTotalAmount(),1);
        for(JkStockTransfer o:transferDao.selectList(new LambdaQueryWrapper<JkStockTransfer>().eq(JkStockTransfer::getStatus,"STOCK_IN").ge(JkStockTransfer::getUpdateTime,day).lt(JkStockTransfer::getUpdateTime,end).eq(JkStockTransfer::getIsDeleted,false)))add(platform,"STOCK_TRANSFER_INBOUND",o.getRegionCode(),o.getTotalAmount(),1);
        for(JkStockTransferReturn o:returnDao.selectList(new LambdaQueryWrapper<JkStockTransferReturn>().eq(JkStockTransferReturn::getStatus,"COMPLETED").ge(JkStockTransferReturn::getUpdateTime,day).lt(JkStockTransferReturn::getUpdateTime,end).eq(JkStockTransferReturn::getIsDeleted,false)))add(platform,"STOCK_TRANSFER_RETURN",o.getRegionCode(),nvl(o.getReturnAmount()).negate(),1);
        List<StoreOrder> orders=storeOrderDao.selectList(new LambdaQueryWrapper<StoreOrder>().eq(StoreOrder::getStatus,3).ge(StoreOrder::getUpdateTime,day).lt(StoreOrder::getUpdateTime,end));
        Set<Long> ids=new HashSet<Long>();for(StoreOrder o:orders)ids.add(Long.valueOf(o.getId()));
        // 金额按明细快照汇总，但 count 按业务订单计一次，避免多商品订单被重复计数。
        Map<String,Metric> retailOrders=new LinkedHashMap<String,Metric>();
        if(!ids.isEmpty())for(JkRetailOrderAttribution a:attributionDao.selectList(new LambdaQueryWrapper<JkRetailOrderAttribution>().in(JkRetailOrderAttribution::getOrderId,ids).eq(JkRetailOrderAttribution::getIsDeleted,false))){
            String key=StrUtil.blankToDefault(a.getRegionCode(),"UNKNOWN")+"|"+a.getOrderId();Metric m=retailOrders.get(key);if(m==null){m=new Metric();m.count=1;retailOrders.put(key,m);}m.amount=m.amount.add(nvl(a.getItemPaidAmount()));
        }
        for(Map.Entry<String,Metric> e:retailOrders.entrySet())add(platform,"RETAIL_COMPLETED",e.getKey().substring(0,e.getKey().indexOf('|')),e.getValue().amount,1);
        // 退款按实际发生日记负数；同一 requestNo 拆分到多明细时，count 仍只计一次退款事件。
        Map<String,Metric> refundEvents=new LinkedHashMap<String,Metric>();
        for(JkRetailRefundAdjustment adjustment:refundAdjustmentDao.selectList(new LambdaQueryWrapper<JkRetailRefundAdjustment>()
                .ge(JkRetailRefundAdjustment::getOccurredTime,day).lt(JkRetailRefundAdjustment::getOccurredTime,end)
                .eq(JkRetailRefundAdjustment::getIsDeleted,false))){
            String region=StrUtil.blankToDefault(adjustment.getRegionCode(),"UNKNOWN");String key=region+"|"+adjustment.getRequestNo();Metric m=refundEvents.get(key);if(m==null){m=new Metric();m.count=1;refundEvents.put(key,m);}m.amount=m.amount.add(nvl(adjustment.getAdjustmentAmount()).negate());
        }
        for(Map.Entry<String,Metric> e:refundEvents.entrySet())add(platform,"RETAIL_REFUND",e.getKey().substring(0,e.getKey().indexOf('|')),e.getValue().amount,1);
        addRetailNet(platform);
        for(Map.Entry<String,Metric> e:platform.entrySet()){String[] key=e.getKey().split("\\|",-1);save(day,key[0],"REGION",key[1],null,e.getValue());count++;}
        // 平台总量由区域汇总相加，避免平台和区域使用不同数据源。
        Map<String,Metric> totals=new LinkedHashMap<String,Metric>();for(Map.Entry<String,Metric> e:platform.entrySet()){String code=e.getKey().substring(0,e.getKey().indexOf('|'));Metric m=totals.get(code);if(m==null){m=new Metric();totals.put(code,m);}m.amount=m.amount.add(e.getValue().amount);m.count+=e.getValue().count;}for(Map.Entry<String,Metric> e:totals.entrySet()){save(day,e.getKey(),"PLATFORM","ALL",null,e.getValue());count++;}
        return count;}

    @Override public List<JkReportMetricResponse> trend(String metricCode,Date startDate,Date endDate,String dimensionType,String dimensionCode){LambdaQueryWrapper<JkReportDailySummary> q=new LambdaQueryWrapper<JkReportDailySummary>().eq(JkReportDailySummary::getMetricCode,metricCode).eq(JkReportDailySummary::getMetricVersion,METRIC_VERSION).eq(JkReportDailySummary::getIsDeleted,false).ge(startDate!=null,JkReportDailySummary::getMetricDate,start(startDate)).le(endDate!=null,JkReportDailySummary::getMetricDate,start(endDate)).eq(StrUtil.isNotBlank(dimensionType),JkReportDailySummary::getDimensionType,dimensionType).eq(StrUtil.isNotBlank(dimensionCode),JkReportDailySummary::getDimensionCode,dimensionCode).orderByAsc(JkReportDailySummary::getMetricDate);List<JkReportMetricResponse> out=new ArrayList<JkReportMetricResponse>();for(JkReportDailySummary s:summaryDao.selectList(q))out.add(toMetric(s));return out;}

    @Override public List<JkReportMetricResponse> regionPerformance(Date startDate,Date endDate){Map<String,Metric> map=new LinkedHashMap<String,Metric>();for(JkReportDailySummary s:summaryDao.selectList(new LambdaQueryWrapper<JkReportDailySummary>().eq(JkReportDailySummary::getDimensionType,"REGION").eq(JkReportDailySummary::getMetricVersion,METRIC_VERSION).ge(startDate!=null,JkReportDailySummary::getMetricDate,start(startDate)).le(endDate!=null,JkReportDailySummary::getMetricDate,start(endDate)).eq(JkReportDailySummary::getIsDeleted,false))){String k=s.getDimensionCode()+"|"+s.getMetricCode();Metric m=map.get(k);if(m==null){m=new Metric();map.put(k,m);}m.amount=m.amount.add(nvl(s.getMetricAmount()));m.count+=s.getMetricCount()==null?0:s.getMetricCount();}List<JkReportMetricResponse> out=new ArrayList<JkReportMetricResponse>();for(Map.Entry<String,Metric> e:map.entrySet()){String[] k=e.getKey().split("\\|",-1);out.add(new JkReportMetricResponse().setDimensionType("REGION").setDimensionCode(k[0]).setMetricCode(k[1]).setAmount(e.getValue().amount).setCount(e.getValue().count));}return out;}

    @Override
    public List<JkReportMetricResponse> teamPerformance(Date startDate, Date endDate, Long rootUserId) {
        Date start = startDate == null ? new Date(0) : start(startDate);
        Date end = endDate == null ? new Date() : nextDay(start(endDate));
        List<StoreOrder> orders = storeOrderDao.selectList(new LambdaQueryWrapper<StoreOrder>()
                .eq(StoreOrder::getStatus, 3).ge(StoreOrder::getUpdateTime, start).lt(StoreOrder::getUpdateTime, end));
        Set<Long> orderIds = new HashSet<Long>();
        for (StoreOrder order : orders) orderIds.add(Long.valueOf(order.getId()));

        Map<String,Metric> completedByOrder = new LinkedHashMap<String,Metric>();
        if (!orderIds.isEmpty()) {
            for (JkRetailOrderAttribution attribution : attributionDao.selectList(new LambdaQueryWrapper<JkRetailOrderAttribution>()
                    .in(JkRetailOrderAttribution::getOrderId, orderIds)
                    .eq(rootUserId != null, JkRetailOrderAttribution::getReceiverUserId, rootUserId)
                    .isNotNull(JkRetailOrderAttribution::getReceiverUserId).eq(JkRetailOrderAttribution::getIsDeleted, false))) {
                String key = attribution.getReceiverUserId() + "|" + attribution.getOrderId();
                Metric metric = completedByOrder.get(key);
                if (metric == null) { metric = new Metric(); metric.count = 1; completedByOrder.put(key, metric); }
                metric.amount = metric.amount.add(nvl(attribution.getItemPaidAmount()));
            }
        }

        Map<String,Metric> refundByRequest = new LinkedHashMap<String,Metric>();
        for (JkRetailRefundAdjustment adjustment : refundAdjustmentDao.selectList(new LambdaQueryWrapper<JkRetailRefundAdjustment>()
                .ge(JkRetailRefundAdjustment::getOccurredTime, start).lt(JkRetailRefundAdjustment::getOccurredTime, end)
                .eq(rootUserId != null, JkRetailRefundAdjustment::getReceiverUserId, rootUserId)
                .isNotNull(JkRetailRefundAdjustment::getReceiverUserId).eq(JkRetailRefundAdjustment::getIsDeleted, false))) {
            String key = adjustment.getReceiverUserId() + "|" + adjustment.getRequestNo();
            Metric metric = refundByRequest.get(key);
            if (metric == null) { metric = new Metric(); metric.count = 1; refundByRequest.put(key, metric); }
            metric.amount = metric.amount.add(nvl(adjustment.getAdjustmentAmount()).negate());
        }

        Map<Long,Metric> completedByUser = collapseUserMetrics(completedByOrder);
        Map<Long,Metric> refundByUser = collapseUserMetrics(refundByRequest);
        List<JkReportMetricResponse> output = new ArrayList<JkReportMetricResponse>();
        for (Map.Entry<Long,Metric> entry : completedByUser.entrySet())
            output.add(new JkReportMetricResponse().setMetricCode("TEAM_RETAIL_COMPLETED").setDimensionType("TEAM_ROOT")
                    .setDimensionId(entry.getKey()).setDimensionCode(String.valueOf(entry.getKey()))
                    .setAmount(entry.getValue().amount).setCount(entry.getValue().count));
        for (Map.Entry<Long,Metric> entry : refundByUser.entrySet())
            output.add(new JkReportMetricResponse().setMetricCode("TEAM_RETAIL_REFUND").setDimensionType("TEAM_ROOT")
                    .setDimensionId(entry.getKey()).setDimensionCode(String.valueOf(entry.getKey()))
                    .setAmount(entry.getValue().amount).setCount(entry.getValue().count));
        return output;
    }

    @Override public List<JkInventoryAgingResponse> inventoryAging(String regionCode,int warnDays,int seriousDays){Map<Long,JkStockAccount> accounts=new HashMap<Long,JkStockAccount>();for(JkStockAccount a:stockAccountDao.selectList(new LambdaQueryWrapper<JkStockAccount>().eq(StrUtil.isNotBlank(regionCode),JkStockAccount::getRegionCode,regionCode).eq(JkStockAccount::getIsDeleted,false)))accounts.put(a.getId(),a);if(accounts.isEmpty())return Collections.emptyList();Map<String,JkInventoryAgingResponse> map=new LinkedHashMap<String,JkInventoryAgingResponse>();List<JkStockBatch> batches=batchDao.selectList(new LambdaQueryWrapper<JkStockBatch>().in(JkStockBatch::getStockAccountId,accounts.keySet()).gt(JkStockBatch::getAvailableQty,0).eq(JkStockBatch::getIsDeleted,false));for(JkStockBatch b:batches){String k=b.getStockAccountId()+"|"+b.getProductId()+"|"+(b.getSkuId()==null?0:b.getSkuId());JkInventoryAgingResponse r=map.get(k);if(r==null){JkStockAccount a=accounts.get(b.getStockAccountId());StoreProduct product=productDao.selectById(b.getProductId());r=new JkInventoryAgingResponse().setStockAccountId(b.getStockAccountId()).setAccountName(a==null?null:a.getOwnerName()).setRegionCode(a==null?null:a.getRegionCode()).setProductId(b.getProductId()).setSkuId(b.getSkuId()).setProductName(product==null?null:product.getStoreName()).setAvailableQty(0).setMaxAgeDays(0).setInventoryCost(BigDecimal.ZERO);map.put(k,r);}int age=stockBatchService.ageDays(b.getInboundTime());r.setAvailableQty(r.getAvailableQty()+nvlInt(b.getAvailableQty())).setMaxAgeDays(Math.max(r.getMaxAgeDays(),age)).setInventoryCost(r.getInventoryCost().add(nvl(b.getUnitCost()).multiply(BigDecimal.valueOf(nvlInt(b.getAvailableQty())))));}
        for(JkInventoryAgingResponse r:map.values()){int age=r.getMaxAgeDays();r.setAgingLevel(age>=seriousDays?"SERIOUS":age>=warnDays?"WARNING":age>=30?"ATTENTION":"NORMAL");Date last=lastOutbound(r.getStockAccountId(),r.getProductId(),r.getSkuId());r.setNoOutboundDays(last==null?r.getMaxAgeDays():(int)((System.currentTimeMillis()-last.getTime())/(86400000L)));}return new ArrayList<JkInventoryAgingResponse>(map.values());}

    @Override
    public List<JkStockBatchReconcileResponse> stockReconcile(String regionCode, boolean onlyMismatch) {
        List<JkStockAccount> accountRows = stockAccountDao.selectList(new LambdaQueryWrapper<JkStockAccount>()
                .eq(StrUtil.isNotBlank(regionCode), JkStockAccount::getRegionCode, regionCode)
                .eq(JkStockAccount::getIsDeleted, false));
        if (accountRows.isEmpty()) return Collections.emptyList();
        Map<Long,JkStockAccount> accounts = new HashMap<Long,JkStockAccount>();
        for (JkStockAccount account : accountRows) accounts.put(account.getId(), account);
        List<JkStockItem> items = stockItemDao.selectList(new LambdaQueryWrapper<JkStockItem>()
                .in(JkStockItem::getStockAccountId, accounts.keySet()).eq(JkStockItem::getIsDeleted, false)
                .orderByAsc(JkStockItem::getStockAccountId).orderByAsc(JkStockItem::getProductId));
        Map<String,int[]> batchTotals = new HashMap<String,int[]>();
        for (JkStockBatch batch : batchDao.selectList(new LambdaQueryWrapper<JkStockBatch>()
                .in(JkStockBatch::getStockAccountId, accounts.keySet()).eq(JkStockBatch::getIsDeleted, false))) {
            String key = stockKey(batch.getStockAccountId(), batch.getProductId(), batch.getSkuId());
            int[] total = batchTotals.get(key); if (total == null) { total = new int[2]; batchTotals.put(key, total); }
            total[0] += nvlInt(batch.getAvailableQty()); total[1] += nvlInt(batch.getFrozenQty());
        }
        List<JkStockBatchReconcileResponse> output = new ArrayList<JkStockBatchReconcileResponse>();
        for (JkStockItem item : items) {
            int[] batch = batchTotals.get(stockKey(item.getStockAccountId(), item.getProductId(), item.getSkuId()));
            int batchAvailable = batch == null ? 0 : batch[0], batchFrozen = batch == null ? 0 : batch[1];
            int totalAvailable = nvlInt(item.getAvailableQty()), totalFrozen = nvlInt(item.getFrozenQty());
            int availableDiff = totalAvailable - batchAvailable, frozenDiff = totalFrozen - batchFrozen;
            boolean balanced = availableDiff == 0 && frozenDiff == 0;
            if (onlyMismatch && balanced) continue;
            JkStockAccount account = accounts.get(item.getStockAccountId());
            StoreProduct product = productDao.selectById(item.getProductId());
            output.add(new JkStockBatchReconcileResponse().setStockItemId(item.getId()).setStockAccountId(item.getStockAccountId())
                    .setAccountName(account == null ? null : account.getOwnerName()).setRegionCode(account == null ? null : account.getRegionCode())
                    .setProductId(item.getProductId()).setSkuId(item.getSkuId()).setProductName(product == null ? null : product.getStoreName())
                    .setTotalAvailableQty(totalAvailable).setTotalFrozenQty(totalFrozen).setBatchAvailableQty(batchAvailable)
                    .setBatchFrozenQty(batchFrozen).setAvailableDifference(availableDiff).setFrozenDifference(frozenDiff)
                    .setReconcileStatus(balanced ? "BALANCED" : "MISMATCH"));
        }
        return output;
    }

    @Override public JkFinanceReconcileResponse financeReconcile(Date startDate,Date endDate){Date start=startDate==null?new Date(0):start(startDate),end=endDate==null?new Date():nextDay(start(endDate));BigDecimal generated=sumCommission(start,end),reversed=sumReverse(start,end),settled=BigDecimal.ZERO;for(JkCommissionRecord r:commissionDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>().ge(JkCommissionRecord::getUpdateTime,start).lt(JkCommissionRecord::getUpdateTime,end).eq(JkCommissionRecord::getIsDeleted,false)))settled=settled.add(nvl(r.getSettledAmount()));BigDecimal av=BigDecimal.ZERO,wi=BigDecimal.ZERO,wd=BigDecimal.ZERO;for(JkFundAccount f:fundDao.selectList(new LambdaQueryWrapper<JkFundAccount>().eq(JkFundAccount::getIsDeleted,false))){av=av.add(nvl(f.getAvailableAmount()));wi=wi.add(nvl(f.getWithdrawingAmount()));wd=wd.add(nvl(f.getWithdrawnAmount()));}BigDecimal submitted=BigDecimal.ZERO,paid=BigDecimal.ZERO;for(JkWithdrawApply w:withdrawDao.selectList(new LambdaQueryWrapper<JkWithdrawApply>().ge(JkWithdrawApply::getCreateTime,start).lt(JkWithdrawApply::getCreateTime,end).eq(JkWithdrawApply::getIsDeleted,false))){submitted=submitted.add(nvl(w.getAmount()));if("PAID".equals(w.getStatus()))paid=paid.add(nvl(w.getAmount()));}BigDecimal diff=settled.subtract(reversed).subtract(av.add(wi).add(wd));return new JkFinanceReconcileResponse().setCommissionGenerated(generated).setCommissionReversed(reversed).setCommissionSettled(settled).setFundAvailable(av).setFundWithdrawing(wi).setFundWithdrawn(wd).setWithdrawSubmitted(submitted).setWithdrawPaid(paid).setDifferenceAmount(diff).setReconcileStatus(diff.compareTo(BigDecimal.ZERO)==0?"BALANCED":"MISMATCH");}

    @Override public List<JkHealthAnonymousSummaryResponse> healthAnonymous(Date startDate,Date endDate,String regionCode,int minSample){Date start=startDate==null?new Date(0):start(startDate),end=endDate==null?new Date():nextDay(start(endDate));Map<Long,String> regionByUser=new HashMap<Long,String>();for(JkUserBusinessRole r:roleDao.selectList(new LambdaQueryWrapper<JkUserBusinessRole>().eq(JkUserBusinessRole::getAuditStatus,"EFFECTIVE").eq(JkUserBusinessRole::getEffectiveStatus,"ENABLED").eq(JkUserBusinessRole::getIsDeleted,false)))if(!regionByUser.containsKey(r.getUserId())&&StrUtil.isNotBlank(r.getRegionCode()))regionByUser.put(r.getUserId(),r.getRegionCode());Map<String,HealthAgg> map=new LinkedHashMap<String,HealthAgg>();for(JkHealthData d:healthDataDao.selectList(new LambdaQueryWrapper<JkHealthData>().ge(JkHealthData::getMeasuredAt,start).lt(JkHealthData::getMeasuredAt,end).eq(JkHealthData::getStatus,"VALID").eq(JkHealthData::getIsDeleted,false))){String region=regionByUser.get(d.getUserId());if(StrUtil.isNotBlank(regionCode)&&!regionCode.equals(region))continue;String key=(region==null?"UNKNOWN":region)+"|"+d.getDataType();HealthAgg a=map.get(key);if(a==null){a=new HealthAgg();map.put(key,a);}a.users.add(d.getUserId());a.count++;if(d.getNumericValue()!=null){a.sum=a.sum.add(d.getNumericValue());a.numericCount++;}}Map<String,Long>alerts=new HashMap<String,Long>();for(JkHealthAlertRecord a:alertDao.selectList(new LambdaQueryWrapper<JkHealthAlertRecord>().ge(JkHealthAlertRecord::getCreateTime,start).lt(JkHealthAlertRecord::getCreateTime,end).eq(JkHealthAlertRecord::getIsDeleted,false))){String region=regionByUser.get(a.getUserId());String key=(region==null?"UNKNOWN":region)+"|"+a.getDataType();alerts.put(key,alerts.containsKey(key)?alerts.get(key)+1:1L);}List<JkHealthAnonymousSummaryResponse> out=new ArrayList<JkHealthAnonymousSummaryResponse>();int threshold=Math.max(10,minSample);for(Map.Entry<String,HealthAgg> e:map.entrySet()){String[] k=e.getKey().split("\\|",-1);HealthAgg a=e.getValue();boolean hidden=a.users.size()<threshold;out.add(new JkHealthAnonymousSummaryResponse().setRegionCode(k[0]).setDataType(k[1]).setUserCount((long)a.users.size()).setRecordCount(hidden?null:a.count).setAverageValue(hidden||a.numericCount==0?null:a.sum.divide(BigDecimal.valueOf(a.numericCount),2,java.math.RoundingMode.HALF_UP)).setAlertCount(hidden?null:alerts.get(e.getKey())).setSuppressed(hidden).setMinimumSampleSize(threshold));}return out;}

    @Override @Transactional(rollbackFor=Exception.class) public JkReportExportTask createExport(Long operator,JkReportExportCreateRequest r){Date now=new Date();JkReportExportTask t=new JkReportExportTask().setTaskNo("REX"+IdWorker.getIdStr()).setReportType(r.getReportType().toUpperCase()).setRequestJson(JSON.toJSONString(r)).setStatus("PENDING").setProgress(0).setRequestUserId(operator).setIsDeleted(false).setCreateTime(now).setUpdateTime(now);Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_MONTH,7);t.setExpireTime(c.getTime());exportDao.insert(t);return t;}
    @Override public PageInfo<JkReportExportTask> exportTasks(Long operator,String status,PageParamRequest pageParam){Page<JkReportExportTask> page=PageHelper.startPage(pageParam.getPage(),pageParam.getLimit());LambdaQueryWrapper<JkReportExportTask> q=new LambdaQueryWrapper<JkReportExportTask>().eq(JkReportExportTask::getRequestUserId,operator).eq(JkReportExportTask::getIsDeleted,false).eq(StrUtil.isNotBlank(status),JkReportExportTask::getStatus,status).orderByDesc(JkReportExportTask::getId);return CommonPage.copyPageInfo(page,exportDao.selectList(q));}
    @Override public int runPendingExports(int limit){List<JkReportExportTask> rows=exportDao.selectList(new LambdaQueryWrapper<JkReportExportTask>().eq(JkReportExportTask::getStatus,"PENDING").eq(JkReportExportTask::getIsDeleted,false).orderByAsc(JkReportExportTask::getId).last("limit "+Math.max(1,Math.min(limit,20))));int success=0;for(JkReportExportTask t:rows){int claimed=exportDao.update(null,new UpdateWrapper<JkReportExportTask>().eq("id",t.getId()).eq("status","PENDING").set("status","PROCESSING").set("progress",10).set("update_time",new Date()));if(claimed!=1)continue;try{writeExport(t);success++;}catch(Exception ex){t.setStatus("FAILED").setErrorMessage(limit(ex.getMessage(),500)).setUpdateTime(new Date());exportDao.updateById(t);}}return success;}
    @Override public JkReportExportTask getExport(Long id,Long operator,boolean all){JkReportExportTask t=exportDao.selectById(id);if(t==null||Boolean.TRUE.equals(t.getIsDeleted())||(!all&&!operator.equals(t.getRequestUserId())))throw new CrmebException("导出任务不存在或无权访问");return t;}

    /**
     * 异步导出只生成服务端 CSV 文件，Controller 再进行鉴权下载。
     * try-with-resources 保证任何报表异常都不会遗留未关闭的文件句柄。
     */
    private void writeExport(JkReportExportTask task) throws Exception {
        JkReportExportCreateRequest request = JSON.parseObject(task.getRequestJson(), JkReportExportCreateRequest.class);
        File directory = new File(exportDirectory);
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("无法创建导出目录");
        File file = new File(directory, task.getTaskNo() + ".csv");
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            String type = task.getReportType();
            if ("REGION".equals(type)) {
                writer.write("区域,指标,金额,数量\n");
                for (JkReportMetricResponse row : regionPerformance(request.getStartDate(), request.getEndDate()))
                    writer.write(csv(row.getDimensionCode()) + "," + csv(row.getMetricCode()) + "," + row.getAmount() + "," + row.getCount() + "\n");
            } else if ("INVENTORY_AGING".equals(type)) {
                writer.write("账户,区域,商品ID,SKU_ID,可用量,最大库龄,无出库天数,等级,库存成本\n");
                for (JkInventoryAgingResponse row : inventoryAging(request.getRegionCode(), 60, 90))
                    writer.write(csv(row.getAccountName()) + "," + csv(row.getRegionCode()) + "," + row.getProductId() + "," + row.getSkuId() + "," + row.getAvailableQty() + "," + row.getMaxAgeDays() + "," + row.getNoOutboundDays() + "," + row.getAgingLevel() + "," + row.getInventoryCost() + "\n");
            } else if ("FINANCE".equals(type)) {
                JkFinanceReconcileResponse row = financeReconcile(request.getStartDate(), request.getEndDate());
                writer.write("生成佣金,冲正佣金,已结算,可提现,提现中,已提现,差异,状态\n");
                writer.write(row.getCommissionGenerated() + "," + row.getCommissionReversed() + "," + row.getCommissionSettled() + "," + row.getFundAvailable() + "," + row.getFundWithdrawing() + "," + row.getFundWithdrawn() + "," + row.getDifferenceAmount() + "," + row.getReconcileStatus() + "\n");
            } else if ("HEALTH_ANONYMOUS".equals(type)) {
                writer.write("区域,数据类型,用户数,记录数,均值,预警数,是否抑制\n");
                for (JkHealthAnonymousSummaryResponse row : healthAnonymous(request.getStartDate(), request.getEndDate(), request.getRegionCode(), 10))
                    writer.write(csv(row.getRegionCode()) + "," + csv(row.getDataType()) + "," + row.getUserCount() + "," + row.getRecordCount() + "," + row.getAverageValue() + "," + row.getAlertCount() + "," + row.getSuppressed() + "\n");
            } else {
                throw new CrmebException("不支持的报表类型");
            }
        }
        task.setStatus("SUCCESS").setProgress(100).setFileName(file.getName()).setFilePath(file.getAbsolutePath())
                .setFileSize(file.length()).setErrorMessage(null).setUpdateTime(new Date());
        exportDao.updateById(task);
    }
    private void save(Date day,String code,String dim,String dimCode,Long dimId,Metric m){summaryDao.insert(new JkReportDailySummary().setMetricDate(day).setMetricCode(code).setDimensionType(dim).setDimensionCode(StrUtil.blankToDefault(dimCode,"UNKNOWN")).setDimensionId(dimId).setMetricAmount(m.amount).setMetricCount(m.count).setMetricVersion(METRIC_VERSION).setSnapshotJson("{}").setIsDeleted(false).setCreateTime(new Date()).setUpdateTime(new Date()));}
    private JkReportMetricResponse toMetric(JkReportDailySummary s){return new JkReportMetricResponse().setMetricDate(s.getMetricDate()).setMetricCode(s.getMetricCode()).setDimensionType(s.getDimensionType()).setDimensionCode(s.getDimensionCode()).setDimensionId(s.getDimensionId()).setAmount(s.getMetricAmount()).setCount(s.getMetricCount());}
    private void add(Map<String,Metric> map,String code,String region,BigDecimal amount,long count){String key=code+"|"+StrUtil.blankToDefault(region,"UNKNOWN");Metric m=map.get(key);if(m==null){m=new Metric();map.put(key,m);}m.amount=m.amount.add(nvl(amount));m.count+=count;}
    private Date lastOutbound(Long account,Integer product,Integer sku){LambdaQueryWrapper<JkStockBatchFlow> q=new LambdaQueryWrapper<JkStockBatchFlow>().eq(JkStockBatchFlow::getStockAccountId,account).eq(JkStockBatchFlow::getProductId,product).eq(JkStockBatchFlow::getFlowType,"OUTBOUND").orderByDesc(JkStockBatchFlow::getCreateTime).last("limit 1");if(sku==null)q.isNull(JkStockBatchFlow::getSkuId);else q.eq(JkStockBatchFlow::getSkuId,sku);JkStockBatchFlow f=batchFlowDao.selectOne(q);return f==null?null:f.getCreateTime();}
    /** 为每个区域生成净零售指标：完成销售额 + 当期退款负数。 */
    private void addRetailNet(Map<String,Metric> map){Set<String> regions=new HashSet<String>();for(String key:map.keySet()){if(key.startsWith("RETAIL_COMPLETED|")||key.startsWith("RETAIL_REFUND|"))regions.add(key.substring(key.indexOf('|')+1));}for(String region:regions){Metric gross=map.get("RETAIL_COMPLETED|"+region),refund=map.get("RETAIL_REFUND|"+region),net=new Metric();if(gross!=null){net.amount=net.amount.add(gross.amount);net.count+=gross.count;}if(refund!=null)net.amount=net.amount.add(refund.amount);map.put("RETAIL_NET|"+region,net);}}
    private String stockKey(Long accountId,Integer productId,Integer skuId){return accountId+"|"+productId+"|"+(skuId==null?0:skuId);}
    private Map<Long,Metric> collapseUserMetrics(Map<String,Metric> source){Map<Long,Metric> result=new LinkedHashMap<Long,Metric>();for(Map.Entry<String,Metric> entry:source.entrySet()){Long userId=Long.valueOf(entry.getKey().substring(0,entry.getKey().indexOf('|')));Metric target=result.get(userId);if(target==null){target=new Metric();result.put(userId,target);}target.amount=target.amount.add(entry.getValue().amount);target.count+=entry.getValue().count;}return result;}
    private BigDecimal sumCommission(Date s,Date e){BigDecimal v=BigDecimal.ZERO;for(JkCommissionRecord r:commissionDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>().ge(JkCommissionRecord::getCreateTime,s).lt(JkCommissionRecord::getCreateTime,e).eq(JkCommissionRecord::getIsDeleted,false)))v=v.add(nvl(r.getCommissionAmount()));return v;}
    private BigDecimal sumReverse(Date s,Date e){BigDecimal v=BigDecimal.ZERO;for(JkCommissionReverse r:reverseDao.selectList(new LambdaQueryWrapper<JkCommissionReverse>().ge(JkCommissionReverse::getCreateTime,s).lt(JkCommissionReverse::getCreateTime,e)))v=v.add(nvl(r.getReverseAmount()));return v;}
    private Date start(Date d){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));c.setTime(d==null?new Date():d);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTime();}
    private Date nextDay(Date d){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));c.setTime(d);c.add(Calendar.DAY_OF_MONTH,1);return c.getTime();}
    private BigDecimal nvl(BigDecimal v){return v==null?BigDecimal.ZERO:v;}private int nvlInt(Integer v){return v==null?0:v;}private String csv(Object v){String s=v==null?"":String.valueOf(v);return '"'+s.replace("\"","\"\"")+'"';}private String limit(String s,int max){if(s==null)return "未知错误";return s.length()>max?s.substring(0,max):s;}
    private static class Metric{BigDecimal amount=BigDecimal.ZERO;long count;}private static class HealthAgg{Set<Long> users=new HashSet<Long>();long count;BigDecimal sum=BigDecimal.ZERO;long numericCount;}
}
