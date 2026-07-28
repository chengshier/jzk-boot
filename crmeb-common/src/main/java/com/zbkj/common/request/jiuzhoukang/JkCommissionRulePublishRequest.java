package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
public class JkCommissionRulePublishRequest {
    @NotNull private Long ruleId;
    @NotNull private Boolean trialConfirmed;
    @NotNull private Date effectiveStartTime;
    private Date effectiveEndTime;
    private String remark;
}
