package org.maram.bill.common.exception;

import org.maram.bill.common.utils.ResultCode;

/**
 * 资源未找到异常
 * 当请求的资源不存在时抛出
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(ResultCode.NOT_FOUND, String.format("%s 不存在: %s", resourceName, identifier));
    }

    public ResourceNotFoundException(ResultCode resultCode) {
        super(resultCode);
    }

    public ResourceNotFoundException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }
}
