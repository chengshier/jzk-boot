package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_stock_transfer_return")
public class JkStockTransferReturn implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String returnNo;
    private String requestNo;
    private Long originalTransferId;
    private String originalTransferNo;
    private Long userId;
    private String roleCode;
    private Long countyAgentId;
    private String regionCode;
    private String status;
    private String auditStatus;
    private String refundStatus;
    private BigDecimal returnAmount;
    private String returnReason;
    private Long auditUserId;
    private Date auditTime;
    private String auditRemark;
    private String rejectReason;
    private String logisticsCompany;
    private String logisticsNo;
    private Date shipTime;
    private Long receiveUserId;
    private Date receiveTime;
    private String receiveRemark;
    private Long refundUserId;
    private Date refundTime;
    private String refundVoucherUrl;
    private String refundRemark;
    private String cancelReason;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;

    @TableField(exist = false) private String applicantName;
    @TableField(exist = false) private String applicantPhone;
    @TableField(exist = false) private String roleName;
    @TableField(exist = false) private String regionName;
    @TableField(exist = false) private String statusText;
    @TableField(exist = false) private String statusTag;
    @TableField(exist = false) private String auditStatusText;
    @TableField(exist = false) private String refundStatusText;
    @TableField(exist = false) private String firstProductName;
    @TableField(exist = false) private String firstSkuName;
}
