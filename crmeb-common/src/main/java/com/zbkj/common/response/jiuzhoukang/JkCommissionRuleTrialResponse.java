package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class JkCommissionRuleTrialResponse {
    private String scenario;
    private String matchStatus;
    private String reasonCode;
    private Long ruleId;
    private Integer ruleVersionNo;
    private String ruleCode;
    private String rewardType;
    private Long beneficiaryUserId;
    private String beneficiaryRoleCode;
    private BigDecimal baseAmount;
    private String calculationType;
    private BigDecimal rawAmount;
    private BigDecimal cappedAmount;
    private String stackPolicy;
    private String expectedSettleDate;
    private List<String> explanations = new ArrayList<>();
}
