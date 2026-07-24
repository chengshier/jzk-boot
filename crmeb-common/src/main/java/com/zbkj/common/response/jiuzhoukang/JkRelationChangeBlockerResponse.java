package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

/** 换绑审核阻断项结构化结果。 */
@Data
@Accessors(chain = true)
public class JkRelationChangeBlockerResponse {
    /** 稳定阻断编码，供 Admin/App 展示与测试断言。 */
    private String code;
    private String label;
    private Boolean blocked;
    /** 可读数值，例如未完成调拨单数、库存数量或待结算金额。 */
    private String value;
    private String unit;
    private String description;
    private String actionHint;
}
