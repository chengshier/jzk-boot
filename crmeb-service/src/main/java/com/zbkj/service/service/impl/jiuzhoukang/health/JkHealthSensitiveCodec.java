package com.zbkj.service.service.impl.jiuzhoukang.health;

import com.zbkj.common.exception.CrmebException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 健康敏感字段编解码器。
 * <p>配置 {@code jk.health.data-encryption-key} 后使用 AES/GCM；未配置时以 PLAIN 前缀兼容开发环境。</p>
 * <p>生产环境必须配置独立密钥，并通过密钥管理系统注入，禁止把真实密钥提交到仓库。</p>
 */
@Component
public class JkHealthSensitiveCodec {
    private static final Logger LOGGER = LoggerFactory.getLogger(JkHealthSensitiveCodec.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile boolean warned;

    @Value("${jk.health.data-encryption-key:}")
    private String configuredKey;

    @Value("${jk.health.allow-plaintext:false}")
    private boolean allowPlaintext;

    public String encode(String plain) {
        if (plain == null || plain.isEmpty()) return plain;
        if (configuredKey == null || configuredKey.trim().isEmpty()) {
            if (!allowPlaintext) {
                throw new CrmebException("健康数据加密密钥未配置，已拒绝保存敏感数据");
            }
            warnPlainMode();
            return "PLAIN:" + plain;
        }
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] packed = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, packed, 0, iv.length);
            System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length);
            return "ENC:" + Base64.getEncoder().encodeToString(packed);
        } catch (Exception e) {
            throw new CrmebException("健康敏感数据加密失败");
        }
    }

    public String decode(String value) {
        if (value == null || value.isEmpty()) return value;
        if (value.startsWith("PLAIN:")) return value.substring(6);
        if (!value.startsWith("ENC:")) return value;
        if (configuredKey == null || configuredKey.trim().isEmpty()) {
            throw new CrmebException("健康数据已加密，但当前环境未配置解密密钥");
        }
        try {
            byte[] packed = Base64.getDecoder().decode(value.substring(4));
            byte[] iv = Arrays.copyOfRange(packed, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(packed, 12, packed.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CrmebException("健康敏感数据解密失败");
        }
    }

    private SecretKeySpec key() throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(configuredKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(hash, "AES");
    }

    private void warnPlainMode() {
        if (!warned) {
            synchronized (JkHealthSensitiveCodec.class) {
                if (!warned) {
                    LOGGER.warn("jk.health.data-encryption-key 未配置，健康扩展字段暂以开发模式明文存储；生产环境禁止使用该模式");
                    warned = true;
                }
            }
        }
    }
}
