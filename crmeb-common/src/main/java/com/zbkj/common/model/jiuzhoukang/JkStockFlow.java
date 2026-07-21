package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jk_stock_flow")
@ApiModel(value = "JkStockFlow对象", description = "九州康库存流水表")
public class JkStockFlow implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String flowNo;
    private String idempotencyKey;
    private String businessNo;
    private String businessType;
    private Long businessId;
    private Long stockAccountId;
    private Long stockItemId;
    private Integer productId;
    private Integer skuId;
    private String skuCode;
    private String flowType;
    private Integer changeQty;
    private Integer beforeAvailableQty;
    private Integer afterAvailableQty;
    private Integer beforeFrozenQty;
    private Integer afterFrozenQty;
    private String remark;
    private Boolean status;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;
    private String tenantId;
    private Long createDept;
}
