package com.zbkj.service.service.jiuzhoukang.commission;

import com.zbkj.common.model.jiuzhoukang.JkWithdrawApply;
import java.math.BigDecimal;

public interface WithdrawService {
    JkWithdrawApply apply(Long userId, String roleCode, BigDecimal amount, String requestNo, String payeeSnapshotJson);
    JkWithdrawApply audit(Long withdrawId, Long operatorId, boolean approved, String requestNo, String remark);
    JkWithdrawApply confirmPaid(Long withdrawId, Long operatorId, String requestNo, String remark);
}
