package com.example.workadmin.common;

import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一异常处理：始终返回 HTTP 200 + 业务 code，方便前端 axios 拦截器统一处理。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidation(Exception e) {
        FieldError fieldError = null;
        if (e instanceof MethodArgumentNotValidException ex) {
            fieldError = ex.getBindingResult().getFieldError();
        } else if (e instanceof BindException ex) {
            fieldError = ex.getBindingResult().getFieldError();
        }
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数不合法";
        return Result.fail(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        return Result.fail(500, "服务器内部错误：" + e.getMessage());
    }
}
