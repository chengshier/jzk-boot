package com.zbkj.service.service.jiuzhoukang.identity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.jiuzhoukang.JkIdentityApply;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkIdentityApplyAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkIdentityApplyRequest;
import com.zbkj.common.request.jiuzhoukang.JkIdentityApplySearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkIdentityApplyResponse;
import com.zbkj.common.response.jiuzhoukang.JkIdentityApplyDetailResponse;

import java.util.List;

public interface JkIdentityApplyService extends IService<JkIdentityApply> {
    JkIdentityApplyResponse submitApply(Long userId, JkIdentityApplyRequest request);
    List<JkIdentityApplyResponse> getMyApplyList(Long userId, PageParamRequest pageParamRequest);
    JkIdentityApplyDetailResponse getMyApplyDetail(Long userId, Long applyId);
    List<JkIdentityApplyResponse> getAdminApplyList(JkIdentityApplySearchRequest request, PageParamRequest pageParamRequest);
    Boolean auditApply(JkIdentityApplyAuditRequest request);
}
