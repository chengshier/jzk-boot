package com.zbkj.service.service.jiuzhoukang.scope;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkIdentityApply;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;

/**
 * 后台九州康业务行级数据范围。
 *
 * 平台全量角色可访问全部数据；区县代仅允许访问本人下级或本区域的创客/合伙人；
 * 其他后台业务身份默认不授予身份治理数据，避免仅凭菜单权限越权读取全局记录。
 */
public interface JkAdminDataScopeService {

    void applyIdentityApplyScope(LambdaQueryWrapper<JkIdentityApply> wrapper);

    void assertCanManageIdentityApply(JkIdentityApply apply);

    void applyUserBusinessRoleScope(LambdaQueryWrapper<JkUserBusinessRole> wrapper);

    void assertCanManageUserBusinessRole(JkUserBusinessRole role);

    void applyAuditLogScope(LambdaQueryWrapper<JkAuditLog> wrapper);
}
