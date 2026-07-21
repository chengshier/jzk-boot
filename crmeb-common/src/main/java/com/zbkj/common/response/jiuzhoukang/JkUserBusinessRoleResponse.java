package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class JkUserBusinessRoleResponse implements Serializable {
    private Long id;
    private Long userId;
    private String applicantName;
    private String applicantPhone;
    private String userNickname;
    private String nickname;
    private String phone;
    private String roleCode;
    private String roleName;
    private Boolean isPrimary;
    private String auditStatus;
    private String auditStatusText;
    private String statusTag;
    private Boolean freezeStatus;
    private String freezeReason;
    private String regionCode;
    private String regionName;
    private Long belongCountyAgentId;
    private Date effectiveTime;
    private List<String> permissionCodes;
}
