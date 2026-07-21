package com.zbkj.service.service.jiuzhoukang.price;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.jiuzhoukang.JkProductPriceRule;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkPriceRuleSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkPriceRuleSearchRequest;
import com.zbkj.common.request.jiuzhoukang.JkPriceRuleStatusRequest;
import com.zbkj.common.response.jiuzhoukang.JkPriceRuleResponse;

import java.util.List;

public interface JkPriceRuleService extends IService<JkProductPriceRule> {
    List<JkPriceRuleResponse> getAdminList(JkPriceRuleSearchRequest request, PageParamRequest pageParamRequest);
    JkPriceRuleResponse saveRule(JkPriceRuleSaveRequest request);
    Boolean updateRuleStatus(JkPriceRuleStatusRequest request);
    List<JkProductPriceRule> listActiveRules(Integer productId, Integer skuId);
}
