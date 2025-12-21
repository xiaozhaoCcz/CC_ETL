package com.cc.job.xo.common.exception;


import com.cc.job.xo.common.result.IResultCode;
import org.slf4j.helpers.MessageFormatter;

/**
 * 自定义业务异常
 *
 * @author Ray
 * @since 2022/7/31
 */
public class BusinessException extends RuntimeException {

    public IResultCode resultCode;

    public BusinessException(IResultCode errorCode) {
        super(errorCode.getMsg());
        this.resultCode = errorCode;
    }

    public IResultCode getResultCode() {
        return resultCode;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(Throwable cause) {
        super(cause);
    }

    public BusinessException(String message, Object... args) {
        super(formatMessage(message, args));
    }

    private static String formatMessage(String message, Object... args) {
        return MessageFormatter.arrayFormat(message, args).getMessage();
    }
}
