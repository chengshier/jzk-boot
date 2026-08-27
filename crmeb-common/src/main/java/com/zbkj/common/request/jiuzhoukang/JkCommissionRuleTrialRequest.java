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
    /** 平台订货等场景的采购方快照。 */
    private Long purchaserUserId;
    private Long sellerUserId;
    /** 业绩/调拨等场景的归属人快照。 */
    private Long ownerUserId;
    private Long directParentUserId;
    private Long countyAgentUserId;
    private String regionCode;
    private Integer productId;
    private Integer skuId;
    private Integer quantity;
    @NotNull(message = "计算基数不能为空") @DecimalMin(value = "0", message = "计算基数不能小于0") private BigDecimal baseAmount;
    /** 真实成本快照；REAL_GROSS_PROFIT 规则只允许基于业务发生时成本计算。 */
    private BigDecimal costAmount;
    private BigDecimal realGrossProfit;
    private Boolean registeredCustomer;
    private Boolean voucherPresent;
    private Boolean audited;
    /** 业务发生时关系与来源快照，佣金入账后不得改读当前关系。 */
    private String relationSnapshotJson;
    private String sourceSnapshotJson;

    /**
     * 合并期兼容入口：正式佣金链路不再从请求 DTO 接收 ownerRoleCode，
     * 角色匹配由 JkCommissionV31Service 基于受益人/关系快照推导。
     * 遗留调用清理完成后应删除本方法，不新增冗余字段或数据库契约。
     */
    @Deprecated
    public JkCommissionRuleTrialRequest setOwnerRoleCode(String ignoredOwnerRoleCode) {
        return this;
    }
}
