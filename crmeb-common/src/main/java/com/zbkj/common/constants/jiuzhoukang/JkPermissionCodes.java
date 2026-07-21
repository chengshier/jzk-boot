package com.zbkj.common.constants.jiuzhoukang;

/**
 * 九州康 admin 菜单与按钮 authority 常量。
 * 对应 {@code eb_system_menu.perms}，用于后台菜单、按钮与接口 authority 收口。
 */
public final class JkPermissionCodes {

    /** 身份申请审核列表页。 */
    public static final String ADMIN_IDENTITY_APPLY_LIST = "admin:jk:identity:apply:list";
    /** 身份申请审核操作。 */
    public static final String ADMIN_IDENTITY_APPLY_AUDIT = "admin:jk:identity:apply:audit";
    /** 价格规则管理列表页。 */
    public static final String ADMIN_PRICE_RULE_LIST = "admin:jk:price:rule:list";
    /** 价格规则保存。 */
    public static final String ADMIN_PRICE_RULE_SAVE = "admin:jk:price:rule:save";
    /** 价格规则启停。 */
    public static final String ADMIN_PRICE_RULE_STATUS = "admin:jk:price:rule:status";
    /** 库存账户列表。 */
    public static final String ADMIN_STOCK_ACCOUNT_LIST = "admin:jk:stock:account:list";
    /** 库存明细列表。 */
    public static final String ADMIN_STOCK_ITEM_LIST = "admin:jk:stock:item:list";
    /** 库存流水列表。 */
    public static final String ADMIN_STOCK_FLOW_LIST = "admin:jk:stock:flow:list";
    /** 平台订货列表。 */
    public static final String ADMIN_PLATFORM_ORDER_LIST = "admin:jk:platform:order:list";
    /** 平台订货付款审核。 */
    public static final String ADMIN_PLATFORM_ORDER_AUDIT = "admin:jk:platform:order:audit";
    /** 平台订货发货。 */
    public static final String ADMIN_PLATFORM_ORDER_SHIP = "admin:jk:platform:order:ship";
    /** 平台订货关闭。 */
    public static final String ADMIN_PLATFORM_ORDER_CLOSE = "admin:jk:platform:order:close";
    /** 调拨列表。 */
    public static final String ADMIN_STOCK_TRANSFER_LIST = "admin:jk:stock:transfer:list";
    /** 调拨审核。 */
    public static final String ADMIN_STOCK_TRANSFER_AUDIT = "admin:jk:stock:transfer:audit";
    /** 调拨付款确认。 */
    public static final String ADMIN_STOCK_TRANSFER_PAYMENT = "admin:jk:stock:transfer:payment";
    /** 调拨拨货。 */
    public static final String ADMIN_STOCK_TRANSFER_DISPATCH = "admin:jk:stock:transfer:dispatch";
    /** 调拨关闭。 */
    public static final String ADMIN_STOCK_TRANSFER_CLOSE = "admin:jk:stock:transfer:close";
    /** 佣金账户列表。 */
    public static final String ADMIN_COMMISSION_ACCOUNT_LIST = "admin:jk:commission:account:list";
    /** 分佣规则列表。 */
    public static final String ADMIN_COMMISSION_RULE_LIST = "admin:jk:commission:rule:list";
    /** 分佣规则保存。 */
    public static final String ADMIN_COMMISSION_RULE_SAVE = "admin:jk:commission:rule:save";
    /** 分佣规则启停。 */
    public static final String ADMIN_COMMISSION_RULE_STATUS = "admin:jk:commission:rule:status";
    /** 佣金明细列表。 */
    public static final String ADMIN_COMMISSION_RECORD_LIST = "admin:jk:commission:record:list";
    /** 佣金冲正记录列表。 */
    public static final String ADMIN_COMMISSION_REVERSE_LIST = "admin:jk:commission:reverse:list";
    /** 手动佣金冲正。 */
    public static final String ADMIN_COMMISSION_REVERSE_MANUAL = "admin:jk:commission:reverse:manual";
    /** 结算任务列表。 */
    public static final String ADMIN_COMMISSION_SETTLE_LIST = "admin:jk:commission:settle:list";
    /** 手动结算。 */
    public static final String ADMIN_COMMISSION_SETTLE_MANUAL = "admin:jk:commission:settle:manual";
    /** 资金账户列表。 */
    public static final String ADMIN_FUND_ACCOUNT_LIST = "admin:jk:fund:account:list";
    /** 资金流水列表。 */
    public static final String ADMIN_FUND_FLOW_LIST = "admin:jk:fund:flow:list";
    /** 提现审核列表。 */
    public static final String ADMIN_WITHDRAW_LIST = "admin:jk:withdraw:list";
    /** 提现审核操作。 */
    public static final String ADMIN_WITHDRAW_AUDIT = "admin:jk:withdraw:audit";
    /** 确认线下打款。 */
    public static final String ADMIN_WITHDRAW_CONFIRM_PAID = "admin:jk:withdraw:confirmPaid";
    /** 审核日志列表。 */
    public static final String ADMIN_AUDIT_LOG_LIST = "admin:jk:audit:log:list";
    /** 业务角色列表。 */
    public static final String ADMIN_BUSINESS_ROLE_LIST = "admin:jk:business:role:list";
    /** 业务角色状态更新。 */
    public static final String ADMIN_BUSINESS_ROLE_STATUS = "admin:jk:business:role:update:status";
    /** 业务权限点列表。 */
    public static final String ADMIN_BUSINESS_PERMISSION_LIST = "admin:jk:business:permission:list";
    /** 用户业务身份列表。 */
    public static final String ADMIN_USER_BUSINESS_ROLE_LIST = "admin:jk:user:business:role:list";
    /** 身份冻结。 */
    public static final String ADMIN_IDENTITY_FREEZE = "admin:jk:identity:freeze";
    /** 身份解冻。 */
    public static final String ADMIN_IDENTITY_UNFREEZE = "admin:jk:identity:unfreeze";
    /** 身份取消。 */
    public static final String ADMIN_IDENTITY_CANCEL = "admin:jk:identity:cancel";

    private JkPermissionCodes() {
    }
}
