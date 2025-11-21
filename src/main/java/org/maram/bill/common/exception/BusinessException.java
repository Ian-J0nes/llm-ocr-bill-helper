package org.maram.bill.common.exception;

import org.maram.bill.common.utils.ResultCode;

/**
 * 业务异常基类
 * 用于表示业务逻辑层面的错误，会被全局异常处理器捕获并返回友好的错误信息
 */
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;
    private final Object data;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
        this.data = null;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
        this.data = null;
    }

    public BusinessException(ResultCode resultCode, Object data) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
        this.data = data;
    }

    public BusinessException(ResultCode resultCode, String message, Object data) {
        super(message);
        this.resultCode = resultCode;
        this.data = data;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }

    public Object getData() {
        return data;
    }

    public Integer getCode() {
        return resultCode.getCode();
    }
}
