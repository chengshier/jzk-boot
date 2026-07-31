package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_stock_check_item")
public class JkStockCheckItem implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long checkId;
    private Integer productId;
    private Integer skuId;
    private String skuCode;
    private String productName;
    private String skuName;
    private Integer bookQuantity;
    private Integer actualQuantity;
    private Integer differenceQuantity;
    private BigDecimal unitCost;
    private BigDecimal differenceAmount;
    private String countRemark;
    private String adjustStatus;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
