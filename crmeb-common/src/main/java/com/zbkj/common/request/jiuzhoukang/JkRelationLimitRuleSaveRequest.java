package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/** 保存直属下级人数限制规则。 */
@Data
public class JkRelationLimitRuleSaveRequest {
    private Long id;
    @NotBlank(message = "规则编码不能为空")
    private String ruleCode;
    @NotBlank(message = "规则名称不能为空")
    private String ruleName;
    private Long planId;
    private String versionNo;
    private String parentRoleCode;
    private String childRoleCode;
    private String regionCode;
    @NotNull(message = "直属下级上限不能为空")
    @Min(value = 0, message = "直属下级上限不能小于0")
    @Max(value = 100000, message = "直属下级上限过大")
    private Integer maxDirectChildren;
    @Min(value = 1, message = "预警阈值不能小于1")
    @Max(value = 100, message = "预警阈值不能大于100")
    private Integer warningThreshold;
    private String overflowPolicy;
    private Integer priority;
    private Date effectiveStartTime;
    private Date effectiveEndTime;
    private Boolean status;
    private String remark;
}
