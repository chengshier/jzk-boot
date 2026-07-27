package com.zbkj.service.service.impl.jiuzhoukang.price;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkProductPriceRule;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkPriceRuleSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkPriceRuleSearchRequest;
import com.zbkj.common.request.jiuzhoukang.JkPriceRuleStatusRequest;
import com.zbkj.common.response.jiuzhoukang.JkPriceRuleResponse;
import com.zbkj.service.dao.jiuzhoukang.JkProductPriceRuleDao;
import com.zbkj.service.service.StoreProductAttrValueService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.jiuzhoukang.price.JkPriceRuleService;
import com.zbkj.service.service.jiuzhoukang.support.JkDisplayEnrichmentSupport;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JkPriceRuleServiceImpl extends ServiceImpl<JkProductPriceRuleDao, JkProductPriceRule> implements JkPriceRuleService {
    @Autowired
    private JkDisplayEnrichmentSupport displayEnrichmentSupport;
    @Autowired
    private StoreProductService productService;
    @Autowired
    private StoreProductAttrValueService skuService;

    @Override
    public List<JkPriceRuleResponse> getAdminList(JkPriceRuleSearchRequest request, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<JkProductPriceRule> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkProductPriceRule::getIsDeleted, false);
        if (request != null && request.getProductId() != null) lqw.eq(JkProductPriceRule::getProductId, request.getProductId());
        if (request != null && StrUtil.isNotBlank(request.getRoleCode())) lqw.eq(JkProductPriceRule::getRoleCode, request.getRoleCode());
        if (request != null && StrUtil.isNotBlank(request.getRegionCode())) lqw.eq(JkProductPriceRule::getRegionCode, request.getRegionCode());
        if (request != null && request.getUserId() != null) lqw.eq(JkProductPriceRule::getUserId, request.getUserId());
        if (request != null && request.getStatus() != null) lqw.eq(JkProductPriceRule::getStatus, request.getStatus());
        lqw.orderByDesc(JkProductPriceRule::getId);
        List<JkPriceRuleResponse> responses = list(lqw).stream().map(this::toResponse).collect(Collectors.toList());
        displayEnrichmentSupport.enrichPriceRules(responses);
        return responses;
    }

    @Override
    public JkPriceRuleResponse saveRule(JkPriceRuleSaveRequest request) {
        ValidatedProduct validated = validateRequest(request);
        Date now = DateUtil.date();
        JkProductPriceRule entity;
        if (request.getId() == null) {
            entity = new JkProductPriceRule();
        } else {
            entity = getById(request.getId());
            if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
                throw new CrmebException("价格规则不存在或已删除");
            }
        }

        BeanUtils.copyProperties(request, entity);
        entity.setSkuCode(validated.sku == null ? null : validated.sku.getUnique());
        normalizePriceFields(entity);
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
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
            throw new CrmebException("价格规则不存在或已删除");
        }
        if (Boolean.TRUE.equals(request.getStatus())) {
            JkPriceRuleSaveRequest validation = new JkPriceRuleSaveRequest();
            BeanUtils.copyProperties(entity, validation);
            validateRequest(validation);
        }
        entity.setStatus(request.getStatus());
        entity.setUpdateTime(DateUtil.date());
        entity.setUpdateUserId(0L);
        return updateById(entity);
    }

    @Override
    public List<JkProductPriceRule> listActiveRules(Integer productId, Integer skuId) {
        LambdaQueryWrapper<JkProductPriceRule> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkProductPriceRule::getIsDeleted, false)
                .eq(JkProductPriceRule::getStatus, true)
                .eq(JkProductPriceRule::getProductId, productId);
        if (skuId == null) {
            lqw.isNull(JkProductPriceRule::getSkuId);
        } else {
            lqw.and(wrapper -> wrapper.eq(JkProductPriceRule::getSkuId, skuId)
                    .or().isNull(JkProductPriceRule::getSkuId));
        }
        lqw.orderByDesc(JkProductPriceRule::getRuleVersion);
        return list(lqw);
    }

    private ValidatedProduct validateRequest(JkPriceRuleSaveRequest request) {
        if (request == null || request.getProductId() == null) {
            throw new CrmebException("请选择商品");
        }
        StoreProduct product = productService.getById(request.getProductId());
        if (product == null || Boolean.TRUE.equals(product.getIsDel())) {
            throw new CrmebException("所选商品不存在或已删除");
        }
        StoreProductAttrValue sku = null;
        if (request.getSkuId() != null) {
            sku = skuService.getById(request.getSkuId());
            if (sku == null || Boolean.TRUE.equals(sku.getIsDel())) {
                throw new CrmebException("所选商品规格不存在或已删除");
            }
            if (!request.getProductId().equals(sku.getProductId())) {
                throw new CrmebException("商品规格不属于所选商品");
            }
        }
        if (StrUtil.isBlank(request.getPriceType())) {
            throw new CrmebException("请选择价格类型");
        }
        if (JkBizConstants.PRICE_TYPE_FIXED.equals(request.getPriceType())) {
            if (request.getFixedPrice() == null || request.getFixedPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new CrmebException("固定价不能小于0");
            }
        } else if (JkBizConstants.PRICE_TYPE_DISCOUNT.equals(request.getPriceType())) {
            if (request.getDiscountRate() == null
                    || request.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0
                    || request.getDiscountRate().compareTo(BigDecimal.ONE) > 0) {
                throw new CrmebException("折扣率必须大于0且不大于1");
            }
        } else {
            throw new CrmebException("不支持的价格类型");
        }
        if (request.getRuleVersion() == null || request.getRuleVersion() < 1) {
            throw new CrmebException("规则版本必须大于等于1");
        }
        if (request.getEffectiveTime() != null && request.getExpireTime() != null
                && !request.getExpireTime().after(request.getEffectiveTime())) {
            throw new CrmebException("失效时间必须晚于生效时间");
        }
        return new ValidatedProduct(product, sku);
    }

    private void normalizePriceFields(JkProductPriceRule entity) {
        if (JkBizConstants.PRICE_TYPE_FIXED.equals(entity.getPriceType())) {
            entity.setDiscountRate(null);
        } else if (JkBizConstants.PRICE_TYPE_DISCOUNT.equals(entity.getPriceType())) {
            entity.setFixedPrice(null);
        }
    }

    private JkPriceRuleResponse toResponse(JkProductPriceRule item) {
        JkPriceRuleResponse response = new JkPriceRuleResponse();
        BeanUtils.copyProperties(item, response);
        return response;
    }

    private static class ValidatedProduct {
        private final StoreProduct product;
        private final StoreProductAttrValue sku;

        private ValidatedProduct(StoreProduct product, StoreProductAttrValue sku) {
            this.product = product;
            this.sku = sku;
        }
    }
}