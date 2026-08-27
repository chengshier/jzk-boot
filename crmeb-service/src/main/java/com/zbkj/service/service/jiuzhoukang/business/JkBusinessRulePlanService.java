package com.zbkj.service.service.jiuzhoukang.business;

import com.zbkj.common.model.jiuzhoukang.JkBusinessRulePlan;
import com.zbkj.common.request.jiuzhoukang.JkBusinessRulePlanPublishRequest;
import com.zbkj.common.request.jiuzhoukang.JkBusinessRulePlanSaveRequest;

import java.util.List;
import java.util.Map;

public interface JkBusinessRulePlanService {
    List<JkBusinessRulePlan> list(String planCode, String publishStatus);
    Map<String, Object> detail(Long id);
    JkBusinessRulePlan saveDraft(JkBusinessRulePlanSaveRequest request);
    JkBusinessRulePlan copyVersion(Long id, String changeSummary);
    JkBusinessRulePlan publish(JkBusinessRulePlanPublishRequest request, Long operatorId);
    JkBusinessRulePlan disable(Long id, String reason, Long operatorId);
    List<Map<String, Object>> roleCards();
}
