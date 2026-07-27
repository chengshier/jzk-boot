package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import java.util.Date;

@Data
public class JkBusinessActionRequest {
    private Long businessId;
    private String remark;
    /** 发货/拨货物流公司；非物流动作可为空。 */
    private String logisticsCompany;
    /** 发货/拨货物流单号；非物流动作可为空。 */
    private String logisticsNo;
    /** 实际发货时间；为空时由服务端使用当前时间。 */
    private Date shippingTime;
}
