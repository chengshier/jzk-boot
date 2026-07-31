package com.zbkj.common.request.jiuzhoukang;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@ApiModel(value = "JkUserProfileRegionSaveRequest", description = "保存现有用户个人资料中的九州康标准所在地区")
public class JkUserProfileRegionSaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请选择所在地区")
    @ApiModelProperty(value = "有效 jk_region.region_code", required = true)
    private String regionCode;

    @ApiModelProperty(value = "请求幂等号；后台修改时用于审计")
    private String requestNo;

    @ApiModelProperty(value = "后台修改原因；用户自行修改时可为空")
    private String reason;
}
