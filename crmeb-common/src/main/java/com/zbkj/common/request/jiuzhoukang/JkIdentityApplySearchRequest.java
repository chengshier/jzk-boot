package com.zbkj.common.request.jiuzhoukang;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "JkIdentityApplySearchRequest对象", description = "九州康身份申请搜索请求")
public class JkIdentityApplySearchRequest implements Serializable {
    private String keyword;
    private String auditStatus;
    private String applyRoleCode;
    private String regionCode;
}
