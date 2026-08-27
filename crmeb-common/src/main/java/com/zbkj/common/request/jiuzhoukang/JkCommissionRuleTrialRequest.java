package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class JkCommissionRuleTrialRequest {
    private Long ruleId;
    @NotBlank(message = "业务场景不能为空") private String scenario;
    @NotBlank(message = "来源类型不能为空") private String sourceType;
    private Long sourceId;
    private Long sourceItemId;
    private Long buyerUserId;
    private Long sellerUserId;
    private Long directParentUserId;
    private Long countyAgentUserId;
    private String regionCode;
    private Integer productId;
    private Integer skuId;
    private Integer quantity;
    @NotNull(message = "计算基数不能为空") @DecimalMin(value = "0", message = "计算基数不能小于0") private BigDecimal baseAmount;
    private BigDecimal realGrossProfit;
    private Boolean registeredCustomer;
    private Boolean voucherPresent;
    private Boolean audited;
}
