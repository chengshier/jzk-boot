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
@TableName("jk_stock_check")
public class JkStockCheck implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String checkNo;
    private String requestNo;
    private Long stockAccountId;
    private Long ownerUserId;
    private String ownerRoleCode;
    private String regionCode;
    private String checkType;
    private String status;
    private Date snapshotTime;
    private Date submittedAt;
    private Long auditUserId;
    private Date auditTime;
    private String auditRemark;
    private Integer differenceQuantity;
    private BigDecimal differenceAmount;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;
    @TableField(exist = false) private String ownerName;
    @TableField(exist = false) private String statusText;
    @TableField(exist = false) private String statusTag;
    @TableField(exist = false) private List<JkStockCheckItem> items;
    @TableField(exist = false) private List<JkStockCheckLog> logs;
}
