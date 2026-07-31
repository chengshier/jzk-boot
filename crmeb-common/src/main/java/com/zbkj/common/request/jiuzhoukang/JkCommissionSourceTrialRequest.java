package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/** 运营试算只选择真实业务单据，不填写快照用户ID或计算基数。 */
@Data
public class JkCommissionSourceTrialRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long ruleId;
    @NotBlank(message = "业务来源不能为空") private String sourceType;
    private Long sourceId;
    private Long sourceItemId;
    private String sourceNo;
}
