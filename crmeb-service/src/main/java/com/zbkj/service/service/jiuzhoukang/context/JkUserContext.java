package com.zbkj.service.service.jiuzhoukang.context;

import com.zbkj.common.model.jiuzhoukang.JkUserDataScope;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class JkUserContext implements Serializable {
    private Long userId;
    private String primaryRoleCode;
    private String primaryRoleName;
    private String auditStatus;
    private Boolean freezeStatus;
    private String freezeReason;
    private String regionCode;
    private Long belongCountyAgentId;
    private List<String> roles = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();
    private List<JkUserDataScope> dataScopes = new ArrayList<>();
    private List<String> canApplyRoles = new ArrayList<>();
    private Long cacheVersion;

    // 显式保留关键访问器，供未启用 Lombok 增量处理的 JDK8 编译路径调用。
    public Long getUserId() { return userId; }
    public List<String> getPermissions() { return permissions; }
}
