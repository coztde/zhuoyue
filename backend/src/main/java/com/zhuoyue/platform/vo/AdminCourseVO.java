package com.zhuoyue.platform.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 后台课程展览管理视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCourseVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private String taskTitle;

    private Integer stageOrder;
}
