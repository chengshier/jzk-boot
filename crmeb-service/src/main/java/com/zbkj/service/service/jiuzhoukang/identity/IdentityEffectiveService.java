package com.zbkj.service.service.jiuzhoukang.identity;

import com.zbkj.common.model.jiuzhoukang.JkIdentityApply;

/**
 * 身份生效编排服务。
 *
 * <p>身份审核通过后统一完成角色绑定、数据范围和业务账户初始化。
 * 推广二维码继续复用 CRMEB 原推广码链路，扫码绑定时同步写入九州康关系表，
 * 因此这里不再保留空的“推广码初始化占位方法”。</p>
 */
public interface IdentityEffectiveService {

    void effectiveIdentity(JkIdentityApply apply, Long auditUserId, String auditUserName, String auditRemark);
}
