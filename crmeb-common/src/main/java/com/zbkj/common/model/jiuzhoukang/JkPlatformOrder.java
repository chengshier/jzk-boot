package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_platform_order")
public class JkPlatformOrder {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String platformOrderNo; private String requestNo; private Long userId; private String roleCode; private Long countyAgentId; private String regionCode;
    private String status; private String payStatus; private String auditStatus; private String logisticsStatus; private String receiveStatus; private BigDecimal totalAmount;
    private Long auditUserId; private Date auditTime; private String auditRemark; private String rejectReason; private String cancelReason; private Boolean isDeleted;
    private Date createTime; private Date updateTime; private Long createUserId; private Long updateUserId; private Integer version;
    @TableField(exist = false) private String applicantName;
    @TableField(exist = false) private String applicantPhone;
    @TableField(exist = false) private String userNickname;
    @TableField(exist = false) private String roleName;
    @TableField(exist = false) private String regionName;
    @TableField(exist = false) private String statusText;
    @TableField(exist = false) private String statusTag;
    @TableField(exist = false) private String auditStatusText;
    @TableField(exist = false) private String payStatusText;
    @TableField(exist = false) private String receiveStatusText;
    @TableField(exist = false) private String firstProductName;
    @TableField(exist = false) private String firstSkuName;
    @TableField(exist = false) private String firstSkuText;
}
