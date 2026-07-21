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

@Data @Accessors(chain = true) @TableName("jk_commission_record")
public class JkCommissionRecord implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String commissionNo; private String sourceType; private Long sourceId; private String sourceNo;
    private Long receiverUserId; private String receiverRoleCode; private Long ruleId; private Integer ruleVersion;
    private BigDecimal baseAmount; private BigDecimal commissionAmount; private BigDecimal settledAmount;
    private String status; private Date freezeEndTime; private String ruleSnapshotJson; private String idempotencyKey; private String requestNo;
    private Boolean isDeleted; private Date createTime; private Date updateTime;

    @TableField(exist = false) private String applicantName;
    @TableField(exist = false) private String applicantPhone;
    @TableField(exist = false) private String userNickname;
    @TableField(exist = false) private String roleName;
    @TableField(exist = false) private String statusText;
    @TableField(exist = false) private String statusTag;
    @TableField(exist = false) private String commissionStatusText;
    @TableField(exist = false) private String sourceTypeText;
}
