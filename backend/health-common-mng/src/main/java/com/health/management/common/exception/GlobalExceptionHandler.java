package com.health.management.common.exception;

import com.health.management.common.enums.ResultCode;
import com.health.management.common.vo.ResponseVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.validation.ConstraintViolationException;

/**
 * 全局异常处理。
 * 注意：AccessDeniedException / BadCredentialsException 不在这里处理，
 * 让 Spring Security 的 ExceptionTranslationFilter 返回真实的 401/403。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    public ResponseVo<Void> handleServiceException(ServiceException e) {
        return ResponseVo.fail(e.getCode(), e.getMessage());
    }

    /**
     * 方法级 @PreAuthorize 拒绝时返回真实 HTTP 403。
     * 使用 ResponseEntity，避免被 RestResponseAdvice 再次包装。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseVo<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ResponseVo.fail(ResultCode.FORBIDDEN.getCode(), "没有访问权限"));
    }

    /**
     * 认证异常兜底（登录接口内部已转换为业务提示）
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ResponseVo<Void>> handleAuthentication(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseVo.fail(ResultCode.UNAUTHORIZED.getCode(), "未登录或登录已过期"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseVo<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return ResponseVo.fail(ResultCode.BAD_REQUEST.getCode(), firstError(e.getBindingResult().getFieldError()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseVo<Void> handleBindException(BindException e) {
        return ResponseVo.fail(ResultCode.BAD_REQUEST.getCode(), firstError(e.getBindingResult().getFieldError()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseVo<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().isEmpty()
                ? "参数校验失败"
                : e.getConstraintViolations().iterator().next().getMessage();
        return ResponseVo.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseVo<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return ResponseVo.fail(ResultCode.BAD_REQUEST.getCode(), "请求参数格式错误");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseVo<Void> handleDuplicateKey(DuplicateKeyException e) {
        return ResponseVo.fail(ResultCode.CONFLICT.getCode(), "数据已存在，请勿重复提交");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseVo<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ResponseVo.fail(ResultCode.BAD_REQUEST.getCode(), "上传文件过大，单个文件不能超过5MB");
    }

    @ExceptionHandler(Exception.class)
    public ResponseVo<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseVo.fail(ResultCode.SYSTEM_ERROR.getCode(), "系统内部错误，请稍后重试");
    }

    private String firstError(FieldError fieldError) {
        if (fieldError == null || fieldError.getDefaultMessage() == null) {
            return "参数校验失败";
        }
        return fieldError.getDefaultMessage();
    }
}
