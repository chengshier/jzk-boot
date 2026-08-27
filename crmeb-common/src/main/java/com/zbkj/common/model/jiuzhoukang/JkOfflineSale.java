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
    private String regionCode;
    private String customerType;
    private Long customerUserId;
    private String customerNameMasked;
    private String customerPhoneMasked;
    private Boolean registeredCustomer;
    private String paymentMethod;
    private String voucherUrl;
    private String promotionSource;
    private Date saleTime;
    private BigDecimal totalAmount;
    private BigDecimal totalCostAmount;
    private BigDecimal totalProfitAmount;
    private Boolean auditRequired;
    private String auditStatus;
    private String status;
    private String relationSnapshotJson;
    private String sourceSnapshotJson;
    private String cancelReason;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false) private List<JkOfflineSaleItem> items;
    @TableField(exist = false) private List<JkOfflineSaleAuditLog> auditLogs;
}
