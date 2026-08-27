package com.zbkj.service.service.impl;

import com.zbkj.common.config.CrmebConfig;
import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.system.SystemAttachment;
import com.zbkj.common.vo.FileResultVo;
import com.zbkj.service.service.SystemAttachmentService;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.service.service.impl.jiuzhoukang.storage.JkS3CompatibleClient;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public class UploadServiceImplMinioTest {

    @Test
    public void minioUploadPersistsTypeSixAndHonorsLocalRetention() throws Exception {
        Path root = Files.createTempDirectory("upload-minio-test");
        try {
            AtomicReference<SystemAttachment> saved = new AtomicReference<SystemAttachment>();
            UploadServiceImpl service = uploadService(root, "1", saved, false);

            FileResultVo result = service.imageUpload(image(), "product", 1);

            Assert.assertEquals(6, saved.get().getImageType().intValue());
            Assert.assertEquals(result.getUrl(), saved.get().getSattDir());
            Assert.assertTrue(new File(root.toFile(), result.getUrl().replace("assets/", "")).isFile());
        } finally {
            delete(root.toFile());
        }
    }

    @Test
    public void minioUploadRemovesLocalFileWhenConfiguredNotToSave() throws Exception {
        Path root = Files.createTempDirectory("upload-minio-test");
        try {
            AtomicReference<SystemAttachment> saved = new AtomicReference<SystemAttachment>();
            UploadServiceImpl service = uploadService(root, "0", saved, false);

            FileResultVo result = service.imageUpload(image(), "product", 1);

            Assert.assertEquals(6, saved.get().getImageType().intValue());
            Assert.assertFalse(new File(root.toFile(), result.getUrl().replace("assets/", "")).exists());
        } finally {
            delete(root.toFile());
        }
    }

    @Test
    public void minioUploadFailureDoesNotPersistAttachment() throws Exception {
        Path root = Files.createTempDirectory("upload-minio-test");
        try {
            AtomicReference<SystemAttachment> saved = new AtomicReference<SystemAttachment>();
            UploadServiceImpl service = uploadService(root, "0", saved, true);

            try {
                service.imageUpload(image(), "product", 1);
                Assert.fail("expected MinIO upload failure");
            } catch (CrmebException expected) {
                Assert.assertTrue(expected.getMessage().contains("MinIO 上传失败"));
            }
            Assert.assertNull(saved.get());
        } finally {
            delete(root.toFile());
        }
    }

    private UploadServiceImpl uploadService(Path root, String fileIsSave, AtomicReference<SystemAttachment> saved, boolean failUpload) {
        UploadServiceImpl service = new UploadServiceImpl();
        CrmebConfig config = new CrmebConfig();
        config.setImagePath(root.toFile().getAbsolutePath() + File.separator);
        ReflectionTestUtils.setField(service, "crmebConfig", config);
        ReflectionTestUtils.setField(service, "systemConfigService", configService(fileIsSave));
        ReflectionTestUtils.setField(service, "systemAttachmentService", attachmentService(saved));
        ReflectionTestUtils.setField(service, "minioClient", new JkS3CompatibleClient() {
            @Override
            public void put(String endpoint, String bucket, String objectKey, byte[] bytes, String contentType,
                            String accessKey, String secretKey, String region) {
                if (failUpload) throw new IllegalStateException("unavailable");
            }
        });
        return service;
    }

    private SystemConfigService configService(String fileIsSave) {
        return (SystemConfigService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{SystemConfigService.class},
                (proxy, method, args) -> {
                    if (!"getValueByKey".equals(method.getName()) && !"getValueByKeyException".equals(method.getName())) return null;
                    String key = (String) args[0];
                    if (SysConfigConstants.CONFIG_UPLOAD_TYPE.equals(key)) return "6";
                    if (SysConfigConstants.CONFIG_FILE_IS_SAVE.equals(key)) return fileIsSave;
                    if (SysConfigConstants.CONFIG_MINIO_UPLOAD_URL.equals(key)) return "https://cdn.example.com";
                    if (SysConfigConstants.CONFIG_MINIO_PREFIX.equals(key)) return "assets";
                    if (SysConfigConstants.CONFIG_MINIO_REGION.equals(key)) return "us-east-1";
                    if (SysConfigConstants.UPLOAD_IMAGE_EXT_STR_CONFIG_KEY.equals(key)) return "jpg";
                    if (SysConfigConstants.UPLOAD_IMAGE_MAX_SIZE_CONFIG_KEY.equals(key)) return "10";
                    return "configured";
                });
    }

    private SystemAttachmentService attachmentService(AtomicReference<SystemAttachment> saved) {
        return (SystemAttachmentService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{SystemAttachmentService.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        saved.set((SystemAttachment) args[0]);
                        return true;
                    }
                    return null;
                });
    }

    private MockMultipartFile image() { return new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3}); }

    private void delete(File value) {
        if (value == null || !value.exists()) return;
        File[] children = value.listFiles();
        if (children != null) for (File child : children) delete(child);
        value.delete();
    }
}
