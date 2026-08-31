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

/** V3.1 库存盘点明细，持久化字段与 Phase3 SQL 一致。 */
@Data
@Accessors(chain = true)
@TableName("jk_stock_check_item")
public class JkStockCheckItem implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long checkId;
    private Long stockItemId;
    private Integer productId;
    private Integer skuId;
    private String skuCode;
    private Integer bookAvailableQty;
    private Integer bookFrozenQty;
    private Integer actualAvailableQty;
    private Integer differenceQty;
    private String differenceType;
    private String remark;
    private Integer versionSnapshot;
    private Boolean adjusted;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;

    /* 历史模型兼容字段：不参与最终表 SQL。 */
    @TableField(exist = false) private String productName;
    @TableField(exist = false) private String skuName;
    @TableField(exist = false) private Integer bookQuantity;
    @TableField(exist = false) private Integer actualQuantity;
    @TableField(exist = false) private Integer differenceQuantity;
    @TableField(exist = false) private BigDecimal unitCost;
    @TableField(exist = false) private BigDecimal differenceAmount;
    @TableField(exist = false) private String countRemark;
    @TableField(exist = false) private String adjustStatus;
}
