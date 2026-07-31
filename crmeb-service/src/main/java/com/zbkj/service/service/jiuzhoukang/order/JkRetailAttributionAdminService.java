package com.zbkj.service.service.jiuzhoukang.order;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttribution;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttributionAdjustment;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkRetailAttributionResolveRequest;

import java.util.List;
import java.util.Map;

public interface JkRetailAttributionAdminService {
    PageInfo<JkRetailOrderAttribution> list(String orderNo, Long buyerUserId, String regionSourceType,
                                            String attributionStatus, PageParamRequest page);
    Map<String, Object> detail(Long id);
    Map<String, Object> overview(Long id);
    JkRetailOrderAttribution resolve(Long id, Long operatorId, JkRetailAttributionResolveRequest request);
    JkRetailOrderAttributionAdjustment adjust(Long id, Long operatorId, JkRetailAttributionResolveRequest request);
    List<JkRetailOrderAttributionAdjustment> audit(Long id);
}
