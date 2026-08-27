package com.zbkj.service.service.impl.jiuzhoukang.health;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 仅供开发/测试环境手工生成健康数据加密密钥。
 * 在 IDE 中直接运行 main 方法，复制控制台输出到部署环境变量即可。
 */
public final class JkHealthEncryptionKeyGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private JkHealthEncryptionKeyGenerator() {
    }

    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static void main(String[] args) {
        System.out.println("JK_HEALTH_DATA_ENCRYPTION_KEY=" + generate());
    }
}
