package com.zbkj.front.controller.jiuzhoukang;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkCommissionAccount;
import com.zbkj.common.model.jiuzhoukang.JkFundAccount;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockItem;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.model.jiuzhoukang.JkTradeReceiveException;
import com.zbkj.common.response.jiuzhoukang.JkStockItemResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkFundAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferDao;
import com.zbkj.service.dao.jiuzhoukang.JkTradeReceiveExceptionDao;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.support.JkDisplayEnrichmentSupport;
import com.zbkj.service.service.jiuzhoukang.support.JkStockProductEnrichmentSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 九州康业务中心聚合接口。
 *
 * <p>统计口径由后端统一维护，App 不再拉取固定数量的业务列表自行统计。</p>
 */
@RestController
@RequestMapping("api/front/jk/business")
@Api(tags = "九州康业务中心")
public class JkBusinessSummaryController {

    @Autowired private FrontTokenComponent token;
    @Autowired private JkUserContextService contextService;
    @Autowired private JkStockAccountDao stockAccountDao;
    @Autowired private JkStockItemDao stockItemDao;
    @Autowired private JkCommissionAccountDao commissionAccountDao;
    @Autowired private JkFundAccountDao fundAccountDao;
    @Autowired private JkPlatformOrderDao platformOrderDao;
    @Autowired private JkStockTransferDao stockTransferDao;
    @Autowired private JkTradeReceiveExceptionDao receiveExceptionDao;
    @Autowired private JkDisplayEnrichmentSupport displayEnrichmentSupport;
    @Autowired private JkStockProductEnrichmentSupport stockProductEnrichmentSupport;

    @GetMapping("/summary")
    @ApiOperation("当前用户业务中心汇总")
    public CommonResult<Map<String, Object>> summary() {
        Integer frontUserId = token.getUserId();
        if (frontUserId == null || frontUserId <= 0) {
            throw new CrmebException("请先登录");
        }
        Long userId = Long.valueOf(frontUserId);
        JkUserContext context = contextService.getFrontContext(userId);
        String roleCode = context.getPrimaryRoleCode();

        List<JkStockAccount> accounts = stockAccountDao.selectList(new LambdaQueryWrapper<JkStockAccount>()
                .eq(JkStockAccount::getOwnerUserId, userId)
                .eq(JkStockAccount::getStatus, true)
                .eq(JkStockAccount::getIsDeleted, false));
        List<Long> accountIds = accounts.stream().map(JkStockAccount::getId).collect(Collectors.toList());
        List<JkStockItem> stockItems = accountIds.isEmpty() ? Collections.emptyList()
                : stockItemDao.selectList(new LambdaQueryWrapper<JkStockItem>()
                .in(JkStockItem::getStockAccountId, accountIds)
                .eq(JkStockItem::getIsDeleted, false));
        List<JkStockItemResponse> stockRows = stockItems.stream().map(this::toStockResponse).collect(Collectors.toList());
        displayEnrichmentSupport.enrichStockItems(stockRows);
        stockProductEnrichmentSupport.enrich(stockRows);

        JkCommissionAccount commissionAccount = commissionAccountDao.selectOne(
                new LambdaQueryWrapper<JkCommissionAccount>()
                        .eq(JkCommissionAccount::getUserId, userId)
                        .eq(JkCommissionAccount::getRoleCode, roleCode)
                        .eq(JkCommissionAccount::getIsDeleted, false)
                        .last("limit 1"));
        JkFundAccount fundAccount = fundAccountDao.selectOne(new LambdaQueryWrapper<JkFundAccount>()
                .eq(JkFundAccount::getUserId, userId)
                .eq(JkFundAccount::getRoleCode, roleCode)
                .eq(JkFundAccount::getIsDeleted, false)
                .last("limit 1"));

        long pendingOrderCount = countPlatformOrders(userId, Arrays.asList("CREATED", "PAYMENT_REJECTED"));
        long pendingReceiveCount = countPlatformOrders(userId, Collections.singletonList("SHIPPED"));
        long pendingTransferCount = countUserTransfers(userId, Arrays.asList(
                "SUBMITTED", "AUDIT_APPROVED", "PAYMENT_SUBMITTED", "PAYMENT_REJECTED",
                "PAYMENT_APPROVED", "TRANSFERRED"));
        long pendingAuditCount = countHandledTransfers(userId, Arrays.asList(
                "SUBMITTED", "PAYMENT_SUBMITTED", "PAYMENT_APPROVED"));
        long receiveExceptionCount = countReceiveExceptions(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("identity", context);
        result.putAll(stockProductEnrichmentSupport.summarize(stockRows));
        result.put("totalCommissionAmount", amount(commissionAccount == null ? null : commissionAccount.getTotalCommissionAmount()));
        result.put("pendingSettleAmount", amount(commissionAccount == null ? null : commissionAccount.getPendingSettleAmount()));
        result.put("settledAmount", amount(commissionAccount == null ? null : commissionAccount.getSettledAmount()));
        result.put("withdrawableAmount", amount(fundAccount == null ? null : fundAccount.getAvailableAmount()));
        result.put("pendingOrderCount", pendingOrderCount);
        result.put("pendingReceiveCount", pendingReceiveCount);
        result.put("pendingTransferCount", pendingTransferCount);
        result.put("pendingAuditCount", pendingAuditCount);
        result.put("receiveExceptionCount", receiveExceptionCount);
        result.put("pendingCount", pendingOrderCount + pendingReceiveCount + pendingTransferCount
                + pendingAuditCount + receiveExceptionCount);
        result.put("menuPermissions", context.getPermissions() == null ? Collections.emptyList() : context.getPermissions());
        return CommonResult.success(result);
    }

    private long countPlatformOrders(Long userId, List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) return 0L;
        return platformOrderDao.selectCount(new LambdaQueryWrapper<JkPlatformOrder>()
                .eq(JkPlatformOrder::getUserId, userId)
                .in(JkPlatformOrder::getStatus, statuses)
                .eq(JkPlatformOrder::getIsDeleted, false));
    }

    private long countUserTransfers(Long userId, List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) return 0L;
        return stockTransferDao.selectCount(new LambdaQueryWrapper<JkStockTransfer>()
                .eq(JkStockTransfer::getUserId, userId)
                .in(JkStockTransfer::getStatus, statuses)
                .eq(JkStockTransfer::getIsDeleted, false));
    }

    private long countHandledTransfers(Long countyAgentId, List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) return 0L;
        return stockTransferDao.selectCount(new LambdaQueryWrapper<JkStockTransfer>()
                .eq(JkStockTransfer::getCountyAgentId, countyAgentId)
                .ne(JkStockTransfer::getUserId, countyAgentId)
                .in(JkStockTransfer::getStatus, statuses)
                .eq(JkStockTransfer::getIsDeleted, false));
    }

    private long countReceiveExceptions(Long userId) {
        return receiveExceptionDao.selectCount(new LambdaQueryWrapper<JkTradeReceiveException>()
                .eq(JkTradeReceiveException::getReceiverUserId, userId)
                .in(JkTradeReceiveException::getStatus, Arrays.asList("PENDING", "PROCESSING"))
                .eq(JkTradeReceiveException::getIsDeleted, false));
    }

    private JkStockItemResponse toStockResponse(JkStockItem item) {
        JkStockItemResponse response = new JkStockItemResponse();
        BeanUtils.copyProperties(item, response);
        return response;
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
