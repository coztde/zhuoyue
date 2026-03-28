package com.zhuoyue.platform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程阶段实体。
 * 每个阶段本质上就是一个“能力里程碑”，前端会把它渲染成可视化阶段地图。
 */
@Data
@TableName("mission_stage")
public class MissionStage {

    /**
     * 阶段主键。
     */
    @TableId
    private Long id;

    /**
     * 阶段编码。
     * 主要用于兼容数据库中要求非空的业务编码字段。
     */
    private String code;

    /**
     * 阶段标题。
     */
    private String title;

    /**
     * 阶段任务。
     * 单表方案下，一个阶段只保留一条任务内容。
     */
    private String taskTitle;

    /**
     * 阶段顺序。
     * 数值越小越靠前。
     */
    private Integer stageOrder;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
