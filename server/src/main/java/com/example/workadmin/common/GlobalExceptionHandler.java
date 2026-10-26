package com.example.workadmin.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

/**
 * 统一异常处理：始终返回 HTTP 200 + 业务 code，方便前端 axios 拦截器统一处理。
 * <p>
 * 未预期的异常只把「错误编号」返回给前端，堆栈和 SQL 写日志——
 * 直接把 e.getMessage() 吐给浏览器会把表名、列名、完整 SQL 一起漏出去。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.error("未处理异常 traceId={}", traceId, e);
        return Result.fail(500, "服务器内部错误，请稍后重试（错误编号 " + traceId + "）");
    }
}
