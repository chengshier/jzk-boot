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
@TableName("jk_offline_sale_item")
public class JkOfflineSaleItem implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long saleId;
    private Integer productId;
    private Integer skuId;
    private String productName;
    private String skuName;
    private String skuCode;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private BigDecimal unitCost;
    private BigDecimal costAmount;
    private BigDecimal profitAmount;
    private Long stockAccountId;
    private String costSnapshotJson;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
