package com.zbkj.service.service.impl.jiuzhoukang.stock;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockFlow;
import com.zbkj.common.model.jiuzhoukang.JkStockItem;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockAccountSearchRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockFlowSearchRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockItemSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkStockAccountResponse;
import com.zbkj.common.response.jiuzhoukang.JkStockFlowResponse;
import com.zbkj.common.response.jiuzhoukang.JkStockItemResponse;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockFlowDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockItemDao;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.stock.StockAccountService;
import com.zbkj.service.service.jiuzhoukang.support.JkDisplayEnrichmentSupport;
import com.zbkj.service.service.jiuzhoukang.support.JkStockVisibilitySupport;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockAccountServiceImpl extends ServiceImpl<JkStockAccountDao, JkStockAccount> implements StockAccountService {

    @Autowired
    private JkStockItemDao stockItemDao;
    @Autowired
    private JkStockFlowDao stockFlowDao;
    @Autowired
    private JkDisplayEnrichmentSupport displayEnrichmentSupport;

    @Override
    public List<JkStockAccountResponse> getAdminAccountList(JkStockAccountSearchRequest request, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<JkStockAccount> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkStockAccount::getIsDeleted, false);
        if (request != null && StrUtil.isNotBlank(request.getAccountType())) {
            lqw.eq(JkStockAccount::getAccountType, request.getAccountType());
        }
        if (request != null && StrUtil.isNotBlank(request.getRoleCode())) {
            lqw.eq(JkStockAccount::getRoleCode, request.getRoleCode());
        }
        if (request != null && StrUtil.isNotBlank(request.getRegionCode())) {
            lqw.eq(JkStockAccount::getRegionCode, request.getRegionCode());
        }
        if (request != null && request.getOwnerUserId() != null) {
            lqw.eq(JkStockAccount::getOwnerUserId, request.getOwnerUserId());
        }
        if (request != null && request.getStatus() != null) {
            lqw.eq(JkStockAccount::getStatus, request.getStatus());
        }
        lqw.orderByDesc(JkStockAccount::getId);
        List<JkStockAccountResponse> responses = list(lqw).stream().map(this::toResponse).collect(Collectors.toList());
        displayEnrichmentSupport.enrichStockAccounts(responses);
        return responses;
    }

    @Override
    public List<JkStockItemResponse> getAdminItemList(JkStockItemSearchRequest request, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<JkStockItem> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkStockItem::getIsDeleted, false);
        if (request != null && request.getStockAccountId() != null) {
            lqw.eq(JkStockItem::getStockAccountId, request.getStockAccountId());
        }
        if (request != null && request.getProductId() != null) {
            lqw.eq(JkStockItem::getProductId, request.getProductId());
        }
        if (request != null && request.getSkuId() != null) {
            lqw.eq(JkStockItem::getSkuId, request.getSkuId());
        }
        if (request != null && StrUtil.isNotBlank(request.getSkuCode())) {
            lqw.eq(JkStockItem::getSkuCode, request.getSkuCode());
        }
        lqw.orderByDesc(JkStockItem::getId);
        List<JkStockItemResponse> responses = stockItemDao.selectList(lqw).stream().map(this::toItemResponse).collect(Collectors.toList());
        displayEnrichmentSupport.enrichStockItems(responses);
        return responses;
    }

    @Override
    public List<JkStockFlowResponse> getAdminFlowList(JkStockFlowSearchRequest request, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<JkStockFlow> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkStockFlow::getIsDeleted, false);
        if (request != null && request.getStockAccountId() != null) {
            lqw.eq(JkStockFlow::getStockAccountId, request.getStockAccountId());
        }
        if (request != null && request.getProductId() != null) {
            lqw.eq(JkStockFlow::getProductId, request.getProductId());
        }
        if (request != null && request.getSkuId() != null) {
            lqw.eq(JkStockFlow::getSkuId, request.getSkuId());
        }
        if (request != null && StrUtil.isNotBlank(request.getFlowType())) {
            lqw.eq(JkStockFlow::getFlowType, request.getFlowType());
        }
        if (request != null && StrUtil.isNotBlank(request.getBusinessNo())) {
            lqw.eq(JkStockFlow::getBusinessNo, request.getBusinessNo());
        }
        lqw.orderByDesc(JkStockFlow::getId);
        List<JkStockFlowResponse> responses = stockFlowDao.selectList(lqw).stream().map(this::toFlowResponse).collect(Collectors.toList());
        displayEnrichmentSupport.enrichStockFlows(responses);
        return responses;
    }

    @Override
    public List<JkStockVisibilitySupport.StockBucket> getVisibleBuckets(Integer productId, Integer skuId, JkUserContext context, String tradeIdentity) {
        List<JkStockVisibilitySupport.StockBucket> buckets = new ArrayList<>();
        if (JkBizConstants.ROLE_NORMAL_USER.equals(tradeIdentity)) {
            addBucket(buckets, findAccount(JkBizConstants.STOCK_ACCOUNT_RETAIL, null, null), productId, skuId, JkBizConstants.STOCK_SOURCE_RETAIL);
            return buckets;
        }
        if (JkBizConstants.ROLE_MAKER.equals(tradeIdentity) || JkBizConstants.ROLE_PARTNER.equals(tradeIdentity)) {
            addBucket(buckets, findAccount(JkBizConstants.STOCK_ACCOUNT_COUNTY_AGENT, context == null ? null : context.getBelongCountyAgentId(), null), productId, skuId, JkBizConstants.STOCK_SOURCE_COUNTY_ALLOCATABLE);
            return buckets;
        }
        if (JkBizConstants.ROLE_COUNTY_AGENT.equals(tradeIdentity)) {
            addBucket(buckets, findAccount(JkBizConstants.STOCK_ACCOUNT_PLATFORM, null, null), productId, skuId, JkBizConstants.STOCK_SOURCE_PLATFORM_ORDERABLE);
            addBucket(buckets, findAccount(JkBizConstants.STOCK_ACCOUNT_COUNTY_AGENT, context == null ? null : context.getUserId(), context == null ? null : context.getRegionCode()), productId, skuId, JkBizConstants.STOCK_SOURCE_OWN);
            return buckets;
        }
        addBucket(buckets, findAccount(JkBizConstants.STOCK_ACCOUNT_RETAIL, null, null), productId, skuId, JkBizConstants.STOCK_SOURCE_RETAIL);
        return buckets;
    }

    private void addBucket(List<JkStockVisibilitySupport.StockBucket> buckets, JkStockAccount account, Integer productId, Integer skuId, String source) {
        if (account == null) {
            buckets.add(new JkStockVisibilitySupport.StockBucket()
                    .setSource(source)
                    .setQty(0)
                    .setUnavailableReason("NO_STOCK_ACCOUNT"));
            return;
        }
        LambdaQueryWrapper<JkStockItem> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkStockItem::getIsDeleted, false);
        lqw.eq(JkStockItem::getStockAccountId, account.getId());
        lqw.eq(JkStockItem::getProductId, productId);
        if (skuId != null) {
            lqw.eq(JkStockItem::getSkuId, skuId);
        }
        lqw.last(" limit 1");
        JkStockItem item = stockItemDao.selectOne(lqw);
        if (item == null) {
            buckets.add(new JkStockVisibilitySupport.StockBucket()
                    .setSource(source)
                    .setQty(0)
                    .setStockAccountId(account.getId())
                    .setOwnerName(account.getOwnerName())
                    .setUnavailableReason("NO_STOCK_ITEM"));
            return;
        }
        buckets.add(new JkStockVisibilitySupport.StockBucket()
                .setSource(source)
                .setQty(item.getAvailableQty() == null ? 0 : item.getAvailableQty())
                .setStockAccountId(account.getId())
                .setOwnerName(account.getOwnerName()));
    }

    private JkStockAccount findAccount(String accountType, Long ownerUserId, String regionCode) {
        LambdaQueryWrapper<JkStockAccount> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkStockAccount::getIsDeleted, false);
        lqw.eq(JkStockAccount::getStatus, true);
        lqw.eq(JkStockAccount::getAccountType, accountType);
        if (ownerUserId != null) {
            lqw.eq(JkStockAccount::getOwnerUserId, ownerUserId);
        }
        if (StrUtil.isNotBlank(regionCode)) {
            lqw.eq(JkStockAccount::getRegionCode, regionCode);
        }
        lqw.last(" limit 1");
        return getOne(lqw);
    }

    private JkStockAccountResponse toResponse(JkStockAccount item) {
        JkStockAccountResponse response = new JkStockAccountResponse();
        BeanUtils.copyProperties(item, response);
        return response;
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
