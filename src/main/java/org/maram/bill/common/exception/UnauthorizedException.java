package org.maram.bill.common.exception;

import org.maram.bill.common.utils.ResultCode;

/**
 * 未授权异常
 * 当用户未登录或凭证无效时抛出
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException() {
        super(ResultCode.UNAUTHORIZED);
    }

    public UnauthorizedException(String message) {
        super(ResultCode.UNAUTHORIZED, message);
    }
}
