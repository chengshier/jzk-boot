package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

@Data
public class JkBusinessRulePlanSaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    @NotBlank(message = "方案编码不能为空") private String planCode;
    @NotBlank(message = "方案名称不能为空") private String planName;
    private List<String> applicableRoleCodes;
    private List<String> applicableRegionCodes;
    private Integer priority;
    private String changeSummary;
    private String remark;
}
