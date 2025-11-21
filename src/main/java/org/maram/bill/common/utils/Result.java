package org.maram.bill.common.utils;

import lombok.Data;

/**
 * 统一API响应结果封装类
 * 该类用于为所有API端点提供一个标准的、一致的返回格式。
 * 它包含了状态码(code)、消息(message)和泛型数据(data)，使得前端能够统一处理响应。
 *
 * @param <T> 响应中包含的数据的类型
 */
@Data
public class Result<T> {

    /**
     * 业务状态码，与 ResultCode 枚举对应。
     */
    private Integer code;

    /**
     * 描述信息，用于向用户展示。
     */
    private String message;

    /**
     * 响应的具体数据，类型为泛型。
     */
    private T data;

    /**
     * 默认构造函数
     */
    public Result() {}

    /**
     * 全参构造函数
     * @param code 状态码
     * @param message 消息
     * @param data 数据
     */
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // --- 成功的静态工厂方法 ---

    /**
     * 返回一个不带数据和自定义消息的成功响应。
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 返回一个带自定义消息但没有数据的成功响应。
     * @param message 成功消息
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, null);
    }

    /**
     * 返回一个带数据的成功响应。
     * @param data 响应数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 返回一个带自定义消息和数据的成功响应。
     * @param message 成功消息
     * @param data 响应数据
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    // --- 失败的静态工厂方法 ---

    /**
     * 返回一个表示通用错误的响应。
     */
    public static <T> Result<T> error() {
        return new Result<>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage(), null);
    }

    /**
     * 返回一个带自定义消息的通用错误响应。
     * @param message 错误消息
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }

    /**
     * 返回一个带自定义状态码和消息的错误响应。
     * @param code 自定义错误码
     * @param message 错误消息
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 根据指定的ResultCode枚举返回一个错误响应。
     * @param resultCode 错误码枚举
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 根据指定的ResultCode枚举和自定义数据返回一个错误响应。
     * @param resultCode 错误码枚举
     * @param data 响应数据
     */
    public static <T> Result<T> error(ResultCode resultCode, T data) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), data);
    }

    // --- HTTP状态相关的静态工厂方法 ---

    /**
     * 返回一个401未授权的响应。
     */
    public static <T> Result<T> unauthorized() {
        return new Result<>(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage(), null);
    }

    /**
     * 返回一个带自定义消息的401未授权响应。
     */
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(ResultCode.UNAUTHORIZED.getCode(), message, null);
    }

    /**
     * 返回一个403禁止访问的响应。
     */
    public static <T> Result<T> forbidden() {
        return new Result<>(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage(), null);
    }

    /**
     * 返回一个带自定义消息的403禁止访问响应。
     */
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(ResultCode.FORBIDDEN.getCode(), message, null);
    }

    /**
     * 返回一个404资源未找到的响应。
     */
    public static <T> Result<T> notFound() {
        return new Result<>(ResultCode.NOT_FOUND.getCode(), ResultCode.NOT_FOUND.getMessage(), null);
    }

    /**
     * 返回一个带自定义消息的404资源未找到响应。
     */
    public static <T> Result<T> notFound(String message) {
        return new Result<>(ResultCode.NOT_FOUND.getCode(), message, null);
    }
    
    /**
     * 返回一个400错误请求的响应。
     */
    public static <T> Result<T> badRequest() {
        return new Result<>(ResultCode.BAD_REQUEST.getCode(), ResultCode.BAD_REQUEST.getMessage(), null);
    }

    /**
     * 返回一个带自定义消息的400错误请求响应。
     */
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(ResultCode.BAD_REQUEST.getCode(), message, null);
    }

    // --- 状态检查方法 ---

    /**
     * 检查当前响应是否表示成功。
     * @return 如果状态码为成功码，则为true。
     */
    public boolean isSuccess() {
        return this.code != null && this.code.equals(ResultCode.SUCCESS.getCode());
    }

}
