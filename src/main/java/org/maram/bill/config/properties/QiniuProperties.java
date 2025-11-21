package org.maram.bill.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "qiniu")
public class QiniuProperties {

    /**
     * AccessKey for Qiniu Cloud.
     */
    private String accessKey;

    /**
     * SecretKey for Qiniu Cloud.
     */
    private String secretKey;

    /**
     * Bucket name.
     */
    private String bucket;

    /**
     * Domain for accessing files (with protocol, e.g. https://cdn.example.com).
     */
    private String domain;

    /**
     * Region identifier (e.g. z0, z1, z2, na0, as0).
     */
    private String region;

    /**
     * Whether to use HTTPS when building URLs.
     */
    private boolean useHttps = true;
}
