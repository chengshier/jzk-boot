package com.zbkj.service.service.jiuzhoukang.identity;

import com.zbkj.common.model.jiuzhoukang.JkIdentityApply;

public interface IdentityEffectiveService {
    void effectiveIdentity(JkIdentityApply apply, Long auditUserId, String auditUserName, String auditRemark);
    void initStockAccountPlaceholder(Long userId, String roleCode);
    void initCommissionAccountPlaceholder(Long userId, String roleCode);
    void initPromotionCodePlaceholder(Long userId, String roleCode);
}
