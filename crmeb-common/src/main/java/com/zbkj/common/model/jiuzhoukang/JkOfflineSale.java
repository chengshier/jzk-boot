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
import java.util.List;

/** 创客、合伙人、区县代理的线下终端销售单。 */
@Data
@Accessors(chain = true)
@TableName("jk_offline_sale")
public class JkOfflineSale implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String saleNo;
    private String requestNo;
    private Long sellerUserId;
    private String sellerRoleCode;
    private Long countyAgentUserId;
    private Long directParentUserId;
    private String regionCode;
    private String customerType;
    private Long customerUserId;
    private String customerNameMasked;
    private String customerPhoneMasked;
    private Boolean registeredCustomer;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private String payMethod;
    private Date saleTime;
    private String voucherUrls;
    private String promotionSource;
    private String status;
    private Boolean auditRequired;
    private Long auditUserId;
    private Date auditTime;
    private String auditRemark;
    private String cancelReason;
    private String relationSnapshotJson;
    private String riskSnapshotJson;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;
    @TableField(exist = false) private String sellerName;
    @TableField(exist = false) private String sellerRoleName;
    @TableField(exist = false) private String countyAgentName;
    @TableField(exist = false) private String statusText;
    @TableField(exist = false) private String statusTag;
    @TableField(exist = false) private List<JkOfflineSaleItem> items;
    @TableField(exist = false) private List<JkOfflineSaleAuditLog> auditLogs;
}
