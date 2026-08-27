package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
public class JkCommissionRulePublishRequest {
    @NotNull(message = "规则不能为空") private Long ruleId;
    @NotNull(message = "生效时间不能为空") private Date effectiveStartTime;
    private Date effectiveEndTime;
    @NotBlank(message = "发布说明不能为空") private String remark;
}
