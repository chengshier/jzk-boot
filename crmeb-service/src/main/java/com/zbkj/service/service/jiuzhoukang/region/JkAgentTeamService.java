package com.zbkj.service.service.jiuzhoukang.region;

import com.zbkj.common.response.jiuzhoukang.JkOptionResponse;
import com.zbkj.common.response.jiuzhoukang.JkPromotionQrcodeResponse;
import com.zbkj.common.response.jiuzhoukang.JkTeamSummaryResponse;

import java.util.List;
import java.util.Map;

public interface JkAgentTeamService {
    JkTeamSummaryResponse summary(Long userId);

    /**
     * 查询当前用户直属团队成员详情。
     *
     * <p>仅允许读取当前用户直属下级，避免通过用户 ID 越权查看其他代理的资料。
     * 返回关系、身份和团队统计等真实数据；当前无法从佣金表准确反推“成员为上级贡献的佣金”时，
     * contributionAvailable=false，禁止生成模拟贡献记录。</p>
     */
    Map<String, Object> memberDetail(Long currentUserId, Long memberUserId);

    JkPromotionQrcodeResponse promotionQrcode(Long userId);

    List<JkOptionResponse> eligibleParentOptions(Long userId, String keyword, int limit);
}
