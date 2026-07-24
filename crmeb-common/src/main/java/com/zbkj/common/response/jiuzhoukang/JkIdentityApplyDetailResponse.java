package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** Front-end identity application detail, limited to the current applicant. */
@Data
public class JkIdentityApplyDetailResponse implements Serializable {
    private JkIdentityApplyResponse application;
    private List<JkAuditLogResponse> auditLogs;
}
