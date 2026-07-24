package com.zbkj.front.controller.jiuzhoukang;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockFlow;
import com.zbkj.common.model.jiuzhoukang.JkStockItem;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.jiuzhoukang.JkStockFlowResponse;
import com.zbkj.common.response.jiuzhoukang.JkStockItemResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
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

    @GetMapping("/flow/list")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_FLOW_VIEW)
    public CommonResult<CommonPage<JkStockFlowResponse>> flow(PageParamRequest page) {
        JkUserContext context = context();
        List<Long> accountIds = ids(accounts(context.getUserId()));
        List<JkStockFlow> list = accountIds.isEmpty() ? Collections.emptyList()
                : flowDao.selectList(new LambdaQueryWrapper<JkStockFlow>()
                .in(JkStockFlow::getStockAccountId, accountIds)
                .eq(JkStockFlow::getIsDeleted, false)
                .orderByDesc(JkStockFlow::getId));
        List<JkStockFlowResponse> rows = list.stream().map(this::toFlowResponse).collect(Collectors.toList());
        displayEnrichmentSupport.enrichStockFlows(rows);
        return CommonResult.success(CommonPage.restPage(new com.github.pagehelper.PageInfo<>(rows)));
    }

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