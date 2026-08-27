package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/** 创建异常收货 V2 处理方案。 */
@Data
public class JkReceiveExceptionResolutionCreateRequest {
    @NotNull(message = "异常单ID不能为空") private Long exceptionId;
    /** RESHIP、REFUND、RETURN、ACCEPT、MIXED。 */
    @NotBlank(message = "处理类型不能为空") private String resolutionType;
    @NotBlank(message = "requestNo不能为空") private String requestNo;
    private String responsibilityParty;
    @DecimalMin(value = "0.00", message = "退款金额不能小于0") private BigDecimal refundAmount;
    @DecimalMin(value = "0.00", message = "索赔金额不能小于0") private BigDecimal claimAmount;
    private List<String> evidenceUrls;
    @NotBlank(message = "处理说明不能为空") private String remark;
    @Valid
    @NotEmpty(message = "处理明细不能为空") private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "异常商品明细ID不能为空") private Long exceptionItemId;
        @Min(value = 0, message = "接受数量不能小于0") private Integer acceptedQty;
        @Min(value = 0, message = "补发数量不能小于0") private Integer reshipQty;
        @Min(value = 0, message = "退款数量不能小于0") private Integer refundQty;
        @Min(value = 0, message = "退回数量不能小于0") private Integer returnQty;
        private String logisticsCompany;
        private String logisticsNo;
        private String itemRemark;
    }
}
