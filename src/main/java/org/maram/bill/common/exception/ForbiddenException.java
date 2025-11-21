package org.maram.bill.common.exception;

import org.maram.bill.common.utils.ResultCode;

/**
 * 禁止访问异常
 * 当用户无权访问某资源时抛出
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException() {
        super(ResultCode.FORBIDDEN);
    }

    public ForbiddenException(String message) {
        super(ResultCode.FORBIDDEN, message);
    }
}
