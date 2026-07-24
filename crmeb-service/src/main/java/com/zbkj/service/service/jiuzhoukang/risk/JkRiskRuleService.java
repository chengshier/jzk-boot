package com.zbkj.service.service.jiuzhoukang.risk;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkRiskRule;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkRiskRuleSaveRequest;

/** 风险规则配置和扫描服务。扫描结果只进入风险中心，不自动改业务账。 */
public interface JkRiskRuleService {
    PageInfo<JkRiskRule> list(String keyword, String scannerType, Boolean enabled, PageParamRequest page);
    JkRiskRule save(Long operatorId, JkRiskRuleSaveRequest request);
    JkRiskRule setEnabled(Long operatorId, Long id, boolean enabled);
    int runOne(Long operatorId, Long id);
    int runEnabled(Long operatorId, int limit);
}
