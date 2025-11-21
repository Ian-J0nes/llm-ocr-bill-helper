package org.maram.bill.common.utils;

/**
 * API响应状态码的中心化枚举。
 * 该枚举定义了所有业务和系统级别的状态码，为API提供了一致且可维护的错误码系统。
 * 状态码按业务领域进行分组，便于管理和查找。
 *
 * <ul>
 *   <li><b>1xxx</b>: 用户相关错误</li>
 *   <li><b>2xxx</b>: 账单和分类相关错误</li>
 *   <li><b>3xxx</b>: 文件相关错误</li>
 *   <li><b>4xxx</b>: AI服务相关错误</li>
 *   <li><b>5xxx</b>: 汇率服务相关错误</li>
 * </ul>
 */
public enum ResultCode {
    // --- 通用HTTP状态码 ---
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    UNAUTHORIZED(401, "未授权访问，请先登录"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "请求的资源不存在"),
    BAD_REQUEST(400, "请求参数错误或无效"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务当前不可用"),
    VALIDATION_ERROR(400, "参数校验失败"),

    // --- 业务相关错误码 (1000-1999) - 用户模块 ---
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    INVALID_PASSWORD(1003, "密码错误"),
    ACCOUNT_LOCKED(1004, "账户已被锁定"),
    ACCOUNT_EXPIRED(1005, "账户已过期"),

    // --- 业务相关错误码 (2000-2999) - 账单模块 ---
    BILL_NOT_FOUND(2001, "账单不存在"),
    BILL_ALREADY_EXISTS(2002, "账单已存在"),
    BILL_FILE_ASSOCIATED(2003, "文件已关联到其他账单"),
    BILL_CREATION_FAILED(2004, "账单创建失败"),
    BILL_UPDATE_FAILED(2005, "账单更新失败"),
    BILL_DELETION_FAILED(2006, "账单删除失败"),
    BILL_PERMISSION_DENIED(2007, "无权限操作此账单"),
    CATEGORY_NOT_FOUND(2101, "分类不存在"),
    BUDGET_NOT_FOUND(2201, "预算不存在"),
    DUPLICATE_CATEGORY_NAME(2102, "分类名称已存在"),
    DUPLICATE_CATEGORY_CODE(2103, "分类编码已存在"),

    // --- 业务相关错误码 (3000-3999) - 文件模块 ---
    FILE_UPLOAD_FAILED(3001, "文件上传失败"),
    FILE_NOT_FOUND(3002, "文件不存在"),
    FILE_TYPE_NOT_SUPPORTED(3003, "不支持的文件类型"),
    FILE_SIZE_EXCEEDED(3004, "文件大小超出限制"),

    // --- 业务相关错误码 (4000-4999) - AI模块 ---
    AI_SERVICE_ERROR(4001, "AI服务异常，请稍后再试"),
    AI_RESPONSE_PARSE_ERROR(4002, "无法解析AI服务的响应"),

    // --- 业务相关错误码 (5000-5999) - 汇率模块 ---
    EXCHANGE_RATE_NOT_FOUND(5001, "无法获取指定货币的汇率"),
    CURRENCY_NOT_SUPPORTED(5002, "暂不支持该货币"),
    EXCHANGE_RATE_SERVICE_ERROR(5003, "汇率服务提供商异常");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
