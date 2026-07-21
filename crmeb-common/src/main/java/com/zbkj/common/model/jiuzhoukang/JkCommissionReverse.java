package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data @Accessors(chain = true) @TableName("jk_commission_reverse")
public class JkCommissionReverse implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String reverseNo; private Long originalCommissionRecordId; private String sourceType; private Long sourceId; private String sourceNo;
    private String reverseType; private BigDecimal reverseAmount; private BigDecimal beforeAmount; private BigDecimal afterAmount;
    private String reason; private String status; private String requestNo; private Long operatorId; private Date createTime; private Date updateTime;

    @TableField(exist = false) private String applicantName;
    @TableField(exist = false) private String applicantPhone;
    @TableField(exist = false) private String userNickname;
    @TableField(exist = false) private String roleName;
    @TableField(exist = false) private String statusText;
    @TableField(exist = false) private String statusTag;
    @TableField(exist = false) private String commissionStatusText;
    @TableField(exist = false) private String sourceTypeText;
    @TableField(exist = false) private String reverseTypeText;
    @TableField(exist = false) private String originalCommissionNo;
}
