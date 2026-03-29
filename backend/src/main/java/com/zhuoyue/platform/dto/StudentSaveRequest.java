package com.zhuoyue.platform.dto;

import lombok.Data;

/**
 * 新增/编辑学生的请求体。
 */
@Data
public class StudentSaveRequest {

    /** 真实姓名，最长 50 个字符。 */
    private String realName;

    /** 平台：GITHUB 或 GITEE。 */
    private String platform;

    /** 平台用户名。 */
    private String platformUsername;

}
