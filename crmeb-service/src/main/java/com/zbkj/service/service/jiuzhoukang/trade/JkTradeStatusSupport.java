package com.zbkj.service.service.jiuzhoukang.trade;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class JkTradeStatusSupport {

    private static final Set<String> PLATFORM_ORDER_RELEASE_STATES = new HashSet<>(Arrays.asList(
            "PAYMENT_APPROVED"
    ));
    private static final Set<String> TRANSFER_RELEASE_STATES = new HashSet<>(Arrays.asList(
            "AUDIT_APPROVED",
            "PAYMENT_SUBMITTED",
            "PAYMENT_APPROVED"
    ));

    private JkTradeStatusSupport() {
    }

    public static boolean platformOrderRequiresFrozenRelease(String status) {
        return PLATFORM_ORDER_RELEASE_STATES.contains(status);
    }

    public static boolean transferRequiresFrozenRelease(String status) {
        return TRANSFER_RELEASE_STATES.contains(status);
    }

    public static String inventoryRejectReason(String remark) {
        if (StrUtil.isBlank(remark)) {
            return "库存不足";
        }
        return "库存不足；" + remark.trim();
    }
}
