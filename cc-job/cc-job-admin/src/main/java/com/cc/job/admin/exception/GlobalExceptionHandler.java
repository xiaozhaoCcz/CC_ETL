package com.cc.job.admin.exception;

import com.cc.job.xo.common.exception.BusinessException;
import com.xxl.job.core.biz.model.ReturnT;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class) //异常处理器
    @ResponseBody  //返回json数据
    public ReturnT error(Exception exception) {
        exception.printStackTrace();
        return new ReturnT<>(ReturnT.FAIL_CODE, exception.getMessage());
    }

    //自定义异常处理
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ReturnT error(BusinessException exception) {
        exception.printStackTrace();
        return new ReturnT<>(ReturnT.FAIL_CODE, exception.getMessage());
    }
}