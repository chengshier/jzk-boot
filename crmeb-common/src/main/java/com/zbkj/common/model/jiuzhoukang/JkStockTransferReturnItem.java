package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_stock_transfer_return_item")
public class JkStockTransferReturnItem implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long returnId;
    private Long originalTransferItemId;
    private Integer productId;
    private Integer skuId;
    private String productName;
    private String skuName;
    private String skuCode;
    private Integer returnQuantity;
    private BigDecimal unitPrice;
    private BigDecimal returnAmount;
    private Long fromStockAccountId;
    private Long toStockAccountId;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
    private Integer version;
}
