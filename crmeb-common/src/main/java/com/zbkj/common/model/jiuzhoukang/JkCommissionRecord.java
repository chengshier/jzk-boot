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

/** 平台应付佣金记录。线下经营毛利不得写入本表。 */
@Data
@Accessors(chain = true)
@TableName("jk_commission_record")
public class JkCommissionRecord implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String commissionNo;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private Long sourceItemId;
    private Long receiverUserId;
    private String receiverRoleCode;
    private Long ruleId;
    private Integer ruleVersion;
    private Integer ruleVersionNo;
    private String rewardType;
    private String incomeNature;
    private BigDecimal baseAmount;
    private BigDecimal commissionAmount;
    private BigDecimal settledAmount;
    private BigDecimal reversedAmount;
    private String status;
    private Date freezeEndTime;
    private Date settleTime;
    private String ruleSnapshotJson;
    private String beneficiarySnapshotJson;
    private String calculationSnapshotJson;
    private String idempotencyKey;
    private String commissionActionKey;
    private String requestNo;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false) private String applicantName;
    @TableField(exist = false) private String applicantPhone;
    @TableField(exist = false) private String userNickname;
    @TableField(exist = false) private String roleName;
    @TableField(exist = false) private String statusText;
    @TableField(exist = false) private String statusTag;
    @TableField(exist = false) private String commissionStatusText;
    @TableField(exist = false) private String sourceTypeText;
    @TableField(exist = false) private String rewardTypeText;
}
