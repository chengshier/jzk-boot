package com.zbkj.service.service.jiuzhoukang.audit;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkAuditLogSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkAuditLogResponse;

import java.util.List;

public interface JkAuditLogService extends IService<JkAuditLog> {
    void saveAuditLog(JkAuditLog auditLog);
    List<JkAuditLogResponse> getAdminList(JkAuditLogSearchRequest request, PageParamRequest pageParamRequest);
    List<JkAuditLogResponse> toResponses(List<JkAuditLog> logs);
}
