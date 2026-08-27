package com.zbkj.service.service.jiuzhoukang.offline;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkOfflineSale;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleCreateRequest;

public interface JkOfflineSaleService {
    JkOfflineSale create(Long sellerUserId, JkOfflineSaleCreateRequest request);
    PageInfo<JkOfflineSale> list(Long sellerUserId, String status, PageParamRequest page);
    JkOfflineSale detail(Long viewerUserId, Long id, boolean admin);
    JkOfflineSale audit(Long operatorId, JkOfflineSaleAuditRequest request);
    JkOfflineSale cancel(Long sellerUserId, Long id, JkOfflineSaleActionRequest request);
    JkOfflineSale returnSale(Long sellerUserId, Long id, JkOfflineSaleActionRequest request);
}
