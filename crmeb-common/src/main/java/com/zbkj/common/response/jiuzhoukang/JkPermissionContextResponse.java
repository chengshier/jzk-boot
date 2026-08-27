package com.zbkj.common.response.jiuzhoukang;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(value = "JkPermissionContextResponse对象", description = "九州康业务权限上下文")
public class JkPermissionContextResponse implements Serializable {

    private Long userId;
    private Boolean entryAccess;
    private String primaryRoleCode;
    private String primaryRoleName;
    private List<String> roles;
    private String auditStatus;
    private String auditStatusText;
    private Boolean freezeStatus;
    private String regionCode;
    private String regionName;
    private Long belongCountyAgentId;
    private List<String> canApplyRoles;
    private List<String> permissions;
    private List<DataScopeItem> dataScopes;
    private List<String> menus;
    private String identityStatusText;
    private String disableReason;
    private String disabledReasonText;
    private Long cacheVersion;

    @Data
    public static class DataScopeItem implements Serializable {
        @ApiModelProperty(value = "数据范围类型")
        private String scopeType;
        private String scopeTypeText;
        private String regionCode;
        private String regionName;
        private Long countyAgentId;
        private Long teamRootUserId;
    }
}
