package com.zhuoyue.platform.dto;

import lombok.Data;

/**
 * 课程展览阶段保存请求。
 */
@Data
public class CourseSaveRequest {
    private String title;

    private String taskTitle;

    private Integer stageOrder;
}
