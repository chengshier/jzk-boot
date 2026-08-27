package com.zbkj.service.service.jiuzhoukang.health;

import com.zbkj.common.request.jiuzhoukang.SinocareEnvelopeRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.jiuzhoukang.JkSinocareCallbackLogResponse;
import com.github.pagehelper.PageInfo;

/** Durable receipt boundary for Sinocare callbacks. Decryption is deliberately deferred from the HTTP response. */
public interface SinocareCallbackService {
    void receive(String eventType, SinocareEnvelopeRequest envelope);

    PageInfo<JkSinocareCallbackLogResponse> list(String eventType, String processStatus, String uniqueId, PageParamRequest page);

    JkSinocareCallbackLogResponse retry(Long id);
}
