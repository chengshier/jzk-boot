package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class JkWithdrawApplyRequest implements Serializable {
    private BigDecimal amount;
    private String requestNo;
    private String payeeSnapshotJson;
}
