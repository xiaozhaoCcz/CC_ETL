package com.cc.job.admin.exception;

/**
 * 无权限访问（返回 403）
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
