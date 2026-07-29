package com.zbkj.service.service.jiuzhoukang.trade;

import com.zbkj.common.model.jiuzhoukang.JkReceiveExceptionResolution;
import com.zbkj.common.request.jiuzhoukang.JkReceiveExceptionResolutionActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkReceiveExceptionResolutionCreateRequest;

import java.util.List;

public interface JkReceiveExceptionResolutionService {
    JkReceiveExceptionResolution create(Long operatorUserId, JkReceiveExceptionResolutionCreateRequest request);
    JkReceiveExceptionResolution complete(Long operatorUserId, JkReceiveExceptionResolutionActionRequest request);
    JkReceiveExceptionResolution cancel(Long operatorUserId, JkReceiveExceptionResolutionActionRequest request);
    List<JkReceiveExceptionResolution> list(Long exceptionId);
}
