package com.zbkj.service.service.jiuzhoukang.commission;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkAccountReconcileRecord;
import com.zbkj.common.request.PageParamRequest;
import java.util.List;

public interface AccountReconcileService {
    List<JkAccountReconcileRecord> reconcile(Long userId, String roleCode, Long operatorId, String batchNo);
    PageInfo<JkAccountReconcileRecord> list(String batchNo, String status, Long userId, PageParamRequest page);
}
