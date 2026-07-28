package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/** 用户直属下级人数额度。 */
@Data
@Accessors(chain = true)
public class JkRelationQuotaResponse implements Serializable {
    private Long parentUserId;
    private String parentRoleCode;
    private String childRoleCode;
    private String regionCode;
    private Long ruleId;
    private String ruleCode;
    private String ruleName;
    private Integer maxDirectChildren;
    private Integer usedCount;
    private Integer reservedCount;
    private Integer remainingCount;
    private Integer warningThreshold;
    private String overflowPolicy;
    private Boolean warning;
    private Boolean full;
}
