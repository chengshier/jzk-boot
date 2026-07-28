package com.zbkj.service.service.jiuzhoukang.stock;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkStockCheck;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckCountRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckCreateRequest;

public interface JkStockCheckService {
    JkStockCheck create(Long operatorId, JkStockCheckCreateRequest request, boolean admin);
    JkStockCheck count(Long operatorId, Long checkId, JkStockCheckCountRequest request, boolean admin);
    JkStockCheck submit(Long operatorId, Long checkId, JkStockCheckActionRequest request, boolean admin);
    JkStockCheck audit(Long operatorId, JkStockCheckAuditRequest request);
    JkStockCheck detail(Long viewerUserId, Long id, boolean admin);
    PageInfo<JkStockCheck> list(Long ownerUserId, String status, PageParamRequest page);
}
