package com.zbkj.common.constants.jiuzhoukang;

/**
 * 九州康业务权限点常量。
 * 仅定义前四阶段仍在治理范围内的业务权限，不进入第五阶段健康数据或 AI 分析能力。
 */
public final class JkBizPermissionCodes {

    /** 查看商品交易视图。 */
    public static final String PRODUCT_TRADE_VIEW = "product.trade.view";
    /** 配置价格规则。 */
    public static final String PRICE_RULE_CONFIG = "price.rule.config";
    /** 查看本人库存。 */
    public static final String STOCK_VIEW_SELF = "stock.view.self";
    /** 查看区域库存。 */
    public static final String STOCK_VIEW_REGION = "stock.view.region";
    /** 平台后台查看全部库存账户、明细和流水。 */
    public static final String STOCK_VIEW_ALL = "stock.view.all";
    /** 发起调拨申请。 */
    public static final String STOCK_APPLY = "stock.apply";
    /** 区县代向平台订货。 */
    public static final String STOCK_PLATFORM_ORDER = "stock.platform.order";
    /** 平台后台审核、发货和关闭区县代订货。 */
    public static final String STOCK_PLATFORM_AUDIT = "stock.platform.audit";
    /** 区县代审核调拨申请。 */
    public static final String STOCK_TRANSFER_AUDIT = "stock.transfer.audit";
    /** 区县代确认拨货或下级确认处理调拨。 */
    public static final String STOCK_TRANSFER_CONFIRM = "stock.transfer.confirm";
    /** 查看库存流水。 */
    public static final String STOCK_FLOW_VIEW = "stock.flow.view";
    /** 提交线下付款凭证。 */
    public static final String PAYMENT_OFFLINE_SUBMIT = "payment.offline.submit";
    /** 审核线下付款凭证。 */
    public static final String PAYMENT_OFFLINE_AUDIT = "payment.offline.audit";
    /** 提交身份申请。 */
    public static final String IDENTITY_APPLY_SUBMIT = "identity.apply.submit";
    /** 审核身份申请。 */
    public static final String IDENTITY_APPLY_AUDIT = "identity.apply.audit";
    /** 冻结业务身份。 */
    public static final String IDENTITY_FREEZE = "identity.freeze";
    /** 解冻业务身份。 */
    public static final String IDENTITY_UNFREEZE = "identity.unfreeze";
    /** 取消业务身份。 */
    public static final String IDENTITY_CANCEL = "identity.cancel";
    /** 查看审核日志。 */
    public static final String AUDIT_LOG_VIEW = "audit.log.view";
    /** 管理分佣规则。 */
    public static final String COMMISSION_RULE_MANAGE = "commission.rule.manage";
    /** 查看本人收益中心。 */
    public static final String COMMISSION_VIEW_SELF = "commission.view.self";
    /** 查看佣金账户。 */
    public static final String COMMISSION_ACCOUNT_VIEW = "commission.account.view";
    /** 查看佣金明细。 */
    public static final String COMMISSION_RECORD_VIEW = "commission.record.view";
    /** 管理佣金冻结。 */
    public static final String COMMISSION_FREEZE_MANAGE = "commission.freeze.manage";
    /** 查看佣金冲正记录。 */
    public static final String COMMISSION_REVERSE_VIEW = "commission.reverse.view";
    /** 管理佣金冲正。 */
    public static final String COMMISSION_REVERSE_MANAGE = "commission.reverse.manage";
    /** 查看结算任务。 */
    public static final String COMMISSION_SETTLE_VIEW = "commission.settle.view";
    /** 管理佣金结算。 */
    public static final String COMMISSION_SETTLE_MANAGE = "commission.settle.manage";
    /** 查看资金账户。 */
    public static final String FUND_ACCOUNT_VIEW = "fund.account.view";
    /** 查看资金流水。 */
    public static final String FUND_FLOW_VIEW = "fund.flow.view";
    /** 提现申请。 */
    public static final String WITHDRAW_APPLY = "withdraw.apply";
    /** 查看本人提现。 */
    public static final String WITHDRAW_VIEW_SELF = "withdraw.view.self";
    /** 审核提现。 */
    public static final String WITHDRAW_AUDIT = "withdraw.audit";
    /** 确认线下打款。 */
    public static final String WITHDRAW_CONFIRM_PAID = "withdraw.confirm.paid";

    /** 管理区域主数据。 */
    public static final String REGION_MANAGE = "region.manage";
    /** 管理区域代理配置。 */
    public static final String REGION_AGENT_MANAGE = "region.agent.manage";
    /** 管理上下级关系。 */
    public static final String AGENT_RELATION_MANAGE = "agent.relation.manage";
    /** 管理后台业务用户映射。 */
    public static final String ADMIN_MAPPING_MANAGE = "admin.mapping.manage";
    /** 管理九州康动态字典。 */
    public static final String DICT_MANAGE = "dict.manage";

    /** 申请取消本人订货或调拨单。 */
    public static final String TRADE_CANCEL_SELF = "trade.cancel.self";
    /** 申请库存调拨退回。 */
    public static final String STOCK_TRANSFER_RETURN_APPLY = "stock.transfer.return.apply";
    /** 审核和处理库存调拨退回。 */
    public static final String STOCK_TRANSFER_RETURN_AUDIT = "stock.transfer.return.audit";
    /** 查看和处理业务事件补偿。 */
    public static final String BUSINESS_EVENT_MANAGE = "business.event.manage";
    /** 执行佣金自动结算。 */
    public static final String COMMISSION_AUTO_SETTLE = "commission.auto.settle";
    /** 查看和执行账户对账。 */
    public static final String ACCOUNT_RECONCILE_MANAGE = "account.reconcile.manage";
    /** 后台九州康管理选择器：用户、管理员、区域。 */
    public static final String MANAGEMENT_OPTION_VIEW = "management.option.view";
    /** 查看团队与推广二维码。 */
    public static final String TEAM_VIEW = "team.view.direct";
    /** 提交上下级换绑申请。 */
    public static final String AGENT_RELATION_CHANGE_APPLY = "agent.relation.change.apply";
    /** 审核上下级换绑申请。 */
    public static final String AGENT_RELATION_CHANGE_AUDIT = "agent.relation.change.audit";


    /** 查看本人健康数据。 */
    public static final String HEALTH_DATA_VIEW_SELF = "health.data.view.self";
    /** 查看用户授权的健康数据。 */
    public static final String HEALTH_DATA_VIEW_AUTHORIZED = "health.data.view.authorized";
    /** 导出本人健康数据；导出动作仍必须写访问日志。 */
    public static final String HEALTH_DATA_EXPORT_SELF = "health.data.export.self";
    /** 导出授权用户健康数据；还必须检查授权记录 allowExport。 */
    public static final String HEALTH_DATA_EXPORT_AUTHORIZED = "health.data.export.authorized";
    /** 绑定或解绑本人健康设备。 */
    public static final String HEALTH_DEVICE_BIND = "health.device.bind";
    /** 管理本人健康数据授权。 */
    public static final String HEALTH_AUTH_MANAGE = "health.auth.manage";
    /** 管理健康设备、预警规则和访问日志。 */
    public static final String HEALTH_ADMIN_MANAGE = "health.admin.manage";
    /** 查看第五阶段健康预警。 */
    public static final String HEALTH_ALERT_VIEW = "health.alert.view";
    /** 管理健康厂商接入配置和拉取任务。 */
    public static final String HEALTH_PROVIDER_MANAGE = "health.provider.manage";
    /** 平台超管在填写原因并强审计后协助查看健康明细。 */
    public static final String HEALTH_DATA_EMERGENCY_VIEW = "health.data.emergency.view";
    /** 查看库存批次和库龄。 */
    public static final String STOCK_BATCH_VIEW = "stock.batch.view";
    /** 初始化或维护库存批次。 */
    public static final String STOCK_BATCH_MANAGE = "stock.batch.manage";
    /** 查看第六阶段业务报表。 */
    public static final String REPORT_VIEW = "report.view";
    /** 管理第六阶段风险事件。 */
    public static final String RISK_MANAGE = "risk.manage";

    private JkBizPermissionCodes() {
    }
}
