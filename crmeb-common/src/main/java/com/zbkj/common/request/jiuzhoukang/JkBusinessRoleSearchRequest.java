package com.zbkj.common.request.jiuzhoukang;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "JkBusinessRoleSearchRequest对象", description = "九州康业务角色搜索请求")
public class JkBusinessRoleSearchRequest implements Serializable {
    private String keyword;
    private Boolean enabled;
}
