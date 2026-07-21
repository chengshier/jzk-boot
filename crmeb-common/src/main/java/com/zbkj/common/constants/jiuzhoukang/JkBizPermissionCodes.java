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
    /** 发起调拨申请。 */
    public static final String STOCK_APPLY = "stock.apply";
    /** 区县代向平台订货。 */
    public static final String STOCK_PLATFORM_ORDER = "stock.platform.order";
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

    private JkBizPermissionCodes() {
    }
}
