package org.maram.bill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.entity.InvoiceFile;
import org.maram.bill.entity.User;
import org.maram.bill.integration.FileStorageException;
import org.maram.bill.integration.QiniuStorageClient;
import org.maram.bill.mapper.InvoiceFileMapper;
import org.maram.bill.mapper.UserMapper;
import org.maram.bill.service.InvoiceFileService;
import org.maram.bill.service.support.FileMetadataService;
import org.maram.bill.service.support.FileMetadataService.FileMetadata;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 发票文件服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceFileServiceImpl extends ServiceImpl<InvoiceFileMapper, InvoiceFile> implements InvoiceFileService {

    private static final String ERROR_INVALID_FILE_ID = "无效的文件ID格式";
    private static final String ERROR_INVALID_FILE_TYPE = "非法文件类型";
    private static final String ERROR_UPLOAD_FAILED = "文件上传失败";
    private static final String ERROR_USER_NOT_FOUND = "未找到有效用户";

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final QiniuStorageClient qiniuStorageClient;
    private final FileMetadataService fileMetadataService;

    /**
     * 上传发票文件（带补偿事务）
     *
     * 流程：
     * 1. 先上传到七牛云（外部操作，无法回滚）
     * 2. 保存数据库记录（在事务中）
     * 3. 如果数据库失败，主动删除七牛云文件（补偿操作）
     */
    @Override
    public String uploadInvoiceFile(byte[] fileData, String fileName, Long userId) {
        String qiniuKey = null;

        try {
            // 步骤1：验证并上传到七牛云
            FileMetadata metadata = fileMetadataService.validateAndResolve(fileName);
            QiniuStorageClient.UploadResult uploadResult = qiniuStorageClient.upload(
                    fileData, fileName, metadata.extension(), metadata.mimeType());
            qiniuKey = uploadResult.key();

            // 步骤2：在事务中保存数据库记录
            String fileId = saveInvoiceFileWithTransaction(fileName, uploadResult, metadata.mimeType(), fileData.length, userId);

            log.info("文件上传成功: filename={}, userId={}, fileId={}, qiniuKey={}",
                    fileName, userId, fileId, qiniuKey);
            return fileId;

        } catch (IllegalArgumentException ex) {
            log.error("{}: filename={}", ERROR_INVALID_FILE_TYPE, fileName, ex);
            compensateQiniuUpload(qiniuKey, fileName);
            return null;
        } catch (FileStorageException ex) {
            log.error("{}: filename={}, userId={}", ERROR_UPLOAD_FAILED, fileName, userId, ex);
            // 七牛上传失败，无需补偿
            return null;
        } catch (Exception ex) {
            log.error("文件上传异常: filename={}, userId={}", fileName, userId, ex);
            // 补偿：删除已上传到七牛的文件
            compensateQiniuUpload(qiniuKey, fileName);
            return null;
        }
    }

    /**
     * 在事务中保存发票文件记录
     * 如果此方法抛出异常，数据库操作会回滚
     */
    @Transactional(rollbackFor = Exception.class)
    protected String saveInvoiceFileWithTransaction(String fileName, QiniuStorageClient.UploadResult uploadResult,
                                                     String mimeType, int fileSize, Long userId) {
        InvoiceFile invoiceFile = createInvoiceFile(fileName, uploadResult, mimeType, fileSize, userId);
        int rows = baseMapper.insert(invoiceFile);

        if (rows <= 0) {
            throw new RuntimeException("数据库插入失败，影响行数为 0");
        }

        return invoiceFile.getId().toString();
    }

    /**
     * 补偿操作：删除七牛云上的文件
     * 当数据库保存失败时调用
     */
    private void compensateQiniuUpload(String qiniuKey, String fileName) {
        if (qiniuKey == null) {
            log.debug("无需补偿：qiniuKey 为空");
            return;
        }

        log.warn("执行补偿操作：删除七牛云文件, key={}, fileName={}", qiniuKey, fileName);
        boolean deleted = qiniuStorageClient.delete(qiniuKey);

        if (deleted) {
            log.info("补偿成功：已删除七牛云文件, key={}", qiniuKey);
        } else {
            log.error("补偿失败：无法删除七牛云文件, key={}，请手动清理", qiniuKey);
        }
    }

    @Override
    public String saveInvoiceFile(byte[] fileData, String fileName, String openid) {
        try {
            Long userId = findUserIdByOpenid(openid);
            if (userId == null) {
                log.error("{}: openid={}", ERROR_USER_NOT_FOUND, openid);
                return null;
            }

            log.info("找到用户ID {}, openid={}", userId, openid);
            return uploadInvoiceFile(fileData, fileName, userId);
            
        } catch (Exception ex) {
            log.error("通过 OpenID 上传文件异常: openid={}, filename={}", openid, fileName, ex);
            return null;
        }
    }

    @Override
    public String uploadInvoiceFileByOpenid(byte[] fileData, String fileName, String openid) {
        return saveInvoiceFile(fileData, fileName, openid);
    }

    @Override
    public boolean deleteInvoiceFile(String fileId) {
        return parseFileId(fileId)
                .map(id -> {
                    int result = baseMapper.deleteById(id);
                    if (result > 0) {
                        log.info("成功删除文件: id={}", fileId);
                        return true;
                    }
                    log.warn("删除文件失败，未找到记录: id={}", fileId);
                    return false;
                })
                .orElse(false);
    }

    @Override
    public String getInvoiceFileInfo(String fileId) {
        return parseFileId(fileId)
                .map(baseMapper::selectById)
                .map(file -> {
                    try {
                        return objectMapper.writeValueAsString(file);
                    } catch (Exception ex) {
                        log.error("序列化文件信息失败: id={}", fileId, ex);
                        return null;
                    }
                })
                .orElse(null);
    }

    @Override
    public boolean existsInvoiceFile(String fileId) {
        return parseFileId(fileId)
                .map(baseMapper::selectById)
                .isPresent();
    }

    @Override
    public InvoiceFile getInvoiceFileEntity(String fileId) {
        return parseFileId(fileId)
                .map(baseMapper::selectById)
                .orElse(null);
    }

    private InvoiceFile createInvoiceFile(String fileName, QiniuStorageClient.UploadResult uploadResult, 
                                          String mimeType, int fileSize, Long userId) {
        InvoiceFile invoiceFile = new InvoiceFile();
        invoiceFile.setFileName(fileName);
        invoiceFile.setFileUrl(uploadResult.url());
        invoiceFile.setFileType(mimeType);
        invoiceFile.setFileSize(uploadResult.size() > 0 ? uploadResult.size() : (long) fileSize);
        invoiceFile.setUserId(userId);
        invoiceFile.setCreateTime(LocalDateTime.now());
        invoiceFile.setUpdateTime(LocalDateTime.now());
        return invoiceFile;
    }

    private Long findUserIdByOpenid(String openid) {
        return userMapper.findByOpenid(openid)
                .filter(user -> user.getId() != null)
                .map(User::getId)
                .orElse(null);
    }

    private Optional<Long> parseFileId(String fileId) {
        try {
            return Optional.of(Long.parseLong(fileId));
        } catch (NumberFormatException ex) {
            log.error("{}: {}", ERROR_INVALID_FILE_ID, fileId);
            return Optional.empty();
        }
    }
}
