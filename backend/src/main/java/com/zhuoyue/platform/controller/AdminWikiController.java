package com.zhuoyue.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuoyue.platform.common.result.Result;
import com.zhuoyue.platform.service.WikiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Wiki 管理员接口，需要 Bearer Token。
 */
@RestController
@RequestMapping("/api/admin/wiki")
@RequiredArgsConstructor
public class AdminWikiController {

    private final WikiService wikiService;

    /** 举报列表（未处理） */
    @GetMapping("/reports")
    public Result<Page<com.zhuoyue.platform.vo.WikiReportVO>> listReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(wikiService.adminListReports(page, size));
    }

    /** 标记举报已处理 */
    @PutMapping("/reports/{id}/handle")
    public Result<Void> handleReport(@PathVariable Long id) {
        wikiService.adminHandleReport(id);
        return Result.success(null);
    }

    /** 删除（隐藏）文章 */
    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        wikiService.adminDeletePost(id);
        return Result.success(null);
    }

    /** 删除（隐藏）评论 */
    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        wikiService.adminDeleteComment(id);
        return Result.success(null);
    }
}
