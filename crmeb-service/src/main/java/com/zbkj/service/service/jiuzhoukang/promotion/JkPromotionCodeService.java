package com.zbkj.service.service.jiuzhoukang.promotion;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkPromotionScene;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkPromotionSceneSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkPromotionCodeResponse;

import java.util.Map;

public interface JkPromotionCodeService {
    PageInfo<JkPromotionScene> listScenes(String keyword, Boolean status, PageParamRequest page);
    JkPromotionScene saveScene(JkPromotionSceneSaveRequest request, Long operatorId);
    JkPromotionCodeResponse generate(Long ownerUserId, String ownerRoleCode, String sceneCode, String requestNo);
    Map<String, Object> status();
}
