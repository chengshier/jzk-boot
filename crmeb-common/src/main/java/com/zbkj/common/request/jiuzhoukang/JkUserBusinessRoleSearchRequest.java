package com.zbkj.common.request.jiuzhoukang;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "JkUserBusinessRoleSearchRequest对象", description = "九州康用户业务身份搜索请求")
public class JkUserBusinessRoleSearchRequest implements Serializable {
    private String keyword;
    private String roleCode;
    private String auditStatus;
    private Boolean freezeStatus;
}
