package com.zbkj.service.service.jiuzhoukang.support;

import com.zbkj.service.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
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
 * <p>新数据使用 AES-GCM 认证加密，密钥来自系统配置 {@code jk_sensitive_data_secret}。
 * 为兼容本分支早期测试数据，仍支持读取 v1 AES-CBC 密文，但不会继续生成 v1 数据。</p>
 */
@Service
public class JkSensitiveFieldCryptoSupport {
    public static final String CONFIG_KEY_SECRET = "jk_sensitive_data_secret";
    private static final String PREFIX_V1 = "v1:";
    private static final String PREFIX_V2 = "v2:";
    private static final int GCM_NONCE_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private SystemConfigService systemConfigService;

    public String encrypt(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) throw new IllegalArgumentException("敏感字段不能为空");
        try {
            byte[] nonce = new byte[GCM_NONCE_LENGTH];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(encrypted, 0, payload, nonce.length, encrypted.length);
            return PREFIX_V2 + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("敏感字段加密失败", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) throw new IllegalArgumentException("敏感字段密文格式非法");
        if (cipherText.startsWith(PREFIX_V2)) return decryptV2(cipherText.substring(PREFIX_V2.length()));
        if (cipherText.startsWith(PREFIX_V1)) return decryptV1(cipherText.substring(PREFIX_V1.length()));
        throw new IllegalArgumentException("敏感字段密文格式非法");
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

    private String decryptV2(String encoded) {
        try {
            byte[] payload = Base64.getDecoder().decode(encoded);
            if (payload.length <= GCM_NONCE_LENGTH + 16) throw new IllegalArgumentException("敏感字段密文格式非法");
            byte[] nonce = Arrays.copyOfRange(payload, 0, GCM_NONCE_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(payload, GCM_NONCE_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("敏感字段解密或完整性校验失败，请确认密钥配置未变更", e);
        }
    }

    /** 仅用于读取本分支早期 v1 测试密文。 */
    private String decryptV1(String encoded) {
        try {
            byte[] payload = Base64.getDecoder().decode(encoded);
            if (payload.length <= 16) throw new IllegalArgumentException("敏感字段密文格式非法");
            byte[] iv = Arrays.copyOfRange(payload, 0, 16);
            byte[] encrypted = Arrays.copyOfRange(payload, 16, payload.length);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new IvParameterSpec(iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("历史敏感字段解密失败，请确认密钥配置未变更", e);
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
