package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 佣金规则草稿保存请求；保存不等于发布。 */
@Data
public class JkCommissionRuleSaveRequest implements Serializable {
    private Long id;
    private Long planId;
    private Integer versionNo;
    private String ruleCode;
    private String ruleName;
    private Integer ruleVersion;
    private String rewardType;
    private String sourceType;
    private String performanceType;
    private String receiverRoleCode;
    private String beneficiaryType;
    private String baseType;
    private String calculationType;
    private BigDecimal rate;
    private BigDecimal fixedAmount;
    private BigDecimal unitAmount;
    private String triggerTiming;
    private Integer settleDelayDays;
    private String stackGroup;
    private String stackPolicy;
    private Integer priority;
    private BigDecimal perOrderCap;
    private BigDecimal perUserPeriodCap;
    private BigDecimal totalBudget;
    private Boolean requiresRegisteredCustomer;
    private Boolean requiresVoucher;
    private Boolean requiresAudit;
    private String receiverRoleCodeLegacy;
    private String regionCode;
    private Date effectiveTime;
    private Date expireTime;
    private Integer freezeDays;
    private String ruleConfigJson;
    private String remark;
}
