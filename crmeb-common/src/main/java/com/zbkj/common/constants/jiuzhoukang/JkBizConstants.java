package com.zbkj.common.constants.jiuzhoukang;

public class JkBizConstants {

    public static final String ROLE_NORMAL_USER = "normal_user";
    public static final String ROLE_MAKER = "maker";
    public static final String ROLE_PARTNER = "partner";
    public static final String ROLE_COUNTY_AGENT = "county_agent";
    public static final String ROLE_PLATFORM_ADMIN = "platform_admin";

    public static final String AUDIT_STATUS_PENDING = "PENDING";
    public static final String AUDIT_STATUS_EFFECTIVE = "EFFECTIVE";
    public static final String AUDIT_STATUS_FROZEN = "FROZEN";
    public static final String AUDIT_STATUS_CANCELLED = "CANCELLED";
    public static final String AUDIT_STATUS_REJECTED = "REJECTED";

    public static final String EFFECTIVE_STATUS_ENABLED = "ENABLED";
    public static final String EFFECTIVE_STATUS_DISABLED = "DISABLED";

    public static final String AUDIT_ACTION_PASS = "PASS";
    public static final String AUDIT_ACTION_REJECT = "REJECT";
    public static final String AUDIT_ACTION_FREEZE = "FREEZE";
    public static final String AUDIT_ACTION_UNFREEZE = "UNFREEZE";
    public static final String AUDIT_ACTION_CANCEL = "CANCEL";

    public static final String OPERATE_SOURCE_APP = "APP";
    public static final String OPERATE_SOURCE_ADMIN = "ADMIN";
    public static final String OPERATE_SOURCE_SYSTEM = "SYSTEM";

    public static final String BUSINESS_TYPE_IDENTITY_APPLY = "IDENTITY_APPLY";
    public static final String BUSINESS_TYPE_PRICE_RULE = "PRICE_RULE";
    public static final String BUSINESS_TYPE_STOCK = "STOCK";

    public static final String SCOPE_SELF = "SELF";
    public static final String SCOPE_DIRECT_TEAM = "DIRECT_TEAM";
    public static final String SCOPE_TEAM = "TEAM";
    public static final String SCOPE_REGION_SELF = "REGION_SELF";
    public static final String SCOPE_PLATFORM_ALL = "PLATFORM_ALL";

    public static final String CACHE_CHANGE_ROLE_PERMISSION = "ROLE_PERMISSION";
    public static final String CACHE_CHANGE_USER_ROLE = "USER_ROLE";
    public static final String CACHE_CHANGE_IDENTITY_STATUS = "IDENTITY_STATUS";
    public static final String CACHE_CHANGE_DATA_SCOPE = "DATA_SCOPE";

    public static final String REDIS_CONTEXT_KEY_PREFIX = "jk:permission:context:";

    /** 九洲康推广素材 JSON 配置键。 */
    public static final String CONFIG_KEY_PROMOTION_MATERIALS = "jk_promotion_materials_json";

    public static final String PERMISSION_PRICE_RULE_CONFIG = "price.rule.config";
    public static final String PERMISSION_STOCK_VIEW_SELF = "stock.view.self";
    public static final String PERMISSION_STOCK_VIEW_REGION = "stock.view.region";
    public static final String PERMISSION_STOCK_APPLY = "stock.apply";
    public static final String PERMISSION_STOCK_PLATFORM_ORDER = "stock.platform.order";
    public static final String PERMISSION_STOCK_TRANSFER_CONFIRM = "stock.transfer.confirm";
    public static final String PERMISSION_STOCK_FLOW_VIEW = "stock.flow.view";

    public static final String PRICE_TYPE_FIXED = "FIXED";
    public static final String PRICE_TYPE_DISCOUNT = "DISCOUNT";
    public static final String PRICE_TYPE_CRMEB_MEMBER = "CRMEB_MEMBER_PRICE";
    public static final String PRICE_TYPE_CRMEB_RETAIL = "CRMEB_RETAIL_PRICE";

    public static final String PRICE_MATCH_LEVEL_USER = "USER";
    public static final String PRICE_MATCH_LEVEL_REGION_ROLE = "REGION_ROLE";
    public static final String PRICE_MATCH_LEVEL_ROLE = "ROLE";
    public static final String PRICE_MATCH_LEVEL_ACTIVITY = "ACTIVITY";

    public static final String STOCK_SOURCE_RETAIL = "RETAIL_STOCK";
    public static final String STOCK_SOURCE_COUNTY_ALLOCATABLE = "COUNTY_AGENT_ALLOCATABLE";
    public static final String STOCK_SOURCE_PLATFORM_ORDERABLE = "PLATFORM_ORDERABLE";
    public static final String STOCK_SOURCE_OWN = "OWN_STOCK";

    public static final String STOCK_ACCOUNT_PLATFORM = "PLATFORM";
    public static final String STOCK_ACCOUNT_RETAIL = "RETAIL";
    public static final String STOCK_ACCOUNT_COUNTY_AGENT = "COUNTY_AGENT";
    public static final String STOCK_ACCOUNT_PARTNER = "PARTNER";
    public static final String STOCK_ACCOUNT_MAKER = "MAKER";

    public static final String STOCK_FLOW_TYPE_INIT = "INIT";
    public static final String STOCK_FLOW_TYPE_TEST_INIT = "TEST_INIT";

    private JkBizConstants() {
    }
}
