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

/**
 * V3.1 库存盘点单。
 * 持久化字段与 jk_v31_phase3_operations_health.sql 保持一致；旧模型字段仅作兼容展示，不参与 SQL。
 */
@Data
@Accessors(chain = true)
@TableName("jk_stock_check")
public class JkStockCheck implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String checkNo;
    private String requestNo;
    private Long stockAccountId;
    private Long ownerUserId;
    private String scopeType;
    private String status;
    private String freezeStatus;
    private Integer bookTotalQty;
    private Integer actualTotalQty;
    private Integer profitQty;
    private Integer lossQty;
    private Long auditUserId;
    private Date auditTime;
    private String auditRemark;
    private String adjustActionKey;
    private Date completedTime;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;

    /* 历史模型兼容字段：Phase3 最终表不持久化。 */
    @TableField(exist = false) private String ownerRoleCode;
    @TableField(exist = false) private String regionCode;
    @TableField(exist = false) private String checkType;
    @TableField(exist = false) private Date snapshotTime;
    @TableField(exist = false) private Date submittedAt;
    @TableField(exist = false) private Integer differenceQuantity;
    @TableField(exist = false) private BigDecimal differenceAmount;
    @TableField(exist = false) private Integer version;

    @TableField(exist = false) private String ownerName;
    @TableField(exist = false) private String statusText;
    @TableField(exist = false) private String statusTag;
    @TableField(exist = false) private List<JkStockCheckItem> items;
    @TableField(exist = false) private List<JkStockCheckAuditLog> auditLogs;
    @TableField(exist = false) private List<JkStockCheckLog> logs;
}
