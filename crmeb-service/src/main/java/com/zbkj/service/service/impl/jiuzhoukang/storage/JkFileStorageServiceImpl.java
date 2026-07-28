package com.zbkj.service.service.impl.jiuzhoukang.storage;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkFileObject;
import com.zbkj.common.response.jiuzhoukang.JkFileObjectResponse;
import com.zbkj.service.dao.jiuzhoukang.JkFileObjectDao;
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

@Service
public class JkFileStorageServiceImpl implements JkFileStorageService {
    @Autowired private JkFileObjectDao fileDao;
    @Autowired private JkS3CompatibleClient s3Client;

    @Value("${jk.storage.enabled:false}") private boolean enabled;
    @Value("${jk.storage.provider:LOCAL_PRIVATE}") private String provider;
    @Value("${jk.storage.max-size:20971520}") private long maxSize;
    @Value("${jk.storage.local-root:/data/jzk-private-files}") private String localRoot;
    @Value("${jk.storage.minio.endpoint:}") private String minioEndpoint;
    @Value("${jk.storage.minio.bucket:}") private String minioBucket;
    @Value("${jk.storage.minio.access-key:}") private String minioAccessKey;
    @Value("${jk.storage.minio.secret-key:}") private String minioSecretKey;
    @Value("${jk.storage.minio.region:us-east-1}") private String minioRegion;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkFileObjectResponse store(MultipartFile file, String businessType, Long businessId, Long ownerUserId, String accessLevel) {
        if (!enabled) throw new CrmebException("统一文件存储尚未启用");
        if (file == null || file.isEmpty()) throw new CrmebException("上传文件不能为空");
        if (file.getSize() > maxSize) throw new CrmebException("文件超过允许大小");
        String contentType = safeContentType(file.getContentType());
        validateContentType(contentType);
        byte[] bytes;
        try { bytes = file.getBytes(); } catch (Exception e) { throw new CrmebException("读取上传文件失败"); }
        String extension = extension(file.getOriginalFilename(), contentType);
        String objectKey = new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
        String normalizedProvider = provider == null ? "LOCAL_PRIVATE" : provider.trim().toUpperCase(Locale.ROOT);
        if ("MINIO".equals(normalizedProvider)) {
            requireMinioConfig();
            s3Client.put(minioEndpoint, minioBucket, objectKey, bytes, contentType, minioAccessKey, minioSecretKey, minioRegion);
        } else if ("LOCAL_PRIVATE".equals(normalizedProvider)) {
            writeLocal(objectKey, bytes);
        } else {
            throw new CrmebException("不支持的文件存储类型：" + normalizedProvider);
        }
        Date now = new Date();
        JkFileObject value = new JkFileObject().setFileNo("FO" + IdWorker.getIdStr())
                .setStorageProvider(normalizedProvider).setBucketName("MINIO".equals(normalizedProvider) ? minioBucket : null)
                .setObjectKey(objectKey).setOriginalName(safeName(file.getOriginalFilename())).setContentType(contentType)
                .setFileSize((long) bytes.length).setFileHash(sha256(bytes)).setBusinessType(businessType)
                .setBusinessId(businessId).setOwnerUserId(ownerUserId)
                .setAccessLevel(isBlank(accessLevel) ? "PRIVATE" : accessLevel).setStatus("ACTIVE")
                .setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
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
            requireMinioConfig();
            return s3Client.get(minioEndpoint, file.getBucketName(), file.getObjectKey(), minioAccessKey, minioSecretKey, minioRegion);
        }
        return readLocal(file.getObjectKey());
    }

    @Override
    public JkFileObject require(Long fileId) {
        JkFileObject file = fileDao.selectById(fileId);
        if (file == null || Boolean.TRUE.equals(file.getIsDeleted()) || !"ACTIVE".equals(file.getStatus())) throw new CrmebException("文件不存在或已失效");
        return file;
    }

    @Override
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        String normalized = provider == null ? "LOCAL_PRIVATE" : provider.trim().toUpperCase(Locale.ROOT);
        result.put("enabled", enabled);
        result.put("provider", normalized);
        result.put("maxSize", maxSize);
        result.put("minioConfigured", !isBlank(minioEndpoint) && !isBlank(minioBucket) && !isBlank(minioAccessKey) && !isBlank(minioSecretKey));
        result.put("ready", enabled && ("LOCAL_PRIVATE".equals(normalized) || ("MINIO".equals(normalized) && Boolean.TRUE.equals(result.get("minioConfigured")))));
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
            output.write(bytes); output.flush(); output.close();
        } catch (Exception e) { throw new CrmebException("写入私有文件存储失败：" + safeMessage(e)); }
    }

    private byte[] readLocal(String key) {
        try {
            File root = new File(localRoot).getCanonicalFile();
            File target = new File(root, key).getCanonicalFile();
            if (!target.getPath().startsWith(root.getPath() + File.separator) || !target.isFile()) throw new IllegalStateException("文件不存在");
            FileInputStream input = new FileInputStream(target);
            byte[] bytes = new byte[(int) target.length()];
            int offset = 0, read;
            while (offset < bytes.length && (read = input.read(bytes, offset, bytes.length - offset)) > 0) offset += read;
            input.close();
            if (offset != bytes.length) throw new IllegalStateException("文件读取不完整");
            return bytes;
        } catch (Exception e) { throw new CrmebException("读取私有文件失败：" + safeMessage(e)); }
    }

    private void requireMinioConfig() {
        if (isBlank(minioEndpoint) || isBlank(minioBucket) || isBlank(minioAccessKey) || isBlank(minioSecretKey)) {
            throw new CrmebException("MinIO 配置不完整");
        }
    }

    private JkFileObjectResponse response(JkFileObject value) {
        return new JkFileObjectResponse().setId(value.getId()).setFileNo(value.getFileNo()).setOriginalName(value.getOriginalName())
                .setContentType(value.getContentType()).setFileSize(value.getFileSize()).setBusinessType(value.getBusinessType())
                .setBusinessId(value.getBusinessId()).setAccessLevel(value.getAccessLevel()).setStatus(value.getStatus())
                .setCreateTime(value.getCreateTime()).setDownloadPath("/api/front/jk/file/" + value.getId() + "/download");
    }

    private void validateContentType(String value) {
        if (!(value.startsWith("image/") || "application/pdf".equals(value))) throw new CrmebException("仅允许图片或 PDF 文件");
    }
    private String safeContentType(String value) { return isBlank(value) ? "application/octet-stream" : value.toLowerCase(Locale.ROOT); }
    private String extension(String name, String contentType) { if (!isBlank(name) && name.lastIndexOf('.') >= 0) { String ext = name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT); if (ext.matches("\\.(png|jpg|jpeg|webp|gif|pdf)")) return ext; } if ("application/pdf".equals(contentType)) return ".pdf"; if (contentType.contains("png")) return ".png"; if (contentType.contains("webp")) return ".webp"; return ".jpg"; }
    private String safeName(String value) { if (isBlank(value)) return "file"; String name = new File(value).getName().replaceAll("[\\r\\n]", ""); return name.length() > 200 ? name.substring(name.length() - 200) : name; }
    private String sha256(byte[] value) { try { byte[] digest = MessageDigest.getInstance("SHA-256").digest(value); StringBuilder out = new StringBuilder(); for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b & 0xff)); return out.toString(); } catch (Exception e) { return new String(value, StandardCharsets.ISO_8859_1).hashCode() + ""; } }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
    private String safeMessage(Exception value) { String message = value.getMessage(); return message == null ? value.getClass().getSimpleName() : message.replace('\r', ' ').replace('\n', ' '); }
}
