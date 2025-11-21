package org.maram.bill.service.support;

import org.maram.bill.config.properties.AppConfigProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * 校验并解析上传文件的元数据。
 */
@Component
public class FileMetadataService {

    private final Set<String> allowedImageTypes;
    private final Set<String> allowedDocumentTypes;

    public FileMetadataService(AppConfigProperties appConfigProperties) {
        this.allowedImageTypes = Set.copyOf(appConfigProperties.getFile().getAllowedImageTypes());
        this.allowedDocumentTypes = Set.copyOf(appConfigProperties.getFile().getAllowedDocumentTypes());
    }

    /**
     * 验证文件扩展名并返回对应的元数据。
     *
     * @param fileName 原始文件名
     * @return FileMetadata 包含扩展名和 MIME 类型
     */
    public FileMetadata validateAndResolve(String fileName) {
        String extension = extractExtension(fileName);
        if (!isAllowed(extension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension);
        }
        String mimeType = resolveMimeType(extension);
        return new FileMetadata(extension, mimeType);
    }

    private boolean isAllowed(String extension) {
        if (!StringUtils.hasText(extension)) {
            return false;
        }
        String lowerExt = extension.toLowerCase(Locale.ROOT);
        return allowedImageTypes.contains(lowerExt) || allowedDocumentTypes.contains(lowerExt);
    }

    private String extractExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private String resolveMimeType(String extension) {
        String lowerExt = extension.toLowerCase(Locale.ROOT);
        switch (lowerExt) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default:
                return "application/octet-stream";
        }
    }

    public record FileMetadata(String extension, String mimeType) {}
}
