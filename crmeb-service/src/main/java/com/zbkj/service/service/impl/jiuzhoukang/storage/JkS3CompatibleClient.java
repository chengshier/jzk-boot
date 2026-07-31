package com.zbkj.service.service.impl.jiuzhoukang.storage;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * 仅覆盖九州康私有对象 PUT/GET 的 S3 Signature V4 客户端。
 * 适用于预先创建好的 MinIO Bucket；不负责创建 Bucket 或公开访问策略。
 */
@Component
public class JkS3CompatibleClient {

    public void put(String endpoint, String bucket, String objectKey, byte[] bytes, String contentType,
                    String accessKey, String secretKey, String region) {
        execute("PUT", endpoint, bucket, objectKey, bytes, contentType, accessKey, secretKey, region, false);
    }

    public byte[] get(String endpoint, String bucket, String objectKey, String accessKey, String secretKey, String region) {
        return execute("GET", endpoint, bucket, objectKey, new byte[0], null, accessKey, secretKey, region, true);
    }

    public void delete(String endpoint, String bucket, String objectKey, String accessKey, String secretKey, String region) {
        execute("DELETE", endpoint, bucket, objectKey, new byte[0], null, accessKey, secretKey, region, false);
    }

    /** Verifies credentials without exposing them or retaining the verification object. */
    public void testWriteDelete(String endpoint, String bucket, String accessKey, String secretKey, String region) {
        String objectKey = "__crmeb_connection_test__/" + UUID.randomUUID().toString().replace("-", "");
        RuntimeException writeFailure = null;
        try {
            put(endpoint, bucket, objectKey, "crmeb-minio-connection-test".getBytes(StandardCharsets.UTF_8),
                    "text/plain", accessKey, secretKey, region);
        } catch (RuntimeException error) {
            writeFailure = error;
        }
        try {
            delete(endpoint, bucket, objectKey, accessKey, secretKey, region);
        } catch (RuntimeException cleanupError) {
            if (writeFailure == null) throw new IllegalStateException("MinIO 连接测试清理失败");
        }
        if (writeFailure != null) throw new IllegalStateException("MinIO 连接测试失败");
    }

    private byte[] execute(String method, String endpoint, String bucket, String objectKey, byte[] body, String contentType,
                           String accessKey, String secretKey, String region, boolean returnBody) {
        HttpURLConnection connection = null;
        try {
            URI endpointUri = URI.create(trimSlash(endpoint));
            String canonicalUri = "/" + encodeSegment(bucket) + "/" + encodeObjectKey(objectKey);
            URL url = new URL(endpointUri.getScheme(), endpointUri.getHost(), endpointUri.getPort(), canonicalUri);
            String host = endpointUri.getHost();
            if (endpointUri.getPort() > 0 && endpointUri.getPort() != 80 && endpointUri.getPort() != 443) host += ":" + endpointUri.getPort();
            Date now = new Date();
            String amzDate = format(now, "yyyyMMdd'T'HHmmss'Z'");
            String dateStamp = format(now, "yyyyMMdd");
            String payloadHash = sha256Hex(body == null ? new byte[0] : body);
            String canonicalHeaders = "host:" + host + "\n" +
                    "x-amz-content-sha256:" + payloadHash + "\n" +
                    "x-amz-date:" + amzDate + "\n";
            String signedHeaders = "host;x-amz-content-sha256;x-amz-date";
            String canonicalRequest = method + "\n" + canonicalUri + "\n\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + payloadHash;
            String safeRegion = isBlank(region) ? "us-east-1" : region;
            String scope = dateStamp + "/" + safeRegion + "/s3/aws4_request";
            String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + scope + "\n" + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            byte[] signingKey = signingKey(secretKey, dateStamp, safeRegion);
            String signature = hex(hmac(signingKey, stringToSign));
            String authorization = "AWS4-HMAC-SHA256 Credential=" + accessKey + "/" + scope +
                    ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Host", host);
            connection.setRequestProperty("x-amz-date", amzDate);
            connection.setRequestProperty("x-amz-content-sha256", payloadHash);
            connection.setRequestProperty("Authorization", authorization);
            if (contentType != null) connection.setRequestProperty("Content-Type", contentType);
            if ("PUT".equals(method)) {
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(body.length);
                OutputStream output = connection.getOutputStream();
                output.write(body);
                output.flush();
                output.close();
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                String error = readText(connection.getErrorStream());
                throw new IllegalStateException("MinIO 请求失败，HTTP " + status + (isBlank(error) ? "" : "：" + abbreviate(error, 500)));
            }
            if (!returnBody) return new byte[0];
            return readBytes(connection.getInputStream());
        } catch (Exception e) {
            if (e instanceof IllegalStateException) throw (IllegalStateException) e;
            throw new IllegalStateException("MinIO 对象请求失败：" + safeMessage(e), e);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private byte[] signingKey(String secret, String date, String region) throws Exception {
        byte[] kDate = hmac(("AWS4" + secret).getBytes(StandardCharsets.UTF_8), date);
        byte[] kRegion = hmac(kDate, region);
        byte[] kService = hmac(kRegion, "s3");
        return hmac(kService, "aws4_request");
    }

    private byte[] hmac(byte[] key, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256Hex(byte[] value) throws Exception { return hex(MessageDigest.getInstance("SHA-256").digest(value)); }
    private String hex(byte[] value) { StringBuilder out = new StringBuilder(value.length * 2); for (byte b : value) out.append(String.format(Locale.ROOT, "%02x", b & 0xff)); return out.toString(); }
    private String format(Date date, String pattern) { SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ROOT); format.setTimeZone(TimeZone.getTimeZone("UTC")); return format.format(date); }
    private String trimSlash(String value) { while (value.endsWith("/")) value = value.substring(0, value.length() - 1); return value; }
    private String encodeObjectKey(String value) { String[] parts = value.split("/", -1); StringBuilder out = new StringBuilder(); for (String part : parts) { if (out.length() > 0) out.append('/'); out.append(encodeSegment(part)); } return out.toString(); }
    private String encodeSegment(String value) { byte[] bytes = value.getBytes(StandardCharsets.UTF_8); StringBuilder out = new StringBuilder(); for (byte b : bytes) { int c = b & 0xff; if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.' || c == '~') out.append((char)c); else out.append('%').append(String.format(Locale.ROOT, "%02X", c)); } return out.toString(); }
    private byte[] readBytes(InputStream input) throws Exception { if (input == null) return new byte[0]; ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int read; while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read); input.close(); return output.toByteArray(); }
    private String readText(InputStream input) throws Exception { return new String(readBytes(input), StandardCharsets.UTF_8); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
    private String abbreviate(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
    private String safeMessage(Exception error) { String value = error.getMessage(); return value == null ? error.getClass().getSimpleName() : value.replace('\n', ' ').replace('\r', ' '); }
}
