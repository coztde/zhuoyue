package com.zhuoyue.platform.service;

/**
 * Git 同步 Service 接口。
 * 负责从 GitHub/Gitee 拉取 commit 数据并更新学生阶段进度。
 */
public interface GitSyncService {

    /** 全量同步所有 ACTIVE 状态的学生。 */
    void syncAll();

    /** 同步单个学生的所有阶段进度。 */
    void syncStudent(Long studentId);
}
