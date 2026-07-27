package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_withdraw_apply")
public class JkWithdrawApply implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String withdrawNo;
    private String requestNo;
    private Long userId;
    private String roleCode;
    private BigDecimal amount;
    private String status;
    /** 服务端加密收款快照，禁止直接序列化到前端。 */
    @JsonIgnore
    private String payeeSnapshotJson;
    private String rejectReason;
    private Long auditUserId;
    private Date auditTime;
    private Long paidUserId;
    private Date paidTime;
    private Boolean isDeleted;
    private Integer version;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false)
    private String applicantName;
    @TableField(exist = false)
    private String applicantPhone;
    @TableField(exist = false)
    private String userNickname;
    @TableField(exist = false)
    private String roleName;
    @TableField(exist = false)
    private String statusText;
    @TableField(exist = false)
    private String statusTag;
    @TableField(exist = false)
    private String withdrawStatusText;
    @TableField(exist = false)
    private Long payeeAccountId;
    @TableField(exist = false)
    private String payeeAccountType;
    @TableField(exist = false)
    private String payeeAccountName;
    @TableField(exist = false)
    private String payeeBankName;
    @TableField(exist = false)
    private String payeeBankAccountMask;
}
