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

@Data @Accessors(chain = true) @TableName("jk_fund_account")
public class JkFundAccount implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String accountNo; private Long userId; private String roleCode; private String regionCode;
    private BigDecimal availableAmount; private BigDecimal withdrawingAmount; private BigDecimal withdrawnAmount;
    private BigDecimal rejectedReturnAmount; private BigDecimal frozenAmount; private BigDecimal negativeOffsetAmount;
    private Boolean status; private Boolean isDeleted; private Integer version; private Date createTime; private Date updateTime;

    @TableField(exist = false) private String applicantName;
    @TableField(exist = false) private String applicantPhone;
    @TableField(exist = false) private String userNickname;
    @TableField(exist = false) private String roleName;
    @TableField(exist = false) private String regionName;
    @TableField(exist = false) private String statusText;
    @TableField(exist = false) private String statusTag;
}
