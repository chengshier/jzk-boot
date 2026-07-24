package com.zbkj.service.service.jiuzhoukang.region;

import com.zbkj.common.request.jiuzhoukang.JkAdminUserMappingSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkAdminUserMappingResponse;
import java.util.List;

public interface JkAdminUserMappingService {
    List<JkAdminUserMappingResponse> list(Integer systemAdminId, Long frontUserId);
    JkAdminUserMappingResponse save(JkAdminUserMappingSaveRequest request, Long operatorId);
    boolean updateStatus(Long id, boolean status, Long operatorId);
}
