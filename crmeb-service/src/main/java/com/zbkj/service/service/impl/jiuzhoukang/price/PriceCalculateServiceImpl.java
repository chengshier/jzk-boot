package com.zbkj.service.service.impl.jiuzhoukang.price;

import cn.hutool.core.util.StrUtil;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.JkProductPriceRule;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.model.user.User;
import com.zbkj.common.response.jiuzhoukang.JkProductTradeViewResponse;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.price.JkPriceRuleService;
import com.zbkj.service.service.jiuzhoukang.price.PriceCalculateService;
import com.zbkj.service.service.jiuzhoukang.support.JkPriceRuleSupport;
import com.zbkj.service.service.jiuzhoukang.support.JkTradeViewSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 九州康统一价格裁决服务。
 * <p>前端展示、订货和调拨均应复用本服务；优先级为指定用户、区域+角色、角色、通用规则，
 * 同一适用层级内 SKU 精确规则优先于商品级规则。最终成交价仍由后端重新计算。</p>
 */
@Service
public class PriceCalculateServiceImpl implements PriceCalculateService {

    @Autowired
    private JkPriceRuleService priceRuleService;

    @Autowired
    private UserService userService;

    @Override
    public JkProductTradeViewResponse.PriceInfo calculatePrice(StoreProduct product, StoreProductAttrValue sku, JkUserContext context) {
        String tradeIdentity = JkTradeViewSupport.resolveTradeIdentity(context);
        List<JkProductPriceRule> rules = priceRuleService.listActiveRules(product.getId(), sku == null ? null : sku.getId());
        List<JkPriceRuleSupport.RuleCandidate> candidates = rules.stream()
                .map(item -> toCandidate(item, context, tradeIdentity))
                .filter(item -> item != null)
                .collect(Collectors.toList());
        BigDecimal retailBasePrice = sku != null && sku.getPrice() != null ? sku.getPrice() : product.getPrice();
        JkPriceRuleSupport.ResolvedPrice resolvedPrice = JkPriceRuleSupport.resolvePrice(
                candidates,
                new Date(),
                resolveMemberPrice(product, context),
                retailBasePrice
        );
        JkProductTradeViewResponse.PriceInfo priceInfo = new JkProductTradeViewResponse.PriceInfo();
        priceInfo.setAmount(resolvedPrice.getAmount());
        priceInfo.setOriginalAmount(sku != null && sku.getOtPrice() != null ? sku.getOtPrice() : product.getOtPrice());
        priceInfo.setPriceType(resolvedPrice.getPriceType());
        priceInfo.setRuleId(resolvedPrice.getRuleId());
        priceInfo.setRuleVersion(resolvedPrice.getRuleVersion());
        priceInfo.setFallbackReason(resolvedPrice.getFallbackReason());
        return priceInfo;
    }

    private JkPriceRuleSupport.RuleCandidate toCandidate(JkProductPriceRule item, JkUserContext context, String tradeIdentity) {
        if (item.getUserId() != null) {
            if (context == null || context.getUserId() == null || !item.getUserId().equals(context.getUserId())) {
                return null;
            }
        } else if (StrUtil.isNotBlank(item.getRegionCode()) && StrUtil.isNotBlank(item.getRoleCode())) {
            if (context == null || !item.getRegionCode().equals(context.getRegionCode()) || !item.getRoleCode().equals(tradeIdentity)) {
                return null;
            }
        } else if (StrUtil.isNotBlank(item.getRoleCode())) {
            if (!item.getRoleCode().equals(tradeIdentity)) {
                return null;
            }
        }
        return new JkPriceRuleSupport.RuleCandidate()
                .setRuleId(item.getId())
                .setRuleVersion(item.getRuleVersion())
                .setPriceType(item.getPriceType())
                .setFixedPrice(item.getFixedPrice())
                .setDiscountRate(item.getDiscountRate())
                .setEffectiveTime(item.getEffectiveTime())
                .setExpireTime(item.getExpireTime())
                .setStatus(item.getStatus())
                .setSkuSpecific(item.getSkuId() != null)
                .setMatchLevel(resolveMatchLevel(item));
    }


    private BigDecimal resolveMemberPrice(StoreProduct product, JkUserContext context) {
        if (context == null || context.getUserId() == null || product == null) {
            return null;
        }
        User user = userService.getById(context.getUserId());
        if (user == null || user.getLevel() == null || user.getLevel() <= 0) {
            return null;
        }
        return product.getVipPrice();
    }

    private String resolveMatchLevel(JkProductPriceRule item) {
        if (item.getUserId() != null) {
            return JkBizConstants.PRICE_MATCH_LEVEL_USER;
        }
        if (StrUtil.isNotBlank(item.getRegionCode()) && StrUtil.isNotBlank(item.getRoleCode())) {
            return JkBizConstants.PRICE_MATCH_LEVEL_REGION_ROLE;
        }
        if (StrUtil.isNotBlank(item.getRoleCode())) {
            return JkBizConstants.PRICE_MATCH_LEVEL_ROLE;
        }
        return JkBizConstants.PRICE_MATCH_LEVEL_ACTIVITY;
    }
}
