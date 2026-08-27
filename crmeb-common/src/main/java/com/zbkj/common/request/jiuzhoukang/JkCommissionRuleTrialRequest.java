package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Accessors(chain = true)
public class JkCommissionRuleTrialRequest {
    private Long ruleId;
    @NotBlank(message = "业务场景不能为空") private String scenario;
    @NotBlank(message = "来源类型不能为空") private String sourceType;
    private Long sourceId;
    private Long sourceItemId;
    private String sourceNo;
    /** 原业务发生时间，用于命中规则版本和周期封顶；不得用当前时间替代历史业务时间。 */
    private Date businessTime;
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
