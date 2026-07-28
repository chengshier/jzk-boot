package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_commission_rule")
public class JkCommissionRule implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String ruleNo;
    private String ruleName;
    private Integer ruleVersion;
    private String sourceType;
    private String receiverRoleCode;
    private String regionCode;
    private Boolean status;
    private Date effectiveTime;
    private Date expireTime;
    private Integer freezeDays;
    private String ruleConfigJson;
    private String remark;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
    private Integer version;

    /** V3.1 商业方案与规则版本。 */
    private Long planId;
    private String ruleCode;
    private String rewardType;
    private String performanceType;
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
    private String publishStatus;
    private Long publishedBy;
    private Date publishedTime;

    @TableField(exist = false) private String sourceTypeText;
    @TableField(exist = false) private String receiverRoleName;
    @TableField(exist = false) private String statusText;
    @TableField(exist = false) private String statusTag;
}
