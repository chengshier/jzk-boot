package com.zbkj.service.service.jiuzhoukang.region;

import com.zbkj.common.model.jiuzhoukang.JkRegion;
import com.zbkj.common.request.jiuzhoukang.JkRegionSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkRegionOptionResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionPathResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionSearchResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionTreeNodeResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionUsageResponse;

import java.util.List;

public interface JkRegionService {
    List<JkRegion> list(String keywords, Boolean status);
    List<JkRegionTreeNodeResponse> listChildren(String parentRegionCode, Boolean enabled);
    List<JkRegionSearchResponse> searchRegions(String keyword, Integer regionLevel, Boolean status, Integer limit);
    JkRegionPathResponse getRegionPath(String regionCode);
    List<JkRegionOptionResponse> listRegionOptions(String parentRegionCode, Integer targetLevel, Boolean enabled, String keyword);
    JkRegionUsageResponse getRegionUsage(String regionCode);
    JkRegion save(JkRegionSaveRequest request, Long operatorId);
    boolean updateStatus(Long id, boolean status, Long operatorId);
}
