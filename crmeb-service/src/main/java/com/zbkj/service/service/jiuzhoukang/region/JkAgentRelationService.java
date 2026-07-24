package com.zbkj.service.service.jiuzhoukang.region;

import com.zbkj.common.request.jiuzhoukang.JkAgentRelationBindRequest;
import com.zbkj.common.response.jiuzhoukang.JkAgentRelationResponse;
import java.util.List;

public interface JkAgentRelationService {
    List<JkAgentRelationResponse> list(Long userId, Long parentUserId, Boolean activeOnly);
    JkAgentRelationResponse bind(JkAgentRelationBindRequest request, Long operatorId);
    boolean invalidate(Long id, String reason, Long operatorId);
}
