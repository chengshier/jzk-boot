package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import java.util.Date;

@Data
public class JkRegionAgentResponse {
    private Long id;
    private String regionCode;
    private String regionName;
    private Long countyAgentUserId;
    private String countyAgentName;
    private String countyAgentPhone;
    private String bindStatus;
    private String bindStatusText;
    private Date effectiveTime;
    private Date expireTime;
    private String changeReason;
    private String remark;
    private Boolean status;
    private Date createTime;
}
