package com.zbkj.service.service.jiuzhoukang.risk;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkRiskEvent;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkRiskHandleRequest;

/** 第六阶段风险事件服务。风险发现与业务改账解耦，默认只记录、提示和人工处置。 */
public interface JkRiskService {
    JkRiskEvent record(String riskType, String riskLevel, String sourceType, Long sourceId, String sourceNo, Long userId, String summary, String detailJson);
    /** 使用稳定幂等键记录风险；同一风险在同一统计周期只生成一条。 */
    JkRiskEvent recordOnce(String idempotencyKey, String riskType, String riskLevel, String sourceType, Long sourceId, String sourceNo, Long userId, String summary, String detailJson);
    PageInfo<JkRiskEvent> list(String riskType, String riskLevel, String status, PageParamRequest page);
    JkRiskEvent handle(Long adminUserId, JkRiskHandleRequest request);
}
