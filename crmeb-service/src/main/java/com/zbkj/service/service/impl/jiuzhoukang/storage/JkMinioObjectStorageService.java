package com.zbkj.service.service.impl.jiuzhoukang.storage;

import com.zbkj.common.exception.CrmebException;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

/**
 * 九州康对象存储。默认关闭，未配置时明确失败，不回退到数据库或临时本地目录。
 */
@Service
public class JkMinioObjectStorageService {
    @Value("${jk.storage.minio.enabled:false}") private boolean enabled;
    @Value("${jk.storage.minio.endpoint:}") private String endpoint;
    @Value("${jk.storage.minio.access-key:}") private String accessKey;
    @Value("${jk.storage.minio.secret-key:}") private String secretKey;
    @Value("${jk.storage.minio.bucket:jzk-business}") private String bucket;

    public boolean isEnabled() { return enabled; }

    public String put(String objectKey, byte[] bytes, String contentType) {
        requireEnabled();
        if (bytes == null) throw new CrmebException("上传内容不能为空");
        String key = normalize(objectKey);
        try {
            MinioClient client = client();
            ensureBucket(client);
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType).build());
            return key;
        } catch (Exception e) {
            throw new CrmebException("MinIO 上传失败：" + safe(e.getMessage()));
        }
    }

    public String presignedDownloadUrl(String objectKey, int minutes) {
        requireEnabled();
        try {
            return client().getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.GET)
                    .bucket(bucket).object(normalize(objectKey)).expiry(Math.max(1, Math.min(minutes, 1440)), TimeUnit.MINUTES).build());
        } catch (Exception e) {
            throw new CrmebException("生成 MinIO 下载地址失败：" + safe(e.getMessage()));
        }
    }

    public void delete(String objectKey) {
        requireEnabled();
        try {
            client().removeObject(RemoveObjectArgs.builder().bucket(bucket).object(normalize(objectKey)).build());
        } catch (Exception e) {
            throw new CrmebException("删除 MinIO 文件失败：" + safe(e.getMessage()));
        }
    }

    private MinioClient client() {
        if (blank(endpoint) || blank(accessKey) || blank(secretKey) || blank(bucket)) {
            throw new CrmebException("MinIO 参数未完整配置");
        }
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }

    private void ensureBucket(MinioClient client) throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private void requireEnabled() {
        if (!enabled) throw new CrmebException("MinIO 对象存储尚未启用");
    }
    private String normalize(String key) {
        if (blank(key)) throw new CrmebException("对象键不能为空");
        String value = key.replace('\\', '/');
        while (value.startsWith("/")) value = value.substring(1);
        if (value.contains("../")) throw new CrmebException("对象键非法");
        return value;
    }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private String safe(String value) { return value == null ? "未知错误" : value.replace('\n', ' ').replace('\r', ' '); }
}
