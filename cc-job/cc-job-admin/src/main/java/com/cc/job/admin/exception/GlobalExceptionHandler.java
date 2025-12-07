package com.cc.job.admin.exception;

import com.cc.job.xo.common.exception.BusinessException;
import com.xxl.job.core.biz.model.ReturnT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 
 * @author cc-job
 * @since 2025-12-02
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ReturnT<String> handleException(Exception exception) {
        log.error("系统异常: {}", exception.getMessage(), exception);
        return new ReturnT<>(ReturnT.FAIL_CODE, "系统内部错误: " + exception.getMessage());
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ReturnT<String> handleBusinessException(BusinessException exception) {
        log.warn("业务异常: {}", exception.getMessage());
        return new ReturnT<>(ReturnT.FAIL_CODE, exception.getMessage());
    }

    /**
     * 处理参数校验异常（@Validated）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ReturnT<String> handleValidationException(MethodArgumentNotValidException exception) {
        String errorMessage = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", errorMessage);
        return new ReturnT<>(ReturnT.FAIL_CODE, "参数校验失败: " + errorMessage);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ReturnT<String> handleBindException(BindException exception) {
        String errorMessage = exception.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("数据绑定失败: {}", errorMessage);
        return new ReturnT<>(ReturnT.FAIL_CODE, "数据绑定失败: " + errorMessage);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ReturnT<String> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("非法参数: {}", exception.getMessage());
        return new ReturnT<>(ReturnT.FAIL_CODE, "参数错误: " + exception.getMessage());
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ReturnT<String> handleNullPointerException(NullPointerException exception) {
        log.error("空指针异常: {}", exception.getMessage(), exception);
        return new ReturnT<>(ReturnT.FAIL_CODE, "系统内部错误: 空指针异常");
    }
}