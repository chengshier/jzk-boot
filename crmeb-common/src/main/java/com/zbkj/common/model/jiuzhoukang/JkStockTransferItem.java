package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_stock_transfer_item")
public class JkStockTransferItem {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long transferId;
    private Integer productId;
    private Integer skuId;
    private String productName;
    private String skuName;
    private String skuCode;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Long priceRuleId;
    private Integer priceRuleVersion;
    private String priceType;
    private String priceSnapshotJson;
    private Long fromStockAccountId;
    private Long toStockAccountId;
    private Integer receivedQty;
    private BigDecimal sourceUnitCost;
    private BigDecimal costAmount;
    private BigDecimal unitSpread;
    private BigDecimal spreadAmount;
    private String costMethod;
    private String costSnapshotJson;
    private String profitStatus;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
    private Integer version;
}
