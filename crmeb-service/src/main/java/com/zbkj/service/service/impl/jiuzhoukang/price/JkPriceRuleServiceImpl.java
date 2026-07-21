package com.zbkj.service.service.impl.jiuzhoukang.price;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.model.jiuzhoukang.JkProductPriceRule;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkPriceRuleSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkPriceRuleSearchRequest;
import com.zbkj.common.request.jiuzhoukang.JkPriceRuleStatusRequest;
import com.zbkj.common.response.jiuzhoukang.JkPriceRuleResponse;
import com.zbkj.service.dao.jiuzhoukang.JkProductPriceRuleDao;
import com.zbkj.service.service.jiuzhoukang.price.JkPriceRuleService;
import com.zbkj.service.service.jiuzhoukang.support.JkDisplayEnrichmentSupport;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JkPriceRuleServiceImpl extends ServiceImpl<JkProductPriceRuleDao, JkProductPriceRule> implements JkPriceRuleService {
    @Autowired
    private JkDisplayEnrichmentSupport displayEnrichmentSupport;

    @Override
    public List<JkPriceRuleResponse> getAdminList(JkPriceRuleSearchRequest request, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<JkProductPriceRule> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkProductPriceRule::getIsDeleted, false);
        if (request != null && request.getProductId() != null) {
            lqw.eq(JkProductPriceRule::getProductId, request.getProductId());
        }
        if (request != null && StrUtil.isNotBlank(request.getRoleCode())) {
            lqw.eq(JkProductPriceRule::getRoleCode, request.getRoleCode());
        }
        if (request != null && StrUtil.isNotBlank(request.getRegionCode())) {
            lqw.eq(JkProductPriceRule::getRegionCode, request.getRegionCode());
        }
        if (request != null && request.getUserId() != null) {
            lqw.eq(JkProductPriceRule::getUserId, request.getUserId());
        }
        if (request != null && request.getStatus() != null) {
            lqw.eq(JkProductPriceRule::getStatus, request.getStatus());
        }
        lqw.orderByDesc(JkProductPriceRule::getId);
        List<JkPriceRuleResponse> responses = list(lqw).stream().map(this::toResponse).collect(Collectors.toList());
        displayEnrichmentSupport.enrichPriceRules(responses);
        return responses;
    }

    @Override
    public JkPriceRuleResponse saveRule(JkPriceRuleSaveRequest request) {
        Date now = DateUtil.date();
        JkProductPriceRule entity = request.getId() == null ? new JkProductPriceRule() : getById(request.getId());
        if (entity == null) {
            entity = new JkProductPriceRule();
        }
        BeanUtils.copyProperties(request, entity);
        if (entity.getId() == null) {
            entity.setRuleNo("PR" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            entity.setStatus(true);
            entity.setIsDeleted(false);
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            entity.setCreateUserId(0L);
            entity.setUpdateUserId(0L);
            entity.setVersion(entity.getVersion() == null ? 0 : entity.getVersion());
            entity.setTenantId("000000");
            save(entity);
        } else {
            entity.setUpdateTime(now);
            entity.setUpdateUserId(0L);
            updateById(entity);
        }
        JkPriceRuleResponse response = toResponse(getById(entity.getId()));
        displayEnrichmentSupport.enrichPriceRules(java.util.Collections.singletonList(response));
        return response;
    }

    @Override
    public Boolean updateRuleStatus(JkPriceRuleStatusRequest request) {
        JkProductPriceRule entity = getById(request.getId());
        if (entity == null) {
            return false;
        }
        entity.setStatus(request.getStatus());
        entity.setUpdateTime(DateUtil.date());
        entity.setUpdateUserId(0L);
        return updateById(entity);
    }

    @Override
    public List<JkProductPriceRule> listActiveRules(Integer productId, Integer skuId) {
        LambdaQueryWrapper<JkProductPriceRule> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkProductPriceRule::getIsDeleted, false);
        lqw.eq(JkProductPriceRule::getStatus, true);
        lqw.eq(JkProductPriceRule::getProductId, productId);
        lqw.and(wrapper -> wrapper.eq(JkProductPriceRule::getSkuId, skuId).or().isNull(JkProductPriceRule::getSkuId));
        lqw.orderByDesc(JkProductPriceRule::getRuleVersion);
        return list(lqw);
    }

    private JkPriceRuleResponse toResponse(JkProductPriceRule item) {
        JkPriceRuleResponse response = new JkPriceRuleResponse();
        BeanUtils.copyProperties(item, response);
        return response;
    }
}
