package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class JkRetailAttributionResolveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请选择最终区域")
    private String finalRegionCode;
    private Long countyAgentUserId;
    private Boolean keepDirectRelation;
    @NotBlank(message = "请输入处理原因")
    private String reason;
    @NotBlank(message = "requestNo不能为空")
    private String requestNo;
}
