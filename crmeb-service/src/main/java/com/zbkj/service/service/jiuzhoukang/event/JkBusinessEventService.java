package com.zbkj.service.service.jiuzhoukang.event;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkBusinessEvent;
import com.zbkj.common.request.PageParamRequest;

public interface JkBusinessEventService {
    JkBusinessEvent prepare(String eventKey, String eventType, Long businessId, String businessNo, String payloadJson);
    void executePrepared(Long eventId, Runnable action, Long operatorId);
    JkBusinessEvent retry(Long eventId, Long operatorId);
    int retryDue(int limit);
    PageInfo<JkBusinessEvent> list(String eventType, String eventStatus, PageParamRequest page);
}
