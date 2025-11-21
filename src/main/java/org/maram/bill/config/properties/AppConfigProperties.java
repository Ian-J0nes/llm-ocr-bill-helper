package org.maram.bill.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppConfigProperties {

    private Pagination pagination;
    private File file;
    private Cache cache;

    @Data
    public static class Pagination {
        private int defaultSize;
        private int maxSize;
    }

    @Data
    public static class File {
        private List<String> allowedImageTypes;
        private List<String> allowedDocumentTypes;
    }

    @Data
    public static class Cache {
        private long expireSeconds;
    }
}
