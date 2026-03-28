package com.zhuoyue.platform.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回体。
 * 所有公开接口都返回同一种结构，前端处理会更稳定，
 * 后期接入埋点、网关或错误告警时也更方便统一收敛。
 *
 * @param <T> 具体数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private boolean success;

    private String code;

    private String message;

    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(true, "0", "ok", data);
    }

    public static <T> Result<T> failure(String code, String message) {
        return new Result<>(false, code, message, null);
    }
}

