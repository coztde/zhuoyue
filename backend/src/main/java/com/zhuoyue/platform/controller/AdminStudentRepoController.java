package com.zhuoyue.platform.controller;

import com.zhuoyue.platform.common.result.Result;
import com.zhuoyue.platform.entity.StudentRepo;
import com.zhuoyue.platform.service.StudentRepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生仓库管理接口。
 * 支持手动添加仓库、一键扫描所有公开仓库、自动关联主页仓库、删除仓库。
 * 所有接口均需 Bearer Token，由 AdminAuthInterceptor 统一拦截鉴权。
 */
@RestController
@RequestMapping("/api/admin/students/{studentId}/repos")
@RequiredArgsConstructor
public class AdminStudentRepoController {

    private final StudentRepoService studentRepoService;

    /** 列出指定学生的所有仓库。 */
    @GetMapping
    public Result<List<StudentRepo>> list(@PathVariable Long studentId) {
        return Result.success(studentRepoService.listRepos(studentId));
    }

    /**
     * 手动添加一个仓库。
     * 请求体：{ "repoName": "owner/repo", "platform": "GITHUB" }
     */
    @PostMapping
    public Result<StudentRepo> add(@PathVariable Long studentId,
                                   @RequestBody Map<String, String> body) {
        String repoName = body.get("repoName");
        String platform = body.get("platform");
        return Result.success(studentRepoService.addManualRepo(studentId, repoName, platform));
    }

    /**
     * 一键扫描并导入该学生平台账号下的所有公开仓库。
     * 已存在的仓库自动去重跳过。
     */
    @PostMapping("/scan")
    public Result<Map<String, Integer>> scan(@PathVariable Long studentId) {
        int added = studentRepoService.scanAllPublicRepos(studentId);
        return Result.success(Map.of("added", added));
    }

    /**
     * 自动关联 username.github.io（或 username.gitee.io）主页仓库。
     */
    @PostMapping("/link-page")
    public Result<StudentRepo> linkPage(@PathVariable Long studentId) {
        return Result.success(studentRepoService.autoLinkPageRepo(studentId));
    }

    /** 将指定仓库设为主分析仓库，AI 分析默认使用此仓库。 */
    @PutMapping("/{repoId}/primary")
    public Result<StudentRepo> setPrimary(@PathVariable Long studentId,
                                          @PathVariable Long repoId) {
        return Result.success(studentRepoService.setPrimary(studentId, repoId));
    }

    /** 删除指定仓库。 */
    @DeleteMapping("/{repoId}")
    public Result<Void> delete(@PathVariable Long studentId,
                               @PathVariable Long repoId) {
        studentRepoService.deleteRepo(repoId);
        return Result.success(null);
    }
}
