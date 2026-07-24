package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class JkRolePermissionAssignRequest {
    @NotNull(message = "角色 ID 不能为空") private Long roleId;
    private List<String> permissionCodes;
}
