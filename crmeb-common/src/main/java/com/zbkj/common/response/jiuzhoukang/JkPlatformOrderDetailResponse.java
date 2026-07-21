package com.zbkj.common.response.jiuzhoukang;

import com.zbkj.common.model.jiuzhoukang.JkOfflinePaymentVoucher;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrderItem;
import lombok.Data;

import java.util.List;

@Data
public class JkPlatformOrderDetailResponse {
    private JkPlatformOrder order;
    private List<JkPlatformOrderItem> items;
    private List<JkOfflinePaymentVoucher> vouchers;
    private List<JkAuditLogResponse> auditLogs;
}
