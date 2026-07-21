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
@TableName("jk_commission_rule_item")
public class JkCommissionRuleItem implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long ruleId;
    private String itemNo;
    private Integer productId;
    private Integer skuId;
    private String receiverRoleCode;
    private String calculationType;
    private BigDecimal commissionRate;
    private BigDecimal fixedAmount;
    private Integer priority;
    private Boolean status;
    private String itemConfigJson;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false)
    private String receiverRoleName;
    @TableField(exist = false)
    private String calculationTypeText;
    @TableField(exist = false)
    private String statusText;
    @TableField(exist = false)
    private String statusTag;
}
