package com.zbkj.admin.controller;

import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.impl.jiuzhoukang.storage.JkS3CompatibleClient;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

public class SystemConfigControllerMinioTest {

    @Test(expected = IllegalArgumentException.class)
    public void minioConnectionTestRejectsLoopbackEndpointBeforeWriting() {
        SystemConfigController controller = new SystemConfigController();
        Map<String, String> request = new HashMap<String, String>();
        request.put("minioEndpoint", "http://127.0.0.1:9000");
        request.put("minioBucket", "uploads");
        request.put("minioAccessKey", "access");
        request.put("minioSecretKey", "secret");
        ReflectionTestUtils.setField(controller, "minioClient", new JkS3CompatibleClient() {
            @Override
            public void testWriteDelete(String endpoint, String bucket, String accessKey, String secretKey, String region) {
                Assert.fail("MinIO write must not run for a loopback endpoint");
            }
        });

        controller.testMinio(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void minioConnectionTestRequiresAllCredentials() {
        new SystemConfigController().testMinio(new HashMap<String, String>());
    }

    @Test
    public void minioConnectionTestDoesNotEchoSecret() {
        SystemConfigController controller = new SystemConfigController();
        Map<String, String> request = new HashMap<String, String>();
        request.put("minioEndpoint", "http://8.8.8.8");
        request.put("minioBucket", "uploads");
        request.put("minioAccessKey", "access");
        request.put("minioSecretKey", "top-secret");
        ReflectionTestUtils.setField(controller, "minioClient", new JkS3CompatibleClient() {
            @Override
            public void testWriteDelete(String endpoint, String bucket, String accessKey, String secretKey, String region) {
                Assert.assertEquals("http://8.8.8.8", endpoint);
                Assert.assertEquals("top-secret", secretKey);
            }
        });

        CommonResult<?> result = controller.testMinio(request);

        Assert.assertNotNull(result.getData());
        Assert.assertFalse(String.valueOf(result.getData()).contains("top-secret"));
    }
}
