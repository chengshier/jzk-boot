package com.zbkj.service.service.jiuzhoukang.commission;

public final class WithdrawStateSupport {
    private WithdrawStateSupport() { }

    public static boolean canTransit(String from, String to) {
        if ("SUBMITTED".equals(from)) return "AUDITING".equals(to) || "CANCELLED".equals(to);
        if ("AUDITING".equals(from)) return "APPROVED".equals(to) || "REJECTED".equals(to);
        return "APPROVED".equals(from) && "PAID".equals(to);
    }
}
