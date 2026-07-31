package com.zbkj.service.service.jiuzhoukang.commission;

import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.request.jiuzhoukang.JkCommissionTemplateSaveRequest;

import java.util.List;
import java.util.Map;

public interface JkCommissionTemplateService {
    List<Map<String, Object>> templates(String receiverRoleCode);
    JkCommissionRule saveDraft(JkCommissionTemplateSaveRequest request);
}
