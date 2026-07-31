package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Data
public class JkBusinessRulePlanPublishRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "方案版本ID不能为空") private Long planId;
    @NotNull(message = "生效时间不能为空") private Date effectiveStartTime;
    private Date effectiveEndTime;
    private String changeSummary;
}
