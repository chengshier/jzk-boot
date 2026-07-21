package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class JkAuditLogResponse implements Serializable {
    private Long id;
    private String businessType;
    private String businessTypeText;
    private Long businessId;
    private String businessNo;
    private String requestNo;
    private Long auditUserId;
    private String auditUserName;
    private String auditUserType;
    private String auditAction;
    private String auditActionText;
    private String beforeStatus;
    private String beforeStatusText;
    private String afterStatus;
    private String afterStatusText;
    private String rejectReason;
    private String auditRemark;
    private String operateSource;
    private Date createTime;
}
