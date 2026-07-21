package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class JkIdentityApplyResponse implements Serializable {
    private Long id;
    private String requestNo;
    private String applyNo;
    private Long userId;
    private String applyRoleCode;
    private String applyRoleName;
    private String auditStatus;
    private String auditStatusText;
    private Boolean freezeStatus;
    private String rejectReason;
    private String applyReason;
    private String regionCode;
    private String regionName;
    private Long belongCountyAgentId;
    private Long parentUserId;
    private String applicantName;
    private String applicantPhone;
    private String userNickname;
    private String statusText;
    private String statusTag;
    private String certificateFiles;
    private Date createTime;
    private Date effectiveTime;
}
