package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

@Data
public class JkAccountReconcileRequest {
    private Long userId;
    private String roleCode;
    private String requestNo;
}
