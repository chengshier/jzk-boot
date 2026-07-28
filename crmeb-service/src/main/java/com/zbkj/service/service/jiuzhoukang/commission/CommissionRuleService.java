package com.zbkj.service.service.jiuzhoukang.commission;

import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRuleItem;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleItemSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRulePublishRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleSaveRequest;

import java.util.List;

public interface CommissionRuleService {
    JkCommissionRule saveRule(JkCommissionRuleSaveRequest request);
    JkCommissionRule publish(JkCommissionRulePublishRequest request, Long operatorId);
    JkCommissionRule disable(Long id, String reason, Long operatorId);
    boolean updateStatus(Long id, boolean status);
    List<JkCommissionRule> listActiveRules(String sourceType, String receiverRoleCode);
    List<JkCommissionRule> listRules(String sourceType, String receiverRoleCode, Boolean status);
    JkCommissionRuleItem saveItem(JkCommissionRuleItemSaveRequest request);
    List<JkCommissionRuleItem> listItems(Long ruleId);
    boolean updateItemStatus(Long id, boolean status);
}
