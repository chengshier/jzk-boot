package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/** 普通运营使用的业务模板表单；技术枚举由服务端模板映射。 */
@Data
public class JkCommissionTemplateSaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long ruleId;
    @NotNull(message = "商业方案不能为空")
    private Long planId;
    @NotBlank(message = "奖励模板不能为空")
    private String templateCode;
    @NotBlank(message = "规则名称不能为空")
    private String ruleName;
    @NotBlank(message = "适用受益身份不能为空")
    private String receiverRoleCode;
    @NotBlank(message = "奖励方式不能为空")
    private String rewardMode;
    @NotNull(message = "奖励数值不能为空")
    private BigDecimal rewardValue;
    private List<Integer> productIds;
    private List<String> regionCodes;
    private BigDecimal performanceThreshold;
    private String periodType;
    /** 留空表示不限制，填写后必须大于零。 */
    private BigDecimal perOrderCap;
    /** 留空表示不限制，填写后必须大于零。 */
    private BigDecimal perUserPeriodCap;
    /** 留空表示不限制，填写后必须大于零。 */
    private BigDecimal totalBudget;
    private Integer settleDelayDays;
    private String remark;
}
