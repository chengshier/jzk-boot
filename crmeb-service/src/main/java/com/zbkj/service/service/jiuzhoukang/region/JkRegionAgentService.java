package com.zbkj.service.service.jiuzhoukang.region;

import com.zbkj.common.request.jiuzhoukang.JkRegionAgentBindRequest;
import com.zbkj.common.response.jiuzhoukang.JkRegionAgentResponse;
import java.util.List;

public interface JkRegionAgentService {
    List<JkRegionAgentResponse> list(String regionCode, Long countyAgentUserId, Boolean activeOnly);
    JkRegionAgentResponse bind(JkRegionAgentBindRequest request, Long operatorId);
    boolean invalidate(Long id, String reason, Long operatorId);
}
