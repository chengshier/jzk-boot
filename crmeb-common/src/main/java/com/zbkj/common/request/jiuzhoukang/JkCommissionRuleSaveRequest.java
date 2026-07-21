package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class JkCommissionRuleSaveRequest implements Serializable {
    private Long id;
    private String ruleName;
    private Integer ruleVersion;
    private String sourceType;
    private String receiverRoleCode;
    private String regionCode;
    private Date effectiveTime;
    private Date expireTime;
    private Integer freezeDays;
    private String ruleConfigJson;
    private String remark;
}
