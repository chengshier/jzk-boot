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
@TableName("jk_stock_item")
@ApiModel(value = "JkStockItem对象", description = "九州康库存明细表")
public class JkStockItem implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String businessNo;
    private Long stockAccountId;
    private Integer productId;
    private Integer skuId;
    private String skuCode;
    private Integer availableQty;
    private Integer frozenQty;
    private Integer totalInQty;
    private Integer totalOutQty;
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
