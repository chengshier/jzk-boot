package com.zbkj.service.service.jiuzhoukang.region;

import com.zbkj.common.response.jiuzhoukang.JkPromotionQrcodeResponse;
import com.zbkj.common.response.jiuzhoukang.JkTeamSummaryResponse;
import com.zbkj.common.response.jiuzhoukang.JkOptionResponse;
import java.util.List;

public interface JkAgentTeamService {
    JkTeamSummaryResponse summary(Long userId);
    JkPromotionQrcodeResponse promotionQrcode(Long userId);
    List<JkOptionResponse> eligibleParentOptions(Long userId, String keyword, int limit);
}
