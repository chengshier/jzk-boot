package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 区域业务占用响应。
 */
@Data
@Accessors(chain = true)
public class JkRegionUsageResponse implements Serializable {
    /** 是否允许停用。 */
    private Boolean canDisable;
    /** 是否允许删除。 */
    private Boolean canDelete;
    /** 启用直属子区域数量。 */
    private Long activeChildCount;
    /** 区域代理绑定数量。 */
    private Long regionAgentCount;
    /** 用户业务身份数量。 */
    private Long userRoleCount;
    /** 用户数据范围数量。 */
    private Long dataScopeCount;
    /** 价格规则数量。 */
    private Long priceRuleCount;
    /** 库存账户数量。 */
    private Long stockAccountCount;
    /** 零售订单归属快照数量。 */
    private Long attributionCount;
    /** 业务记录总数。 */
    private Long businessRecordCount;
    /** 风险原因。 */
    private List<String> reasons;
}
