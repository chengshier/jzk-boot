package com.zbkj.common.request.jiuzhoukang;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "JkAuditLogSearchRequest对象", description = "九州康审核日志搜索请求")
public class JkAuditLogSearchRequest implements Serializable {
    private String businessType;
    private Long businessId;
    private String auditAction;
}
