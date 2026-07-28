package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class JkCommissionRuleTrialRequest {
    private Long ruleId;
    @NotNull private String sourceType;
    private Long sourceId;
    private Long sourceItemId;
    private String sourceNo;
    @NotNull private Long ownerUserId;
    private String ownerRoleCode;
    private Long directParentUserId;
    private Long countyAgentUserId;
    private Long sellerUserId;
    private Long purchaserUserId;
    private String regionCode;
    private Integer productId;
    private Integer skuId;
    private Integer quantity;
    @NotNull @DecimalMin("0.00") private BigDecimal baseAmount;
    private BigDecimal costAmount;
    private Boolean registeredCustomer;
    private Boolean voucherPresent;
    private Boolean audited;
    private String relationSnapshotJson;
    private String sourceSnapshotJson;
}
