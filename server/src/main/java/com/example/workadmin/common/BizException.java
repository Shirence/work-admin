package com.example.workadmin.common;

/**
 * 业务异常：由全局异常处理器翻译成 { code, msg }。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
