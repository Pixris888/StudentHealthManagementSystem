package com.health.management.common.exception;

import com.health.management.common.enums.ResultCode;

/**
 * 业务异常：携带业务状态码，由 GlobalExceptionHandler 统一转换为 ResponseVo。
 */
public class ServiceException extends RuntimeException {

    private final int code;

    public ServiceException(String message) {
        this(ResultCode.FAIL.getCode(), message);
    }

    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ServiceException(ResultCode resultCode, String message) {
        this(resultCode.getCode(), message);
    }

    public int getCode() {
        return code;
    }

    public static ServiceException badRequest(String message) {
        return new ServiceException(ResultCode.BAD_REQUEST, message);
    }

    public static ServiceException unauthorized(String message) {
        return new ServiceException(ResultCode.UNAUTHORIZED, message);
    }

    public static ServiceException forbidden(String message) {
        return new ServiceException(ResultCode.FORBIDDEN, message);
    }

    public static ServiceException notFound(String message) {
        return new ServiceException(ResultCode.NOT_FOUND, message);
    }

    public static ServiceException conflict(String message) {
        return new ServiceException(ResultCode.CONFLICT, message);
    }

    public static ServiceException fail(String message) {
        return new ServiceException(ResultCode.FAIL, message);
    }
}
