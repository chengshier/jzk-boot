package com.zbkj.service.service.jiuzhoukang.region;

import com.zbkj.common.request.jiuzhoukang.JkAgentRelationBindRequest;
import com.zbkj.common.request.jiuzhoukang.JkAgentRelationForceAdjustRequest;
import com.zbkj.common.response.jiuzhoukang.JkAgentRelationResponse;

import java.util.List;

public interface JkAgentRelationService {
    List<JkAgentRelationResponse> list(Long userId, Long parentUserId, Boolean activeOnly);

    /**
     * 首次绑定兼容入口。用户已有有效上级时不得覆盖，只能返回同上级幂等结果或提示走换绑。
     */
    JkAgentRelationResponse bind(JkAgentRelationBindRequest request, Long operatorId);

    /** 审核通过的换绑申请专用入口。 */
    JkAgentRelationResponse changeFromApprovedApply(JkAgentRelationBindRequest request, Long currentRelationId,
                                                     String reservationRequestNo, Long operatorId);

    /** 管理员强制调整专用入口，必须记录原因和审计日志。 */
    JkAgentRelationResponse forceAdjust(JkAgentRelationForceAdjustRequest request, Long operatorId);

    boolean invalidate(Long id, String reason, Long operatorId);
}
