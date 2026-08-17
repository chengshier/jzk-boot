package com.zbkj.common.response.jiuzhoukang;

import com.zbkj.common.model.jiuzhoukang.JkSinocareCallbackLog;
import org.junit.Assert;
import org.junit.Test;

public class JkSinocareCallbackLogResponseTest {
    @Test
    public void hidesEncryptedPayloadAndSignature() {
        JkSinocareCallbackLog source = new JkSinocareCallbackLog()
                .setId(1L).setEventType("1003").setUniqueId("user-id")
                .setPayloadCipher("ciphertext").setSignature("signature")
                .setProcessStatus("FAILED").setErrorMessage("failed");

        JkSinocareCallbackLogResponse response = JkSinocareCallbackLogResponse.from(source);

        Assert.assertEquals("1003", response.getEventType());
        Assert.assertEquals("FAILED", response.getProcessStatus());
        Assert.assertFalse(response.toString().contains("ciphertext"));
        Assert.assertFalse(response.toString().contains("signature"));
    }
}
