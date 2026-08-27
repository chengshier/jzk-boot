package com.zbkj.service.service.jiuzhoukang.promotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 九州康小程序码的轻量场景协议。
 * <p>这里刻意不把 scene 交给 CRMEB 的 spread 解析：入口码只授予入口权限，推广码只携带申请邀请人。</p>
 */
public final class JkPromotionSceneSupport {
    private static final Pattern PROMOTION = Pattern.compile("^bind:(\\d+)$");
    private static final String ENTRY_ACCESS = "entry:open";

    private JkPromotionSceneSupport() { }

    public static SceneEntry parse(String scene) {
        if (ENTRY_ACCESS.equals(scene)) return new SceneEntry(SceneType.ENTRY_ACCESS, null);
        Matcher matcher = PROMOTION.matcher(scene == null ? "" : scene);
        if (matcher.matches()) return new SceneEntry(SceneType.IDENTITY_PROMOTION, Long.valueOf(matcher.group(1)));
        throw new IllegalArgumentException("不是有效的九州康业务场景");
    }

    public enum SceneType { ENTRY_ACCESS, IDENTITY_PROMOTION }

    public static final class SceneEntry {
        private final SceneType type;
        private final Long inviterUserId;
        SceneEntry(SceneType type, Long inviterUserId) { this.type = type; this.inviterUserId = inviterUserId; }
        public SceneType getType() { return type; }
        public Long getInviterUserId() { return inviterUserId; }
    }
}
