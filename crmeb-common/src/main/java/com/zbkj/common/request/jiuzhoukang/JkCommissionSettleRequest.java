package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import java.util.List;

@Data
public class JkCommissionSettleRequest {
    private List<Long> commissionRecordIds;
    private String requestNo;
    private String remark;
}