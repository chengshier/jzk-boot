package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class JkStockCheckCreateRequest {
    @NotBlank(message = "requestNo不能为空") private String requestNo;
    @NotNull(message = "库存账户不能为空") private Long stockAccountId;
    /** V3.1 最终盘点范围；首版仅支持 ACCOUNT，预留后续按商品/SKU扩展。 */
    private String scopeType;
    /** 历史客户端兼容字段，不作为 V3.1 最终盘点口径。 */
    private String checkType;
    private String remark;
}
