package com.zbkj.service.service.jiuzhoukang.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 九州康展示字典的安全兜底。正式字典由 SQL 种子维护；未加载种子时仍保证接口不返回空文案。
 */
public final class JkDictLabelHelper {
    private static final Map<String, String> LABELS;
    private static volatile Resolver resolver;

    static {
        Map<String, String> labels = new HashMap<>();
        put(labels, "platform_order_status", "PENDING_PAYMENT", "待付款");
        put(labels, "platform_order_status", "CREATED", "待提交付款");
        put(labels, "platform_order_status", "PAYMENT_SUBMITTED", "已提交付款凭证");
        put(labels, "platform_order_status", "PAYMENT_REJECTED", "付款驳回");
        put(labels, "platform_order_status", "PAYMENT_APPROVED", "付款审核通过");
        put(labels, "platform_order_status", "WAIT_SHIP", "待发货");
        put(labels, "platform_order_status", "SHIPPED", "已发货");
        put(labels, "platform_order_status", "RECEIVE_EXCEPTION", "收货异常处理中");
        put(labels, "platform_order_status", "RECEIVED", "已收货");
        put(labels, "platform_order_status", "STOCK_IN", "已入库");
        put(labels, "platform_order_status", "CANCELLED", "已取消");
        put(labels, "platform_order_status", "CLOSED", "已关闭");
        put(labels, "stock_flow_type", "TEST_INIT", "测试初始化");
        put(labels, "stock_flow_type", "FREEZE", "冻结库存");
        put(labels, "stock_flow_type", "RELEASE", "释放冻结");
        put(labels, "stock_flow_type", "OUTBOUND", "出库");
        put(labels, "stock_flow_type", "INBOUND", "入库");
        put(labels, "stock_business_type", "PLATFORM_ORDER", "平台订货");
        put(labels, "stock_business_type", "STOCK_TRANSFER", "库存调拨");
        put(labels, "stock_business_type", "TEST_INIT", "测试初始化");
        put(labels, "stock_transfer_status", "SUBMITTED", "待区县代审核");
        put(labels, "stock_transfer_status", "AUDIT_APPROVED", "审核通过");
        put(labels, "stock_transfer_status", "AUDIT_REJECTED", "审核驳回");
        put(labels, "stock_transfer_status", "PAYMENT_SUBMITTED", "已提交付款凭证");
        put(labels, "stock_transfer_status", "PAYMENT_REJECTED", "付款驳回");
        put(labels, "stock_transfer_status", "PAYMENT_APPROVED", "付款确认通过");
        put(labels, "stock_transfer_status", "TRANSFERRED", "已拨货");
        put(labels, "stock_transfer_status", "RECEIVE_EXCEPTION", "收货异常处理中");
        put(labels, "stock_transfer_status", "STOCK_IN", "已入库");
        put(labels, "stock_transfer_status", "CANCELLED", "已取消");
        put(labels, "stock_transfer_status", "CLOSED", "已关闭");
        put(labels, "receive_status", "UNRECEIVED", "待收货");
        put(labels, "receive_status", "EXCEPTION", "收货异常处理中");
        put(labels, "receive_status", "STOCK_IN", "已入库");
        put(labels, "voucher_audit_status", "PENDING", "待审核");
        put(labels, "voucher_audit_status", "APPROVED", "审核通过");
        put(labels, "voucher_audit_status", "REJECTED", "审核驳回");
        put(labels, "payment_audit_status", "PENDING", "待审核");
        put(labels, "payment_audit_status", "APPROVED", "审核通过");
        put(labels, "payment_audit_status", "REJECTED", "审核驳回");
        put(labels, "audit_status", "NONE", "未发起");
        put(labels, "audit_status", "PENDING", "待审核");
        put(labels, "audit_status", "APPROVED", "审核通过");
        put(labels, "audit_status", "EFFECTIVE", "已生效");
        put(labels, "audit_status", "FROZEN", "已冻结");
        put(labels, "audit_status", "REJECTED", "已驳回");
        put(labels, "audit_status", "CANCELLED", "已取消");
        put(labels, "pay_status", "UNPAID", "未付款");
        put(labels, "pay_status", "PAYMENT_SUBMITTED", "待确认付款");
        put(labels, "pay_status", "APPROVED", "付款已确认");
        put(labels, "pay_status", "REJECTED", "付款已驳回");
        put(labels, "stock_source", "RETAIL_STOCK", "零售库存");
        put(labels, "stock_source", "PLATFORM_ORDERABLE", "平台可订货库存");
        put(labels, "stock_source", "COUNTY_AGENT_ALLOCATABLE", "所属区县代可调拨库存");
        put(labels, "stock_source", "OWN_STOCK", "我的库存");
        put(labels, "withdraw_status", "SUBMITTED", "已提交");
        put(labels, "withdraw_status", "AUDITING", "审核中");
        put(labels, "withdraw_status", "APPROVED", "审核通过");
        put(labels, "withdraw_status", "REJECTED", "已驳回");
        put(labels, "withdraw_status", "PAID", "已打款");
        put(labels, "withdraw_status", "CANCELLED", "已取消");
        put(labels, "commission_status", "CREATED", "已创建");
        put(labels, "commission_status", "PENDING_SETTLE", "待结算");
        put(labels, "commission_status", "FROZEN", "已冻结");
        put(labels, "commission_status", "SETTLED", "已结算");
        put(labels, "commission_status", "AVAILABLE", "可提现");
        put(labels, "commission_status", "WITHDRAWING", "提现中");
        put(labels, "commission_status", "WITHDRAWN", "已提现");
        put(labels, "commission_status", "REVERSED", "已冲正");
        put(labels, "commission_status", "CANCELLED", "已取消");
        put(labels, "commission_reverse_type", "REFUND", "退款冲正");
        put(labels, "commission_reverse_type", "RETURN", "退货冲正");
        put(labels, "commission_reverse_type", "MANUAL_ADJUST", "人工调整");
        put(labels, "commission_reverse_type", "IDENTITY_FROZEN", "身份冻结");
        put(labels, "commission_reverse_type", "ORDER_CANCEL", "订单取消");
        put(labels, "commission_reverse_type", "TRANSFER_RETURN", "调拨退回");
        put(labels, "commission_reverse_status", "PENDING", "待处理");
        put(labels, "commission_reverse_status", "APPROVED", "已通过");
        put(labels, "commission_reverse_status", "REJECTED", "已驳回");
        put(labels, "commission_reverse_status", "COMPLETED", "已完成");
        put(labels, "commission_settle_status", "PENDING", "待执行");
        put(labels, "commission_settle_status", "RUNNING", "执行中");
        put(labels, "commission_settle_status", "SUCCESS", "执行成功");
        put(labels, "commission_settle_status", "FAILED", "执行失败");
        put(labels, "commission_settle_status", "PARTIAL_SUCCESS", "部分成功");
        put(labels, "fund_flow_type", "SETTLE_IN", "结算入账");
        put(labels, "fund_flow_type", "WITHDRAW_FREEZE", "提现冻结");
        put(labels, "fund_flow_type", "WITHDRAW_RELEASE", "提现释放");
        put(labels, "fund_flow_type", "WITHDRAW_PAID", "提现打款");
        put(labels, "fund_flow_type", "REVERSE_DEDUCT", "冲正扣减");
        put(labels, "fund_flow_type", "MANUAL_ADJUST", "人工调整");
        put(labels, "audit_action", "PASS", "通过");
        put(labels, "audit_action", "REJECT", "驳回");
        put(labels, "audit_action", "FREEZE", "冻结");
        put(labels, "audit_action", "UNFREEZE", "解冻");
        put(labels, "audit_action", "CANCEL", "取消");
        put(labels, "audit_action", "CLOSE", "关闭");
        put(labels, "audit_action", "CONFIRM", "确认");
        put(labels, "audit_action", "PAY", "付款确认");
        put(labels, "audit_action", "SHIP", "发货");
        put(labels, "audit_action", "RECEIVE", "收货");
        put(labels, "audit_action", "SUBMIT", "提交");
        put(labels, "audit_action", "SHIP_RETURN", "寄回退货");
        put(labels, "audit_action", "RECEIVE_RETURN", "确认退货收货");
        put(labels, "audit_action", "CONFIRM_REFUND", "确认退款");
        put(labels, "relation_change_status", "PENDING", "待审核");
        put(labels, "relation_change_status", "APPROVED", "已通过");
        put(labels, "relation_change_status", "REJECTED", "已驳回");
        put(labels, "relation_change_status", "CANCELLED", "已取消");
        put(labels, "business_event_status", "PENDING", "待处理");
        put(labels, "business_event_status", "PROCESSING", "处理中");
        put(labels, "business_event_status", "FAILED", "处理失败");
        put(labels, "business_event_status", "SUCCESS", "处理成功");
        put(labels, "business_event_status", "DEAD", "已终止");
        put(labels, "account_reconcile_status", "BALANCED", "账目平衡");
        put(labels, "account_reconcile_status", "DIFFERENCE", "存在差异");
        put(labels, "stock_transfer_return_status", "SUBMITTED", "待区县代审核");
        put(labels, "stock_transfer_return_status", "AUDIT_APPROVED", "审核通过待寄回");
        put(labels, "stock_transfer_return_status", "AUDIT_REJECTED", "审核驳回");
        put(labels, "stock_transfer_return_status", "RETURN_SHIPPED", "已寄回待收货");
        put(labels, "stock_transfer_return_status", "REFUND_PENDING", "已收货待退款");
        put(labels, "stock_transfer_return_status", "COMPLETED", "退回完成");
        put(labels, "stock_transfer_return_status", "CANCELLED", "已取消");
        put(labels, "stock_transfer_return_status", "CLOSED", "已关闭");
        put(labels, "refund_status", "UNREFUNDED", "未退款");
        put(labels, "refund_status", "REFUND_PENDING", "待退款");
        put(labels, "refund_status", "REFUNDED", "已退款");
        put(labels, "stock_business_type", "STOCK_TRANSFER_RETURN", "调拨退回");
        LABELS = Collections.unmodifiableMap(labels);
    }

    private JkDictLabelHelper() { }

    public static String label(String dictType, String code) {
        if (code == null || code.trim().isEmpty()) return "--";
        Resolver current = resolver;
        if (current != null) {
            try {
                String dynamic = current.label(dictType, code);
                if (dynamic != null && !dynamic.trim().isEmpty()) return dynamic;
            } catch (RuntimeException ignored) {
                // 数据库字典不可用时继续使用静态安全兜底，不影响核心业务响应。
            }
        }
        String label = LABELS.get(dictType + ':' + code);
        return label == null ? code : label;
    }

    public static void installResolver(Resolver value) { resolver = value; }

    public interface Resolver { String label(String dictType, String code); }

    private static void put(Map<String, String> labels, String dictType, String code, String label) {
        labels.put(dictType + ':' + code, label);
    }
}
