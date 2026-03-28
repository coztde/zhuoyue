package com.zhuoyue.platform.common.exception;

import com.zhuoyue.platform.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * 统一异常出口有两个核心价值：
 * 1. 前端拿到的错误结构始终一致；
 * 2. 真正的堆栈和错误上下文只在服务端日志里暴露，避免把内部实现细节直接抛给前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException exception) {
        return Result.failure(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("系统发生未处理异常", exception);
        return Result.failure("SYSTEM_ERROR", "系统开小差了，请稍后重试");
    }
}
