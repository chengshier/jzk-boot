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

/** 可版本化、可试算、可审核发布的佣金规则。status 仅表示业务开关，publishStatus 控制是否允许真实入账。 */
@Data
@Accessors(chain = true)
@TableName("jk_commission_rule")
public class JkCommissionRule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO) private Long id;
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

    private Long planId;
    private String planCode;
    private Integer planVersionNo;
    /** 兼容旧字段，等同于 planVersionNo。 */
    private Integer versionNo;
    private String templateCode;
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
    private String scopeConfigJson;
    private String incomeNature;
    private Date effectiveStartTime;
    private Date effectiveEndTime;
    private String publishStatus;
    private Long publishedBy;
    /** 正式数据库列为 published_at。 */
    private Date publishedAt;

    /**
     * 兼容 Phase3 整合期间服务层曾使用的 publishedTime 命名；只转写 publishedAt，绝不映射第二个数据库列。
     */
    public JkCommissionRule setPublishedTime(Date publishedTime) {
        this.publishedAt = publishedTime;
        return this;
    }

    @TableField(exist = false) private String sourceTypeText;
    @TableField(exist = false) private String receiverRoleName;
    @TableField(exist = false) private String statusText;
    @TableField(exist = false) private String statusTag;
    @TableField(exist = false) private String publishStatusText;
    @TableField(exist = false) private String capabilityStatusText;
}
