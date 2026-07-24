package com.zbkj.front.controller.jiuzhoukang;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockBatch;
import com.zbkj.common.model.jiuzhoukang.JkStockFlow;
import com.zbkj.common.model.jiuzhoukang.JkStockItem;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.jiuzhoukang.JkStockFlowResponse;
import com.zbkj.common.response.jiuzhoukang.JkStockItemResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.dao.jiuzhoukang.JkStockBatchDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockFlowDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockItemDao;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.stock.StockAccountService;
import com.zbkj.service.service.jiuzhoukang.support.JkDisplayEnrichmentSupport;
import com.zbkj.service.service.jiuzhoukang.support.JkStockProductEnrichmentSupport;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/front/jk/stock")
public class JkStockController {
    @Autowired private FrontTokenComponent token;
    @Autowired private JkUserContextService contextService;
    @Autowired private StockAccountService accountService;
    @Autowired private JkStockItemDao itemDao;
    @Autowired private JkStockFlowDao flowDao;
    @Autowired private JkStockBatchDao batchDao;
    @Autowired private JkDisplayEnrichmentSupport displayEnrichmentSupport;
    @Autowired private JkStockProductEnrichmentSupport stockProductEnrichmentSupport;

    @GetMapping("/my")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_VIEW_SELF)
    public CommonResult<Map<String, Object>> my() {
        JkUserContext context = context();
        List<JkStockAccount> accounts = accounts(context.getUserId());
        List<Long> accountIds = ids(accounts);
        List<JkStockItem> items = accountIds.isEmpty() ? Collections.emptyList()
                : itemDao.selectList(new LambdaQueryWrapper<JkStockItem>()
                .in(JkStockItem::getStockAccountId, accountIds)
                .eq(JkStockItem::getIsDeleted, false)
                .orderByDesc(JkStockItem::getId));
        List<JkStockItemResponse> rows = items.stream().map(this::toItemResponse).collect(Collectors.toList());
        displayEnrichmentSupport.enrichStockItems(rows);
        stockProductEnrichmentSupport.enrich(rows);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("identity", context.getPrimaryRoleName());
        data.put("freezeReason", context.getFreezeReason());
        data.put("accounts", accounts);
        data.put("items", rows);
        data.putAll(stockProductEnrichmentSupport.summarize(rows));
        return CommonResult.success(data);
    }

    @GetMapping("/sku/{skuId}/detail")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_VIEW_SELF)
    public CommonResult<Map<String, Object>> skuDetail(@PathVariable Integer skuId,
                                                        @RequestParam(required = false) Integer productId) {
        JkUserContext context = context();
        List<Long> accountIds = ids(accounts(context.getUserId()));
        if (accountIds.isEmpty()) throw new CrmebException("当前身份尚未建立库存账户");

        LambdaQueryWrapper<JkStockItem> itemQuery = new LambdaQueryWrapper<JkStockItem>()
                .in(JkStockItem::getStockAccountId, accountIds)
                .eq(JkStockItem::getSkuId, skuId)
                .eq(JkStockItem::getIsDeleted, false)
                .orderByDesc(JkStockItem::getUpdateTime)
                .orderByDesc(JkStockItem::getId);
        if (productId != null) itemQuery.eq(JkStockItem::getProductId, productId);
        List<JkStockItem> items = itemDao.selectList(itemQuery);
        if (items.isEmpty()) throw new CrmebException("当前库存账户不存在该商品规格");

        JkStockItemResponse detail = toItemResponse(items.get(0));
        int available = 0;
        int frozen = 0;
        int totalIn = 0;
        int totalOut = 0;
        Date latestUpdate = null;
        for (JkStockItem item : items) {
            available += safe(item.getAvailableQty());
            frozen += safe(item.getFrozenQty());
            totalIn += safe(item.getTotalInQty());
            totalOut += safe(item.getTotalOutQty());
            if (item.getUpdateTime() != null && (latestUpdate == null || item.getUpdateTime().after(latestUpdate))) {
                latestUpdate = item.getUpdateTime();
            }
        }
        detail.setAvailableQty(available);
        detail.setAvailableQuantity(available);
        detail.setFrozenQty(frozen);
        detail.setTotalInQty(totalIn);
        detail.setTotalOutQty(totalOut);
        detail.setUpdateTime(latestUpdate);
        displayEnrichmentSupport.enrichStockItems(Collections.singletonList(detail));
        stockProductEnrichmentSupport.enrich(Collections.singletonList(detail));

        LambdaQueryWrapper<JkStockBatch> batchQuery = new LambdaQueryWrapper<JkStockBatch>()
                .in(JkStockBatch::getStockAccountId, accountIds)
                .eq(JkStockBatch::getSkuId, skuId)
                .eq(JkStockBatch::getIsDeleted, false)
                .orderByDesc(JkStockBatch::getInboundTime)
                .orderByDesc(JkStockBatch::getId)
                .last("limit 100");
        if (productId != null) batchQuery.eq(JkStockBatch::getProductId, productId);
        List<JkStockBatch> batches = batchDao.selectList(batchQuery);

        Date trendStart = startOfDay(-6);
        LambdaQueryWrapper<JkStockFlow> trendQuery = new LambdaQueryWrapper<JkStockFlow>()
                .in(JkStockFlow::getStockAccountId, accountIds)
                .eq(JkStockFlow::getSkuId, skuId)
                .eq(JkStockFlow::getIsDeleted, false)
                .ge(JkStockFlow::getCreateTime, trendStart)
                .orderByAsc(JkStockFlow::getCreateTime)
                .orderByAsc(JkStockFlow::getId);
        if (productId != null) trendQuery.eq(JkStockFlow::getProductId, productId);
        List<JkStockFlow> trendFlows = flowDao.selectList(trendQuery);

        List<JkStockFlowResponse> recentFlows = trendFlows.stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .limit(20)
                .map(this::toFlowResponse)
                .collect(Collectors.toList());
        displayEnrichmentSupport.enrichStockFlows(recentFlows);

        Map<String, Object> data = new LinkedHashMap<>();
        data.putAll(beanMap(detail));
        data.put("batchList", batches);
        data.put("trend", buildTrend(trendFlows, available + frozen));
        data.put("recentFlows", recentFlows);
        data.put("trendDescription", "趋势按库存入库和出库流水反推每日期末库存；冻结与释放不改变库存总量");
        return CommonResult.success(data);
    }

    @GetMapping("/flow/list")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_FLOW_VIEW)
    public CommonResult<CommonPage<JkStockFlowResponse>> flow(PageParamRequest page,
                                                               @RequestParam(required = false) Integer productId,
                                                               @RequestParam(required = false) Integer skuId,
                                                               @RequestParam(required = false) String flowType,
                                                               @RequestParam(required = false) String businessNo) {
        JkUserContext context = context();
        List<Long> accountIds = ids(accounts(context.getUserId()));
        if (accountIds.isEmpty()) return CommonResult.success(new CommonPage<JkStockFlowResponse>());

        LambdaQueryWrapper<JkStockFlow> query = new LambdaQueryWrapper<JkStockFlow>()
                .in(JkStockFlow::getStockAccountId, accountIds)
                .eq(JkStockFlow::getIsDeleted, false)
                .orderByDesc(JkStockFlow::getId);
        if (productId != null) query.eq(JkStockFlow::getProductId, productId);
        if (skuId != null) query.eq(JkStockFlow::getSkuId, skuId);
        if (flowType != null && !flowType.trim().isEmpty()) query.eq(JkStockFlow::getFlowType, flowType.trim());
        if (businessNo != null && !businessNo.trim().isEmpty()) query.like(JkStockFlow::getBusinessNo, businessNo.trim());

        PageHelper.startPage(page.getPage(), page.getLimit());
        List<JkStockFlow> list = flowDao.selectList(query);
        PageInfo<JkStockFlow> sourcePage = new PageInfo<>(list);
        List<JkStockFlowResponse> rows = list.stream().map(this::toFlowResponse).collect(Collectors.toList());
        displayEnrichmentSupport.enrichStockFlows(rows);
        return CommonResult.success(CommonPage.restPage(CommonPage.copyPageInfo(sourcePage, rows)));
    }

    private List<Map<String, Object>> buildTrend(List<JkStockFlow> flows, int currentTotal) {
        int[] net = new int[7];
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, Integer> indexByDate = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) indexByDate.put(keyFormat.format(startOfDay(i - 6)), i);
        for (JkStockFlow flow : flows) {
            Integer index = flow.getCreateTime() == null ? null : indexByDate.get(keyFormat.format(flow.getCreateTime()));
            if (index == null) continue;
            if ("INBOUND".equals(flow.getFlowType())) net[index] += safe(flow.getChangeQty());
            else if ("OUTBOUND".equals(flow.getFlowType())) net[index] -= safe(flow.getChangeQty());
        }

        int[] endQty = new int[7];
        endQty[6] = Math.max(0, currentTotal);
        for (int i = 6; i > 0; i--) endQty[i - 1] = Math.max(0, endQty[i] - net[i]);

        SimpleDateFormat labelFormat = new SimpleDateFormat("MM-dd");
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Date date = startOfDay(i - 6);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", keyFormat.format(date));
            row.put("label", labelFormat.format(date));
            row.put("value", endQty[i]);
            row.put("netChange", net[i]);
            result.add(row);
        }
        return result;
    }

    private Map<String, Object> beanMap(JkStockItemResponse detail) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", detail.getId());
        result.put("stockAccountId", detail.getStockAccountId());
        result.put("productId", detail.getProductId());
        result.put("skuId", detail.getSkuId());
        result.put("skuCode", detail.getSkuCode());
        result.put("productName", detail.getProductName());
        result.put("skuName", detail.getSkuName());
        result.put("skuText", detail.getSkuText());
        result.put("productImage", detail.getProductImage());
        result.put("unitName", detail.getUnitName());
        result.put("barCode", detail.getBarCode());
        result.put("retailPrice", detail.getRetailPrice());
        result.put("costPrice", detail.getCostPrice());
        result.put("referencePrice", detail.getReferencePrice());
        result.put("stockValue", detail.getStockValue());
        result.put("availableQty", detail.getAvailableQty());
        result.put("availableQuantity", detail.getAvailableQuantity());
        result.put("frozenQty", detail.getFrozenQty());
        result.put("totalInQty", detail.getTotalInQty());
        result.put("totalOutQty", detail.getTotalOutQty());
        result.put("createTime", detail.getCreateTime());
        result.put("updateTime", detail.getUpdateTime());
        return result;
    }

    private Date startOfDay(int offsetDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, offsetDays);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private int safe(Integer value) { return value == null ? 0 : value; }

    private JkUserContext context() {
        Integer uid = token.getUserId();
        if (uid == null) throw new CrmebException("请先登录");
        JkUserContext context = contextService.getFrontContext(Long.valueOf(uid));
        if (Boolean.TRUE.equals(context.getFreezeStatus())) throw new CrmebException(context.getFreezeReason());
        if (!(JkBizConstants.ROLE_COUNTY_AGENT.equals(context.getPrimaryRoleCode())
                || JkBizConstants.ROLE_MAKER.equals(context.getPrimaryRoleCode())
                || JkBizConstants.ROLE_PARTNER.equals(context.getPrimaryRoleCode()))) {
            throw new CrmebException("当前身份不支持库存中心");
        }
        return context;
    }

    private List<JkStockAccount> accounts(Long uid) {
        return accountService.list(new LambdaQueryWrapper<JkStockAccount>()
                .eq(JkStockAccount::getOwnerUserId, uid)
                .eq(JkStockAccount::getIsDeleted, false)
                .eq(JkStockAccount::getStatus, true));
    }

    private List<Long> ids(List<JkStockAccount> accounts) {
        List<Long> result = new ArrayList<>();
        for (JkStockAccount account : accounts) result.add(account.getId());
        return result;
    }

    private JkStockItemResponse toItemResponse(JkStockItem item) {
        JkStockItemResponse response = new JkStockItemResponse();
        BeanUtils.copyProperties(item, response);
        return response;
    }

    private JkStockFlowResponse toFlowResponse(JkStockFlow item) {
        JkStockFlowResponse response = new JkStockFlowResponse();
        BeanUtils.copyProperties(item, response);
        return response;
    }
}
