package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class JkRegionAgentBindRequest {
    @NotBlank(message = "区域编码不能为空") private String regionCode;
    @NotNull(message = "区县代用户不能为空") private Long countyAgentUserId;
    private String requestNo;
    private String remark;
    private String changeReason;
}
