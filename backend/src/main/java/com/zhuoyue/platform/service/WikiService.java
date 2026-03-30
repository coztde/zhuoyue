package com.zhuoyue.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuoyue.platform.dto.WikiCommentRequest;
import com.zhuoyue.platform.dto.WikiLikeRequest;
import com.zhuoyue.platform.dto.WikiPostRequest;
import com.zhuoyue.platform.dto.WikiReportRequest;
import com.zhuoyue.platform.vo.WikiCommentVO;
import com.zhuoyue.platform.vo.WikiPostVO;

import java.util.List;

public interface WikiService {

    Page<WikiPostVO> listPosts(int pageNum, int pageSize, String sort, String keyword, String tag, String voterToken);

    WikiPostVO getPost(Long id, String voterToken);

    WikiPostVO createPost(WikiPostRequest request);

    WikiPostVO updatePost(Long id, WikiPostRequest request);

    /** 获取编辑锁，成功返回最新文章VO，失败抛 BizException */
    WikiPostVO lockPost(Long id, String token);

    /** 释放编辑锁 */
    void unlockPost(Long id, String token);

    List<WikiCommentVO> listComments(Long postId, String voterToken);

    WikiCommentVO addComment(Long postId, WikiCommentRequest request);

    /** 点赞或取消点赞，返回最新点赞数和是否已点赞 */
    WikiPostVO toggleLike(WikiLikeRequest request, String voterToken);

    void report(WikiReportRequest request);

    /** 定时刷新所有文章热度 */
    void refreshHeatScores();

    // 管理员操作
    void adminDeletePost(Long id);
    void adminDeleteComment(Long id);
    Page<com.zhuoyue.platform.vo.WikiReportVO> adminListReports(int pageNum, int pageSize);
    void adminHandleReport(Long reportId);
}
