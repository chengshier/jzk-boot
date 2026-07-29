package com.zbkj.service.service.jiuzhoukang.profile;

import com.zbkj.common.request.jiuzhoukang.JkUserProfileRegionSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkUserProfileRegionResponse;

public interface JkUserProfileRegionService {
    JkUserProfileRegionResponse get(Long userId);

    JkUserProfileRegionResponse saveByUser(Long userId, JkUserProfileRegionSaveRequest request);

    JkUserProfileRegionResponse saveByAdmin(Long userId, Long adminId, String adminName,
                                            JkUserProfileRegionSaveRequest request);
}
