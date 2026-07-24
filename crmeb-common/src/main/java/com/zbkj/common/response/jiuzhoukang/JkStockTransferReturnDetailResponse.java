package com.zbkj.common.response.jiuzhoukang;

import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferReturn;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferReturnItem;
import lombok.Data;
import java.util.List;

@Data
public class JkStockTransferReturnDetailResponse {
    private JkStockTransferReturn returnOrder;
    private List<JkStockTransferReturnItem> items;
    private List<JkAuditLogResponse> auditLogs;
}
