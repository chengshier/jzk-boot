package com.zbkj.service.service.jiuzhoukang.region;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkAgentRelationChangeApply;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkAgentRelationChangeApplyRequest;
import com.zbkj.common.request.jiuzhoukang.JkAgentRelationChangeAuditRequest;

public interface JkAgentRelationChangeService {
    JkAgentRelationChangeApply apply(Long userId, JkAgentRelationChangeApplyRequest request);
    PageInfo<JkAgentRelationChangeApply> listMine(Long userId, PageParamRequest page);
    PageInfo<JkAgentRelationChangeApply> listAdmin(String status, Long userId, PageParamRequest page);
    JkAgentRelationChangeApply detail(Long viewerUserId, Long id, boolean admin);
    JkAgentRelationChangeApply audit(Long operatorId, JkAgentRelationChangeAuditRequest request);
    JkAgentRelationChangeApply cancel(Long userId, Long id, String requestNo, String reason);
}
