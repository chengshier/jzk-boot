package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jk_product_price_rule")
@ApiModel(value = "JkProductPriceRule对象", description = "九州康商品价格规则表")
public class JkProductPriceRule implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @ApiModelProperty(value = "规则编号")
    private String ruleNo;
    private Integer productId;
    private Integer skuId;
    private String skuCode;
    private String roleCode;
    private String regionCode;
    private Long userId;
    private String priceType;
    private BigDecimal fixedPrice;
    private BigDecimal discountRate;
    private Integer ruleVersion;
    private Date effectiveTime;
    private Date expireTime;
    private Boolean status;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;
    private String remark;
    private String tenantId;
    private Long createDept;
}
