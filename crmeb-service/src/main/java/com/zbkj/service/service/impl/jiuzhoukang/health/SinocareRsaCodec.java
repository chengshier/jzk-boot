package com.zbkj.service.service.impl.jiuzhoukang.health;

import com.zbkj.common.exception.CrmebException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** 三诺文档约定：SHA256withRSA 验签，然后用三诺提供的 1024 位公钥分段解密 ciphertext。 */
@Component
public class SinocareRsaCodec {
    @Value("${jk.health.sinocare.public-key:}") private String publicKeyText;
    public String verifyAndDecrypt(String ciphertext, String signature) {
        if (publicKeyText == null || publicKeyText.trim().isEmpty()) throw new CrmebException("三诺公钥未配置");
        try {
            PublicKey key = key(publicKeyText);
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key); verifier.update(ciphertext.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getDecoder().decode(signature))) throw new CrmebException("三诺回调验签失败");
            byte[] source = Base64.getDecoder().decode(ciphertext);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"); cipher.init(Cipher.DECRYPT_MODE, key);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int offset=0; offset<source.length; offset+=128) out.write(cipher.doFinal(source, offset, Math.min(128, source.length-offset)));
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (CrmebException e) { throw e; }
        catch (Exception e) { throw new CrmebException("三诺回调解密失败"); }
    }
    private PublicKey key(String value) throws Exception {
        String normalized=value.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(normalized)));
    }
}
