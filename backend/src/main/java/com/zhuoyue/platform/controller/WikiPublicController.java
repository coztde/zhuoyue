package com.zhuoyue.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuoyue.platform.common.result.Result;
import com.zhuoyue.platform.dto.WikiCommentRequest;
import com.zhuoyue.platform.dto.WikiLikeRequest;
import com.zhuoyue.platform.dto.WikiPostRequest;
import com.zhuoyue.platform.dto.WikiReportRequest;
import com.zhuoyue.platform.service.WikiService;
import com.zhuoyue.platform.vo.WikiCommentVO;
import com.zhuoyue.platform.vo.WikiPostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Wiki 公开接口，无需登录。
 */
@RestController
@RequestMapping("/api/public/wiki")
@RequiredArgsConstructor
public class WikiPublicController {

    private final WikiService wikiService;

    /** 文章列表（分页、搜索、排序） */
    @GetMapping("/posts")
    public Result<Page<WikiPostVO>> listPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "heat") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestHeader(value = "X-Wiki-Token", required = false) String token) {
        return Result.success(wikiService.listPosts(page, size, sort, keyword, tag, token));
    }

    /** 文章详情 */
    @GetMapping("/posts/{id}")
    public Result<WikiPostVO> getPost(
            @PathVariable Long id,
            @RequestHeader(value = "X-Wiki-Token", required = false) String token) {
        return Result.success(wikiService.getPost(id, token));
    }

    /** 发布文章 */
    @PostMapping("/posts")
    public Result<WikiPostVO> createPost(@RequestBody WikiPostRequest request) {
        return Result.success(wikiService.createPost(request));
    }

    /** 获取编辑锁 */
    @PostMapping("/posts/{id}/lock")
    public Result<WikiPostVO> lockPost(
            @PathVariable Long id,
            @RequestHeader(value = "X-Wiki-Token") String token) {
        return Result.success(wikiService.lockPost(id, token));
    }

    /** 释放编辑锁 */
    @PostMapping("/posts/{id}/unlock")
    public Result<Void> unlockPost(
            @PathVariable Long id,
            @RequestHeader(value = "X-Wiki-Token") String token) {
        wikiService.unlockPost(id, token);
        return Result.success(null);
    }

    /** 编辑文章（乐观锁） */
    @PutMapping("/posts/{id}")
    public Result<WikiPostVO> updatePost(
            @PathVariable Long id,
            @RequestBody WikiPostRequest request) {
        return Result.success(wikiService.updatePost(id, request));
    }

    /** 评论列表（树形） */
    @GetMapping("/posts/{postId}/comments")
    public Result<List<WikiCommentVO>> listComments(
            @PathVariable Long postId,
            @RequestHeader(value = "X-Wiki-Token", required = false) String token) {
        return Result.success(wikiService.listComments(postId, token));
    }

    /** 发表评论 */
    @PostMapping("/posts/{postId}/comments")
    public Result<WikiCommentVO> addComment(
            @PathVariable Long postId,
            @RequestBody WikiCommentRequest request) {
        return Result.success(wikiService.addComment(postId, request));
    }

    /** 点赞/取消点赞 */
    @PostMapping("/like")
    public Result<WikiPostVO> toggleLike(
            @RequestBody WikiLikeRequest request,
            @RequestHeader(value = "X-Wiki-Token", required = false) String token) {
        return Result.success(wikiService.toggleLike(request, token));
    }

    /** 举报 */
    @PostMapping("/report")
    public Result<Void> report(@RequestBody WikiReportRequest request) {
        wikiService.report(request);
        return Result.success(null);
    }
}
