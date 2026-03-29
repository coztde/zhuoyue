package com.zhuoyue.platform.service;

import com.zhuoyue.platform.entity.StudentRepo;

import java.util.List;

/**
 * 学生仓库管理 Service 接口。
 * 支持手动添加、一键扫描所有公开仓库、自动关联 github.io 仓库、删除仓库。
 */
public interface StudentRepoService {

    /** 列出指定学生的所有仓库。 */
    List<StudentRepo> listRepos(Long studentId);

    /** 手动添加一个仓库。 */
    StudentRepo addManualRepo(Long studentId, String repoName, String platform);

    /**
     * 自动扫描并导入该学生平台账号下的所有公开仓库。
     * 已存在的仓库（按 repo_name 去重）跳过，不重复添加。
     *
     * @return 新增的仓库数量
     */
    int scanAllPublicRepos(Long studentId);

    /**
     * 自动关联 username.github.io（或 username.gitee.io）仓库。
     * 若仓库已存在则跳过。
     */
    StudentRepo autoLinkPageRepo(Long studentId);

    /**
     * 将指定仓库设为主分析仓库，同一学生的其他仓库自动取消主仓库标记。
     */
    StudentRepo setPrimary(Long studentId, Long repoId);

    /** 删除指定仓库（同时级联清除该仓库的进度记录）。 */
    void deleteRepo(Long repoId);
}
