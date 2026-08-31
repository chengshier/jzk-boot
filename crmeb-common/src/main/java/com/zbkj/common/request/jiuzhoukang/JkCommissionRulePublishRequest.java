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
    /** 发布必须来自已完成人工确认的试算，不允许绕过试算直接发布。 */
    @NotNull(message = "请先完成规则试算并确认") private Boolean trialConfirmed;
    @NotBlank(message = "发布说明不能为空") private String remark;
}
