package com.zbkj.common.constants.jiuzhoukang;

/** V3.1 第二、第三批及补漏新增后台 authority。 */
public final class JkV31PermissionCodes {
    public static final String ADMIN_COMMISSION_RULE_TRIAL = "admin:jk:commission:rule:trial";
    public static final String ADMIN_COMMISSION_RULE_PUBLISH = "admin:jk:commission:rule:publish";
    /** 仅高级规则管理员可见的底层技术枚举接口，普通运营不授权。 */
    public static final String ADMIN_COMMISSION_RULE_ADVANCED = "admin:jk:commission:rule:advanced";
    public static final String ADMIN_PERFORMANCE_LIST = "admin:jk:performance:list";
    public static final String ADMIN_OPERATION_PROFIT_LIST = "admin:jk:operation:profit:list";
    public static final String ADMIN_OFFLINE_SALE_LIST = "admin:jk:offline:sale:list";
    public static final String ADMIN_OFFLINE_SALE_AUDIT = "admin:jk:offline:sale:audit";
    public static final String ADMIN_STOCK_CHECK_LIST = "admin:jk:stock:check:list";
    public static final String ADMIN_STOCK_CHECK_AUDIT = "admin:jk:stock:check:audit";
    public static final String ADMIN_PROMOTION_SCENE_MANAGE = "admin:jk:promotion:scene:manage";
    public static final String ADMIN_SUBSCRIPTION_TASK_LIST = "admin:jk:subscription:task:list";
    public static final String ADMIN_SUBSCRIPTION_TASK_MANAGE = "admin:jk:subscription:task:manage";
    public static final String ADMIN_HEALTH_REPORT_LIST = "admin:jk:health:report:list";

    public static final String ADMIN_USER_PROFILE_REGION_VIEW = "admin:jk:user:profile:region:view";
    public static final String ADMIN_USER_PROFILE_REGION_EDIT = "admin:jk:user:profile:region:edit";
    public static final String ADMIN_RETAIL_ATTRIBUTION_LIST = "admin:jk:retail:attribution:list";
    public static final String ADMIN_RETAIL_ATTRIBUTION_DETAIL = "admin:jk:retail:attribution:detail";
    public static final String ADMIN_RETAIL_ATTRIBUTION_RESOLVE = "admin:jk:retail:attribution:resolve";
    public static final String ADMIN_RETAIL_ATTRIBUTION_ADJUST = "admin:jk:retail:attribution:adjust";
    public static final String ADMIN_RETAIL_ATTRIBUTION_EXPORT = "admin:jk:retail:attribution:export";

    public static final String ADMIN_BUSINESS_PLAN_LIST = "admin:jk:business:plan:list";
    public static final String ADMIN_BUSINESS_PLAN_EDIT = "admin:jk:business:plan:edit";
    public static final String ADMIN_BUSINESS_PLAN_PUBLISH = "admin:jk:business:plan:publish";
    public static final String ADMIN_BUSINESS_PLAN_DISABLE = "admin:jk:business:plan:disable";

    private JkV31PermissionCodes() { }
}
