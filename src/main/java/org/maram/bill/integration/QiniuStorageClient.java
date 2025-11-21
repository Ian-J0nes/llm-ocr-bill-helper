package org.maram.bill.integration;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.config.properties.QiniuProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class QiniuStorageClient {

    private static final DateTimeFormatter DATE_PATH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final UploadManager uploadManager;
    private final BucketManager bucketManager;
    private final Auth auth;
    private final String bucket;
    private final String domain;

    public QiniuStorageClient(QiniuProperties properties) {
        if (!StringUtils.hasText(properties.getAccessKey()) || !StringUtils.hasText(properties.getSecretKey())) {
            throw new IllegalStateException("七牛云 AccessKey/SecretKey 未配置");
        }
        if (!StringUtils.hasText(properties.getBucket())) {
            throw new IllegalStateException("七牛云 Bucket 未配置");
        }

        Configuration configuration = new Configuration(resolveRegion(properties.getRegion()));
        configuration.useHttpsDomains = properties.isUseHttps();
        this.uploadManager = new UploadManager(configuration);
        this.auth = Auth.create(properties.getAccessKey(), properties.getSecretKey());
        this.bucketManager = new BucketManager(this.auth, configuration);
        this.bucket = properties.getBucket();
        this.domain = normalizeDomain(properties.getDomain(), properties.isUseHttps());
        if (this.domain == null) {
            log.warn("未配置七牛云访问域名，将仅返回对象 key");
        }
    }

    public UploadResult upload(byte[] data, String fileName, String extension, String mimeType) {
        String key = buildObjectKey(extension);
        String uploadToken = auth.uploadToken(bucket);

        StringMap params = new StringMap();
        if (StringUtils.hasText(fileName)) {
            params.put("x:originalName", fileName);
        }
        if (!StringUtils.hasText(mimeType)) {
            mimeType = "application/octet-stream";
        }

        try {
            Response response = uploadManager.put(data, key, uploadToken, params, mimeType, false);
            if (!response.isOK()) {
                log.error("七牛上传失败, status={}, body={}", response.statusCode, response.bodyString());
                throw new FileStorageException("七牛上传失败，状态码:" + response.statusCode);
            }

            long size = extractFileSize(response, data.length);
            String url = domain != null ? domain + "/" + key : key;
            log.info("七牛上传成功, key={}, size={} bytes", key, size);
            return new UploadResult(key, url, size);

        } catch (QiniuException ex) {
            String response = ex.response != null ? ex.response.toString() : "";
            log.error("七牛上传异常, key={}, response={} ", key, response, ex);
            throw new FileStorageException("上传文件到七牛失败", ex);
        }
    }

    private Region resolveRegion(String regionCode) {
        if (!StringUtils.hasText(regionCode)) {
            return Region.autoRegion();
        }
        String code = regionCode.trim().toLowerCase(Locale.ROOT);
        return switch (code) {
            case "z0", "huadong" -> Region.huadong();
            case "z1", "huabei" -> Region.huabei();
            case "z2", "huanan" -> Region.huanan();
            case "na0", "northamerica", "beimei" -> Region.beimei();
            case "as0", "xinjiapo", "ap-southeast" -> Region.xinjiapo();
            default -> Region.autoRegion();
        };
    }

    private String normalizeDomain(String rawDomain, boolean useHttps) {
        if (!StringUtils.hasText(rawDomain)) {
            return null;
        }
        String domain = rawDomain.trim();
        if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
            domain = (useHttps ? "https://" : "http://") + domain;
        }
        if (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        return domain;
    }

    private String buildObjectKey(String extension) {
        String datePath = DATE_PATH_FORMAT.format(LocalDate.now());
        String randomId = UUID.randomUUID().toString().replaceAll("-", "");
        String suffix = StringUtils.hasText(extension) ? "." + extension.toLowerCase(Locale.ROOT) : "";
        return "invoice/" + datePath + "/" + randomId + suffix;
    }

    private long extractFileSize(Response response, long fallback) {
        try {
            if (response == null) {
                return fallback;
            }
            Map<String, Object> map = new Gson().fromJson(response.bodyString(), Map.class);
            if (map != null && map.containsKey("fsize")) {
                Object value = map.get("fsize");
                if (value instanceof Number number) {
                    return number.longValue();
                }
            }
        } catch (Exception ex) {
            log.debug("解析七牛响应大小失败，使用回退值", ex);
        }
        return fallback;
    }

    /**
     * 删除七牛云上的文件（补偿操作）
     *
     * @param key 七牛云对象 key
     * @return 是否删除成功
     */
    public boolean delete(String key) {
        if (!StringUtils.hasText(key)) {
            log.warn("删除操作：key 为空，跳过");
            return false;
        }

        try {
            Response response = bucketManager.delete(bucket, key);
            if (response.isOK()) {
                log.info("七牛删除成功, key={}", key);
                return true;
            } else {
                log.error("七牛删除失败, key={}, status={}, body={}",
                        key, response.statusCode, response.bodyString());
                return false;
            }
        } catch (QiniuException ex) {
            log.error("七牛删除异常, key={}", key, ex);
            return false;
        }
    }

    public record UploadResult(String key, String url, long size) {
    }
}
