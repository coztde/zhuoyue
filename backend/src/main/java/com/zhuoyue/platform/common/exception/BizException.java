package com.zhuoyue.platform.common.exception;

import lombok.Getter;

/**
 * 业务异常。
 * 这里不把异常直接散落在 Controller 中，而是统一抛出业务异常，
 * 让全局异常处理器决定如何返回给前端。
 */
@Getter
public class BizException extends RuntimeException {

    private final String code;

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }
}

