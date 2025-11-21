package org.maram.bill.common.base;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.common.exception.BusinessException;
import org.maram.bill.common.exception.ForbiddenException;
import org.maram.bill.common.exception.ResourceNotFoundException;
import org.maram.bill.common.exception.UnauthorizedException;
import org.maram.bill.common.utils.Result;
import org.maram.bill.common.utils.ResultCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理所有Controller层抛出的异常，返回标准化的错误响应
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ==================== 自定义业务异常 ====================

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("业务异常 - URI: {}, 错误码: {}, 消息: {}",
                request.getRequestURI(), ex.getCode(), ex.getMessage());
        return Result.error(ex.getResultCode(), ex.getData() != null ? ex.getData() : ex.getMessage());
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("资源未找到 - URI: {}, 消息: {}", request.getRequestURI(), ex.getMessage());
        return Result.error(ex.getResultCode(), ex.getMessage());
    }

    /**
     * 处理未授权异常
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleUnauthorizedException(UnauthorizedException ex, HttpServletRequest request) {
        log.warn("未授权访问 - URI: {}, 消息: {}", request.getRequestURI(), ex.getMessage());
        return Result.error(ResultCode.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * 处理禁止访问异常
     */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleForbiddenException(ForbiddenException ex, HttpServletRequest request) {
        log.warn("禁止访问 - URI: {}, 消息: {}", request.getRequestURI(), ex.getMessage());
        return Result.error(ResultCode.FORBIDDEN, ex.getMessage());
    }

    // ==================== Spring Security 异常 ====================

    /**
     * 处理 Spring Security 认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("认证失败 - URI: {}, 消息: {}", request.getRequestURI(), ex.getMessage());
        return Result.error(ResultCode.UNAUTHORIZED, "认证失败，请重新登录");
    }

    /**
     * 处理 Spring Security 访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("访问被拒绝 - URI: {}, 消息: {}", request.getRequestURI(), ex.getMessage());
        return Result.error(ResultCode.FORBIDDEN, "无权限访问此资源");
    }

    // ==================== 参数验证异常 ====================

    /**
     * 处理 @Valid 验证失败异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "无效值",
                        (existing, replacement) -> existing  // 处理重复键
                ));

        log.warn("参数验证失败: {}", errors);
        return Result.error(ResultCode.VALIDATION_ERROR, errors);
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        log.warn("缺少请求参数: {}", ex.getParameterName());
        return Result.error(ResultCode.BAD_REQUEST, String.format("缺少必要参数: %s", ex.getParameterName()));
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("参数类型不匹配: {} = {}", ex.getName(), ex.getValue());
        return Result.error(ResultCode.BAD_REQUEST,
                String.format("参数 '%s' 类型错误，期望类型: %s",
                        ex.getName(),
                        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "未知"));
    }

    /**
     * 处理请求体解析失败异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败: {}", ex.getMessage());
        return Result.error(ResultCode.BAD_REQUEST, "请求体格式错误，请检查JSON格式");
    }

    // ==================== HTTP 请求异常 ====================

    /**
     * 处理不支持的 HTTP 方法异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<?> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("不支持的HTTP方法: {}", ex.getMethod());
        return Result.error(405, String.format("不支持 %s 请求方法", ex.getMethod()));
    }

    /**
     * 处理不支持的 MediaType 异常
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<?> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("不支持的媒体类型: {}", ex.getContentType());
        return Result.error(415, "不支持的媒体类型");
    }

    /**
     * 处理 404 异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.warn("接口不存在: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return Result.error(ResultCode.NOT_FOUND, String.format("接口不存在: %s", ex.getRequestURL()));
    }

    // ==================== 文件上传异常 ====================

    /**
     * 处理文件大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("文件大小超出限制: {}", ex.getMessage());
        return Result.error(ResultCode.FILE_SIZE_EXCEEDED, "文件大小超出限制，最大允许10MB");
    }

    // ==================== 通用异常 ====================

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("非法参数 - URI: {}, 消息: {}", request.getRequestURI(), ex.getMessage());
        return Result.error(ResultCode.BAD_REQUEST, ex.getMessage());
    }

    /**
     * 处理所有未捕获的异常（兜底处理）
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleAllUncaughtException(Exception ex, HttpServletRequest request) {
        log.error("未捕获的异常 - URI: {}, 异常类型: {}, 消息: {}",
                request.getRequestURI(),
                ex.getClass().getName(),
                ex.getMessage(),
                ex);

        // 生产环境不暴露异常详情
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR, "服务器内部错误，请稍后重试");
    }
}
