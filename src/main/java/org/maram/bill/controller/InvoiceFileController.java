package org.maram.bill.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.common.security.UserContext;
import org.maram.bill.entity.InvoiceFile;
import org.maram.bill.service.InvoiceFileService;
import org.maram.bill.common.utils.Result;
import org.maram.bill.common.utils.ResultCode;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;

@RestController
@RequestMapping("/files")
@Slf4j
@RequiredArgsConstructor
public class InvoiceFileController {

    private final InvoiceFileService invoiceFileService;
    private final UserContext userContext;

    /**
     * 上传发票文件 (需要认证)
     *
     * @param file 上传的文件
     * @return 包含文件ID的 Result 对象
     */
    @PostMapping
    public Result<String> uploadInvoiceFile(@RequestParam("file") MultipartFile file) {
        return withIdentity(identity -> {
            if (file.isEmpty()) {
                return Result.error("上传失败：文件不能为空");
            }

            log.info("用户 OpenID {} 上传发票文件: {}", identity.openid(), file.getOriginalFilename());
            try {
                String fileId = invoiceFileService.saveInvoiceFile(file.getBytes(), file.getOriginalFilename(), identity.openid());
                if (fileId != null) {
                    return Result.success("文件上传成功，正在后台处理中", fileId);
                }
                log.error("文件上传失败，服务层返回null，OpenID: {}, 文件名: {}", identity.openid(), file.getOriginalFilename());
                return Result.error(ResultCode.FILE_UPLOAD_FAILED);
            } catch (IOException e) {
                log.error("读取上传文件时发生IO异常，OpenID: {}, 文件名: {}", identity.openid(), file.getOriginalFilename(), e);
                return Result.error("文件上传异常: " + e.getMessage());
            } catch (Exception e) {
                log.error("文件上传过程中发生未知异常，OpenID: {}, 文件名: {}", identity.openid(), file.getOriginalFilename(), e);
                return Result.error("文件上传失败，发生未知错误");
            }
        });
    }

    /**
     * 删除发票文件 (需要认证)
     *
     * @param fileId 文件ID
     * @return 操作结果
     */
    @DeleteMapping("/{fileId}")
    public Result<Boolean> deleteInvoiceFile(@PathVariable String fileId) {
        if (fileId == null || fileId.trim().isEmpty()) {
             return Result.error("无效的文件ID");
        }

        return withFile(fileId, (identity, invoiceFile) -> {
            log.info("用户 OpenID {} 请求删除发票文件: {}", identity.openid(), fileId);

            try {
                boolean result = invoiceFileService.deleteInvoiceFile(fileId);
                if (result) {
                    return Result.success("操作成功");
                }
                log.warn("文件删除失败，服务层返回false: {}", fileId);
                if (!invoiceFileService.existsInvoiceFile(fileId)) {
                    return Result.error("文件不存在");
                }
                return Result.error("文件删除失败");
            } catch (Exception e) {
                log.error("删除文件时发生异常: ID={}", fileId, e);
                return Result.error("删除文件失败，发生内部错误");
            }
        });
    }

    /**
     * 获取发票文件信息 (需要认证)
     *
     * @param fileId 文件ID
     * @return 包含文件信息的JSON字符串的 Result 对象
     */
    @GetMapping("/{fileId}")
    public Result<String> getInvoiceFileInfo(@PathVariable String fileId) {
        if (fileId == null || fileId.trim().isEmpty()) {
             return Result.error("无效的文件ID");
        }

        return withFile(fileId, (identity, invoiceFile) -> {
            log.info("用户 OpenID {} 请求获取发票文件信息: {}", identity.openid(), fileId);

            try {
                String fileInfo = invoiceFileService.getInvoiceFileInfo(fileId);
                if (fileInfo != null) {
                    log.debug("成功获取文件信息: {}", fileId);
                    return Result.success(fileInfo);
                }
                log.warn("未找到文件信息: {}", fileId);
                return Result.error("文件不存在");
            } catch (Exception e) {
                log.error("获取文件信息时发生异常: ID={}", fileId, e);
                return Result.error("获取文件信息失败，发生内部错误");
            }
        });
    }

    /**
     * 检查发票文件是否存在 (需要认证)
     *
     * @param fileId 文件ID
     * @return 包含布尔值的 Result 对象，表示文件是否存在
     */
    @GetMapping("/{fileId}/exists")
    public Result<Boolean> existsInvoiceFile(@PathVariable String fileId) {
        if (fileId == null || fileId.trim().isEmpty()) {
             return Result.error("无效的文件ID");
        }

        return withIdentity(identity -> {
            try {
                InvoiceFile invoiceFile = invoiceFileService.getInvoiceFileEntity(fileId);
                boolean exists = invoiceFile != null && identity.userId().equals(invoiceFile.getUserId());
                log.debug("文件存在性检查结果: ID={}, 存在={}, 用户={}", fileId, exists, identity.userId());
                return Result.success(exists);
            } catch (Exception e) {
                log.error("检查文件是否存在时发生异常: ID={}", fileId, e);
                return Result.error("检查文件是否存在时出错");
            }
        });
    }

    private <T> Result<T> withIdentity(Function<UserIdentity, Result<T>> action) {
        String openid = userContext.currentOpenid().orElse(null);
        if (openid == null || openid.isEmpty()) {
            log.warn("无法从认证上下文中获取OpenID，请检查 JWT 认证");
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        Long userId = userContext.currentUserId().orElse(null);
        if (userId == null) {
            log.warn("无法通过 OpenID {} 获取用户ID", openid);
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        return action.apply(new UserIdentity(openid, userId));
    }

    private <T> Result<T> withFile(String fileId, BiFunction<UserIdentity, InvoiceFile, Result<T>> action) {
        return withIdentity(identity -> {
            InvoiceFile invoiceFile = invoiceFileService.getInvoiceFileEntity(fileId);
            if (invoiceFile == null) {
                log.warn("未找到发票文件: {}", fileId);
                return Result.error(ResultCode.FILE_NOT_FOUND);
            }
            if (!identity.userId().equals(invoiceFile.getUserId())) {
                log.warn("用户 OpenID {} 无权访问文件: {}", identity.openid(), fileId);
                return Result.error(ResultCode.FORBIDDEN);
            }
            return action.apply(identity, invoiceFile);
        });
    }

    private record UserIdentity(String openid, Long userId) {
    }

}
