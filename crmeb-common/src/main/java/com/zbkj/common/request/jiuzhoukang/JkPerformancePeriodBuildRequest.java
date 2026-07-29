package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Data
public class JkPerformancePeriodBuildRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    @NotBlank(message = "周期类型不能为空") private String periodType;
    @NotNull(message = "开始时间不能为空") private Date startTime;
    @NotNull(message = "结束时间不能为空") private Date endTime;
    private Long planId;
    private Long ruleId;
    private String ownerRoleCode;
    private String regionCode;
    @NotBlank(message = "requestNo不能为空") private String requestNo;
}
