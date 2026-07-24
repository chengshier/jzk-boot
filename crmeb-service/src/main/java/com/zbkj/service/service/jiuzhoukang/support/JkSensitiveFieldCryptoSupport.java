package com.zbkj.service.service.jiuzhoukang.support;

import com.zbkj.service.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 九州康敏感字段加解密支持。
 *
 * <p>密钥来自系统配置 {@code jk_sensitive_data_secret}，生产环境必须单独配置且不得写入源码。</p>
 */
@Service
public class JkSensitiveFieldCryptoSupport {
    public static final String CONFIG_KEY_SECRET = "jk_sensitive_data_secret";
    private static final String PREFIX = "v1:";

    @Autowired
    private SystemConfigService systemConfigService;

    public String encrypt(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            throw new IllegalArgumentException("敏感字段不能为空");
        }
        try {
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("敏感字段加密失败", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || !cipherText.startsWith(PREFIX)) {
            throw new IllegalArgumentException("敏感字段密文格式非法");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText.substring(PREFIX.length()));
            if (payload.length <= 16) throw new IllegalArgumentException("敏感字段密文格式非法");
            byte[] iv = Arrays.copyOfRange(payload, 0, 16);
            byte[] encrypted = Arrays.copyOfRange(payload, 16, payload.length);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new IvParameterSpec(iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("敏感字段解密失败，请确认密钥配置未变更", e);
        }
    }

    public String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte item : digest) builder.append(String.format("%02x", item & 0xff));
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("敏感字段摘要生成失败", e);
        }
    }

    private SecretKeySpec key() throws Exception {
        String secret = systemConfigService.getValueByKey(CONFIG_KEY_SECRET);
        if (secret == null || secret.trim().length() < 16) {
            throw new IllegalStateException("请先配置九州康敏感数据密钥：" + CONFIG_KEY_SECRET + "，长度至少16位");
        }
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.trim().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(digest, 16), "AES");
    }
}
