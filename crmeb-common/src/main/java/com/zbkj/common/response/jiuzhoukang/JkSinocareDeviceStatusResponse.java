package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 前台设备与授权页所需的三诺设备同步状态。 */
@Data
@Accessors(chain = true)
public class JkSinocareDeviceStatusResponse {
    private Boolean authorized;
    private Boolean hasGlucoseData;
    private String productName;
    private String deviceSn;
    private Integer status;
    private Date detectionStartTime;
    private Date detectionEndTime;
    private Date lastDataAt;
}
