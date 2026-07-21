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
@TableName("jk_fund_flow")
public class JkFundFlow implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String flowNo;
    private Long accountId;
    private Long withdrawApplyId;
    private String flowType;
    private BigDecimal changeAmount;
    private BigDecimal beforeAmount;
    private BigDecimal afterAmount;
    private String sourceType;
    private Long sourceId;
    private String requestNo;
    private String idempotencyKey;
    private String remark;
    private Date createTime;

    @TableField(exist = false)
    private String applicantName;
    @TableField(exist = false)
    private String applicantPhone;
    @TableField(exist = false)
    private String userNickname;
    @TableField(exist = false)
    private String roleName;
    @TableField(exist = false)
    private String regionName;
    @TableField(exist = false)
    private String statusText;
    @TableField(exist = false)
    private String statusTag;
    @TableField(exist = false)
    private String flowTypeText;
    @TableField(exist = false)
    private String fundFlowTypeText;
    @TableField(exist = false)
    private String sourceTypeText;
}
