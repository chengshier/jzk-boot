package com.zbkj.service.service.impl.jiuzhoukang.storage;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkFileObject;
import com.zbkj.common.response.jiuzhoukang.JkFileObjectResponse;
import com.zbkj.service.dao.jiuzhoukang.JkFileObjectDao;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.service.service.jiuzhoukang.storage.JkFileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** 统一私有文件存储。配置缺失或写入失败时明确报错，不返回伪成功地址。 */
@Service
public class JkFileStorageServiceImpl implements JkFileStorageService {
    @Autowired private JkFileObjectDao fileDao;
    @Autowired private JkS3CompatibleClient s3Client;
    @Autowired private SystemConfigService systemConfigService;

    @Value("${jk.storage.enabled:false}") private boolean enabled;
    @Value("${jk.storage.provider:LOCAL_PRIVATE}") private String provider;
    @Value("${jk.storage.max-size:20971520}") private long maxSize;
    @Value("${jk.storage.local-root:/data/jzk-private-files}") private String localRoot;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkFileObjectResponse store(MultipartFile file, String businessType, Long businessId, Long ownerUserId, String accessLevel) {
        if (file == null || file.isEmpty()) throw new CrmebException("上传文件不能为空");
        byte[] bytes;
        try { bytes = file.getBytes(); }
        catch (Exception error) { throw new CrmebException("读取上传文件失败"); }
        return storeBytes(bytes, file.getOriginalFilename(), file.getContentType(), businessType, businessId, ownerUserId, accessLevel);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkFileObjectResponse storeBytes(byte[] bytes, String originalName, String contentType, String businessType,
                                           Long businessId, Long ownerUserId, String accessLevel) {
        if (!enabled) throw new CrmebException("统一文件存储尚未启用");
        if (bytes == null || bytes.length == 0) throw new CrmebException("文件内容不能为空");
        if (bytes.length > maxSize) throw new CrmebException("文件超过允许大小");
        String normalizedContentType = safeContentType(contentType);
        validateContentType(normalizedContentType);
        String extension = extension(originalName, normalizedContentType);
        String objectKey = new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/"
                + UUID.randomUUID().toString().replace("-", "") + extension;
        String normalizedProvider = provider == null ? "LOCAL_PRIVATE" : provider.trim().toUpperCase(Locale.ROOT);
        if ("MINIO".equals(normalizedProvider)) {
            MinioConfig minio = requireMinioConfig();
            s3Client.put(minio.endpoint, minio.bucket, objectKey, bytes, normalizedContentType,
                    minio.accessKey, minio.secretKey, minio.region);
        } else if ("LOCAL_PRIVATE".equals(normalizedProvider)) {
            writeLocal(objectKey, bytes);
        } else {
            throw new CrmebException("不支持的文件存储类型：" + normalizedProvider);
        }
        Date now = new Date();
        JkFileObject value = new JkFileObject().setFileNo("FO" + IdWorker.getIdStr())
                .setStorageProvider(normalizedProvider)
                .setBucketName("MINIO".equals(normalizedProvider) ? minioBucket() : null)
                .setObjectKey(objectKey).setOriginalName(safeName(originalName)).setContentType(normalizedContentType)
                .setFileSize((long) bytes.length).setFileHash(sha256(bytes)).setBusinessType(businessType)
                .setBusinessId(businessId).setOwnerUserId(ownerUserId)
                .setAccessLevel(isBlank(accessLevel) ? "PRIVATE" : accessLevel.trim().toUpperCase(Locale.ROOT))
                .setStatus("ACTIVE").setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
        fileDao.insert(value);
        return response(value);
    }

    @Override
    public byte[] read(Long fileId, Long viewerUserId, boolean admin) {
        JkFileObject file = require(fileId);
        if (!admin && !"PUBLIC_AUTHENTICATED".equals(file.getAccessLevel())
                && (file.getOwnerUserId() == null || !file.getOwnerUserId().equals(viewerUserId))) {
            throw new CrmebException("无权访问该文件");
        }
        if (file.getExpireTime() != null && !file.getExpireTime().after(new Date())) throw new CrmebException("文件访问已过期");
        if ("MINIO".equals(file.getStorageProvider())) {
            MinioConfig minio = requireMinioConfig();
            return s3Client.get(minio.endpoint, file.getBucketName(), file.getObjectKey(),
                    minio.accessKey, minio.secretKey, minio.region);
        }
        return readLocal(file.getObjectKey());
    }

    @Override
    public JkFileObject require(Long fileId) {
        JkFileObject file = fileDao.selectById(fileId);
        if (file == null || Boolean.TRUE.equals(file.getIsDeleted()) || !"ACTIVE".equals(file.getStatus())) {
            throw new CrmebException("文件不存在或已失效");
        }
        return file;
    }

    @Override
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        String normalized = provider == null ? "LOCAL_PRIVATE" : provider.trim().toUpperCase(Locale.ROOT);
        boolean minioConfigured = minioConfigured();
        result.put("enabled", enabled);
        result.put("provider", normalized);
        result.put("maxSize", maxSize);
        result.put("minioConfigured", minioConfigured);
        result.put("ready", enabled && ("LOCAL_PRIVATE".equals(normalized) || ("MINIO".equals(normalized) && minioConfigured)));
        return result;
    }

    private void writeLocal(String key, byte[] bytes) {
        try {
            File root = new File(localRoot).getCanonicalFile();
            File target = new File(root, key).getCanonicalFile();
            if (!target.getPath().startsWith(root.getPath() + File.separator)) throw new IllegalStateException("非法文件路径");
            File parent = target.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) throw new IllegalStateException("创建文件目录失败");
            FileOutputStream output = new FileOutputStream(target);
            output.write(bytes);
            output.flush();
            output.close();
        } catch (Exception error) {
            throw new CrmebException("写入私有文件存储失败：" + safeMessage(error));
        }
    }

    private byte[] readLocal(String key) {
        try {
            File root = new File(localRoot).getCanonicalFile();
            File target = new File(root, key).getCanonicalFile();
            if (!target.getPath().startsWith(root.getPath() + File.separator) || !target.isFile()) {
                throw new IllegalStateException("文件不存在");
            }
            FileInputStream input = new FileInputStream(target);
            byte[] bytes = new byte[(int) target.length()];
            int offset = 0;
            int read;
            while (offset < bytes.length && (read = input.read(bytes, offset, bytes.length - offset)) > 0) offset += read;
            input.close();
            if (offset != bytes.length) throw new IllegalStateException("文件读取不完整");
            return bytes;
        } catch (Exception error) {
            throw new CrmebException("读取私有文件失败：" + safeMessage(error));
        }
    }

    private MinioConfig requireMinioConfig() {
        String endpoint = minioValue(SysConfigConstants.CONFIG_MINIO_ENDPOINT);
        String bucket = minioBucket();
        String accessKey = minioValue(SysConfigConstants.CONFIG_MINIO_ACCESS_KEY);
        String secretKey = minioValue(SysConfigConstants.CONFIG_MINIO_SECRET_KEY);
        if (isBlank(endpoint) || isBlank(bucket) || isBlank(accessKey) || isBlank(secretKey)) {
            throw new CrmebException("MinIO 配置不完整");
        }
        return new MinioConfig(endpoint, bucket, accessKey, secretKey, minioRegion());
    }

    private boolean minioConfigured() {
        return !isBlank(minioValue(SysConfigConstants.CONFIG_MINIO_ENDPOINT)) && !isBlank(minioBucket())
                && !isBlank(minioValue(SysConfigConstants.CONFIG_MINIO_ACCESS_KEY))
                && !isBlank(minioValue(SysConfigConstants.CONFIG_MINIO_SECRET_KEY));
    }

    private String minioBucket() { return minioValue(SysConfigConstants.CONFIG_MINIO_BUCKET); }
    private String minioRegion() { String value = minioValue(SysConfigConstants.CONFIG_MINIO_REGION); return isBlank(value) ? "us-east-1" : value; }
    private String minioValue(String key) { return systemConfigService == null ? null : systemConfigService.getValueByKey(key); }

    private static class MinioConfig {
        private final String endpoint, bucket, accessKey, secretKey, region;
        private MinioConfig(String endpoint, String bucket, String accessKey, String secretKey, String region) {
            this.endpoint = endpoint; this.bucket = bucket; this.accessKey = accessKey; this.secretKey = secretKey; this.region = region;
        }
    }

    private JkFileObjectResponse response(JkFileObject value) {
        return new JkFileObjectResponse().setId(value.getId()).setFileNo(value.getFileNo())
                .setOriginalName(value.getOriginalName()).setContentType(value.getContentType())
                .setFileSize(value.getFileSize()).setBusinessType(value.getBusinessType())
                .setBusinessId(value.getBusinessId()).setAccessLevel(value.getAccessLevel()).setStatus(value.getStatus())
                .setCreateTime(value.getCreateTime()).setDownloadPath("/api/front/jk/file/" + value.getId() + "/download");
    }

    private void validateContentType(String value) {
        if (!(value.startsWith("image/") || "application/pdf".equals(value))) {
            throw new CrmebException("仅允许图片或 PDF 文件");
        }
    }

    private String safeContentType(String value) {
        return isBlank(value) ? "application/octet-stream" : value.toLowerCase(Locale.ROOT);
    }

    private String extension(String name, String contentType) {
        if (!isBlank(name) && name.lastIndexOf('.') >= 0) {
            String ext = name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (ext.matches("\\.(png|jpg|jpeg|webp|gif|pdf)")) return ext;
        }
        if ("application/pdf".equals(contentType)) return ".pdf";
        if (contentType.contains("png")) return ".png";
        if (contentType.contains("webp")) return ".webp";
        return ".jpg";
    }

    private String safeName(String value) {
        if (isBlank(value)) return "file";
        String name = new File(value).getName().replaceAll("[\\r\\n]", "");
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private String sha256(byte[] value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            return out.toString();
        } catch (Exception error) {
            return String.valueOf(new String(value, StandardCharsets.ISO_8859_1).hashCode());
        }
    }

    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
    private String safeMessage(Exception value) {
        String message = value.getMessage();
        return message == null ? value.getClass().getSimpleName() : message.replace('\r', ' ').replace('\n', ' ');
    }
}
