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
    /** 佣金流水列表。 */
    public static final String ADMIN_COMMISSION_FLOW_LIST = "admin:jk:commission:flow:list";
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
    /** 业务角色保存。 */
    public static final String ADMIN_BUSINESS_ROLE_SAVE = "admin:jk:business:role:save";
    /** 业务角色授权。 */
    public static final String ADMIN_BUSINESS_ROLE_PERMISSION = "admin:jk:business:role:permission";
    /** 业务角色状态更新。 */
    public static final String ADMIN_BUSINESS_ROLE_STATUS = "admin:jk:business:role:update:status";
    /** 业务权限点列表。 */
    public static final String ADMIN_BUSINESS_PERMISSION_LIST = "admin:jk:business:permission:list";
    /** 业务权限点保存。 */
    public static final String ADMIN_BUSINESS_PERMISSION_SAVE = "admin:jk:business:permission:save";
    /** 业务权限点状态更新。 */
    public static final String ADMIN_BUSINESS_PERMISSION_STATUS = "admin:jk:business:permission:status";
    /** 用户业务身份列表。 */
    public static final String ADMIN_USER_BUSINESS_ROLE_LIST = "admin:jk:user:business:role:list";
    /** 身份冻结。 */
    public static final String ADMIN_IDENTITY_FREEZE = "admin:jk:identity:freeze";
    /** 身份解冻。 */
    public static final String ADMIN_IDENTITY_UNFREEZE = "admin:jk:identity:unfreeze";
    /** 身份取消。 */
    public static final String ADMIN_IDENTITY_CANCEL = "admin:jk:identity:cancel";


    /** 区域管理。 */
    public static final String ADMIN_REGION_MANAGE = "admin:jk:region:manage";
    /** 区域代理管理。 */
    public static final String ADMIN_REGION_AGENT_MANAGE = "admin:jk:region-agent:manage";
    /** 上下级关系管理。 */
    public static final String ADMIN_AGENT_RELATION_MANAGE = "admin:jk:agent-relation:manage";
    /** 后台业务用户映射管理。 */
    public static final String ADMIN_MAPPING_MANAGE = "admin:jk:admin-mapping:manage";
    /** 动态字典管理。 */
    public static final String ADMIN_DICT_MANAGE = "admin:jk:dict:manage";

    /** 业务事件补偿列表。 */
    public static final String ADMIN_BUSINESS_EVENT_LIST = "admin:jk:business:event:list";
    /** 业务事件手动重试。 */
    public static final String ADMIN_BUSINESS_EVENT_RETRY = "admin:jk:business:event:retry";
    /** 自动结算执行。 */
    public static final String ADMIN_COMMISSION_SETTLE_AUTO = "admin:jk:commission:settle:auto";
    /** 账户对账列表。 */
    public static final String ADMIN_ACCOUNT_RECONCILE_LIST = "admin:jk:account:reconcile:list";
    /** 执行账户对账。 */
    public static final String ADMIN_ACCOUNT_RECONCILE_RUN = "admin:jk:account:reconcile:run";
    /** 换绑申请列表。 */
    public static final String ADMIN_AGENT_RELATION_CHANGE_LIST = "admin:jk:agent-relation:change:list";
    /** 换绑申请审核。 */
    public static final String ADMIN_AGENT_RELATION_CHANGE_AUDIT = "admin:jk:agent-relation:change:audit";
    /** 调拨退回列表。 */
    public static final String ADMIN_STOCK_TRANSFER_RETURN_LIST = "admin:jk:stock:transfer:return:list";
    /** 调拨退回审核。 */
    public static final String ADMIN_STOCK_TRANSFER_RETURN_AUDIT = "admin:jk:stock:transfer:return:audit";
    /** 调拨退回收货。 */
    public static final String ADMIN_STOCK_TRANSFER_RETURN_RECEIVE = "admin:jk:stock:transfer:return:receive";
    /** 调拨退回退款确认。 */
    public static final String ADMIN_STOCK_TRANSFER_RETURN_REFUND = "admin:jk:stock:transfer:return:refund";
    /** 调拨退回关闭。 */
    public static final String ADMIN_STOCK_TRANSFER_RETURN_CLOSE = "admin:jk:stock:transfer:return:close";
    /** 九州康后台通用选择器。 */
    public static final String ADMIN_MANAGEMENT_OPTION_LIST = "admin:jk:management:option:list";


    /** 健康设备管理。 */
    public static final String ADMIN_HEALTH_DEVICE_MANAGE = "admin:jk:health:device:manage";
    /** 健康授权查询。 */
    public static final String ADMIN_HEALTH_AUTH_LIST = "admin:jk:health:auth:list";
    /** 健康预警规则管理。 */
    public static final String ADMIN_HEALTH_ALERT_RULE_MANAGE = "admin:jk:health:alert:rule:manage";
    /** 健康预警记录处理。 */
    public static final String ADMIN_HEALTH_ALERT_RECORD_MANAGE = "admin:jk:health:alert:record:manage";
    /** 健康数据访问日志。 */
    public static final String ADMIN_HEALTH_ACCESS_LOG_LIST = "admin:jk:health:access-log:list";
    public static final String ADMIN_HEALTH_BIND_LIST = "admin:jk:health:bind:list";
    public static final String ADMIN_HEALTH_DATA_LIST = "admin:jk:health:data:list";
    public static final String ADMIN_HEALTH_DATA_EXPORT = "admin:jk:health:data:export";
    public static final String ADMIN_HEALTH_SYNC_LIST = "admin:jk:health:sync:list";
    public static final String ADMIN_HEALTH_SYNC_RETRY = "admin:jk:health:sync:retry";
    public static final String ADMIN_HEALTH_INTEGRATION_STATUS = "admin:jk:health:integration:status";
    /** 健康厂商双模式接入配置。 */
    public static final String ADMIN_HEALTH_PROVIDER_MANAGE = "admin:jk:health:provider:manage";
    /** 人工触发厂商主动拉取。 */
    public static final String ADMIN_HEALTH_PROVIDER_PULL = "admin:jk:health:provider:pull";
    /** 平台超管协助核查健康明细。 */
    public static final String ADMIN_HEALTH_EMERGENCY_VIEW = "admin:jk:health:emergency:view";
    /** 平台超管协助导出健康明细。 */
    public static final String ADMIN_HEALTH_EMERGENCY_EXPORT = "admin:jk:health:emergency:export";
    /** 库存批次查询。 */
    public static final String ADMIN_STOCK_BATCH_LIST = "admin:jk:stock:batch:list";
    /** 历史库存批次初始化。 */
    public static final String ADMIN_STOCK_BATCH_INIT = "admin:jk:stock:batch:init";
    /** 维护批次成本、生产日期和有效期，不允许修改数量。 */
    public static final String ADMIN_STOCK_BATCH_UPDATE = "admin:jk:stock:batch:update";
    /** 第六阶段业务概览报表。 */
    public static final String ADMIN_REPORT_OVERVIEW = "admin:jk:report:overview";
    /** 第六阶段风险事件列表。 */
    public static final String ADMIN_RISK_EVENT_LIST = "admin:jk:risk:event:list";
    /** 第六阶段风险事件处置。 */
    public static final String ADMIN_RISK_EVENT_HANDLE = "admin:jk:risk:event:handle";
    /** 运行指定日期经营汇总。 */
    public static final String ADMIN_REPORT_DAILY_RUN = "admin:jk:report:daily:run";
    public static final String ADMIN_REPORT_TREND = "admin:jk:report:trend";
    public static final String ADMIN_REPORT_REGION = "admin:jk:report:region";
    public static final String ADMIN_REPORT_TEAM = "admin:jk:report:team";
    public static final String ADMIN_REPORT_INVENTORY = "admin:jk:report:inventory";
    public static final String ADMIN_REPORT_INVENTORY_RECONCILE = "admin:jk:report:inventory:reconcile";
    public static final String ADMIN_REPORT_FINANCE = "admin:jk:report:finance";
    public static final String ADMIN_REPORT_HEALTH = "admin:jk:report:health";
    public static final String ADMIN_REPORT_EXPORT = "admin:jk:report:export";
    /** 风险规则配置。 */
    public static final String ADMIN_RISK_RULE_MANAGE = "admin:jk:risk:rule:manage";
    /** 人工执行风险扫描。 */
    public static final String ADMIN_RISK_RULE_RUN = "admin:jk:risk:rule:run";

    private JkPermissionCodes() {
    }
}
