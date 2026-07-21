package com.zbkj.common.request.jiuzhoukang;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel(value = "JkBusinessRoleStatusRequest对象", description = "九州康业务角色状态修改请求")
public class JkBusinessRoleStatusRequest implements Serializable {
    @NotNull(message = "角色ID不能为空")
    private Long roleId;
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
