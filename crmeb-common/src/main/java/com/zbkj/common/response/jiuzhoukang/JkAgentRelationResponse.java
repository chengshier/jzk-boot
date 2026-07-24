package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

import java.util.Date;

@Data
public class JkAgentRelationResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userPhone;
    private String userAvatar;
    private String roleCode;
    private String roleName;
    private String regionCode;
    private String regionName;
    private Boolean freezeStatus;
    private String identityStatusText;
    private Integer directTeamCount;
    private Integer teamCount;
    private Long parentUserId;
    private String parentName;
    private String parentPhone;
    private Long rootUserId;
    private String relationType;
    private String bindSource;
    private String sourceCode;
    private Date effectiveTime;
    private Date expireTime;
    private String changeReason;
    private String remark;
    private Boolean status;
    private Date createTime;
}
