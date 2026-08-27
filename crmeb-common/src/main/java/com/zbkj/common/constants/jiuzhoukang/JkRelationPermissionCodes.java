package com.zbkj.common.constants.jiuzhoukang;

/**
 * 九州康 V3.1 关系人数规则与管理员强制调整后台权限常量。
 */
public final class JkRelationPermissionCodes {
    /** 关系人数规则。 */
    public static final String ADMIN_RELATION_LIMIT_RULE_LIST = "admin:jk:relation:limit:rule:list";
    public static final String ADMIN_RELATION_LIMIT_RULE_SAVE = "admin:jk:relation:limit:rule:save";
    public static final String ADMIN_RELATION_LIMIT_RULE_STATUS = "admin:jk:relation:limit:rule:status";

    /** 上下级关系调整。 */
    public static final String ADMIN_AGENT_RELATION_FORCE_ADJUST = "admin:jk:agent-relation:force-adjust";

    private JkRelationPermissionCodes() {
    }
}
