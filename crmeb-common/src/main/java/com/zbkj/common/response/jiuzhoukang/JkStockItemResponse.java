package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class JkStockItemResponse implements Serializable {
    private Long id;
    private Long stockAccountId;
    private String applicantName;
    private String applicantPhone;
    private String userNickname;
    private String roleName;
    private String regionName;
    private Integer productId;
    private Integer skuId;
    private String skuCode;
    private String productName;
    private String skuName;
    private String skuText;
    private String productImage;
    private String unitName;
    private String barCode;
    /** CRMEB 商品/SKU 当前零售价，仅用于展示。 */
    private BigDecimal retailPrice;
    /** CRMEB 商品/SKU 当前成本价，仅用于库存价值估算。 */
    private BigDecimal costPrice;
    /** 页面展示参考价，优先取成本价，缺失时回退零售价。 */
    private BigDecimal referencePrice;
    /** 当前可用库存按 referencePrice 估算的价值。 */
    private BigDecimal stockValue;
    private Integer availableQty;
    /** 对 App 旧字段的兼容别名。 */
    private Integer availableQuantity;
    private Integer frozenQty;
    private Integer totalInQty;
    private Integer totalOutQty;
    private Integer version;
}