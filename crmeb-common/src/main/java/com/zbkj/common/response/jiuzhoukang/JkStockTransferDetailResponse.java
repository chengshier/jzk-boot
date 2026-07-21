package com.zbkj.common.response.jiuzhoukang;

import com.zbkj.common.model.jiuzhoukang.JkOfflinePaymentVoucher;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferItem;
import lombok.Data;

import java.util.List;

@Data
public class JkStockTransferDetailResponse {
    private JkStockTransfer transfer;
    private List<JkStockTransferItem> items;
    private List<JkOfflinePaymentVoucher> vouchers;
    private List<JkAuditLogResponse> auditLogs;
}
