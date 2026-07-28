package com.zbkj.service.service.jiuzhoukang.commission;

import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleTrialRequest;
import com.zbkj.common.response.jiuzhoukang.JkCommissionRuleTrialResponse;

import java.util.List;

/** 统一佣金场景匹配、试算和真实生成入口。 */
public interface CommissionScenarioService {
    List<JkCommissionRuleTrialResponse> trial(JkCommissionRuleTrialRequest request);
    void dispatch(JkCommissionRuleTrialRequest request, String eventKey, String sourceNo, String requestNo);
}
