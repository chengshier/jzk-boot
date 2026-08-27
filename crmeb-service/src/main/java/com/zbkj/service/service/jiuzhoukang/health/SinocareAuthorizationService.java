package com.zbkj.service.service.jiuzhoukang.health;
import com.zbkj.common.model.jiuzhoukang.JkSinocareAuthorization;
import com.zbkj.common.response.jiuzhoukang.JkSinocareAuthorizationPrepareResponse;

public interface SinocareAuthorizationService {
    JkSinocareAuthorization issueForUser(Long userId);

    JkSinocareAuthorizationPrepareResponse buildAuthorizationUrl(Long userId);
}
