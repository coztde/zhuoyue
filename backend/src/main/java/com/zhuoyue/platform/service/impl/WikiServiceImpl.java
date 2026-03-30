package com.zhuoyue.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuoyue.platform.common.constant.CacheConstants;
import com.zhuoyue.platform.common.exception.BizException;
import com.zhuoyue.platform.dto.WikiCommentRequest;
import com.zhuoyue.platform.dto.WikiLikeRequest;
import com.zhuoyue.platform.dto.WikiPostRequest;
import com.zhuoyue.platform.dto.WikiReportRequest;
import com.zhuoyue.platform.entity.WikiComment;
import com.zhuoyue.platform.entity.WikiLike;
import com.zhuoyue.platform.entity.WikiPost;
import com.zhuoyue.platform.entity.WikiReport;
import com.zhuoyue.platform.mapper.WikiCommentMapper;
import com.zhuoyue.platform.mapper.WikiLikeMapper;
import com.zhuoyue.platform.mapper.WikiPostMapper;
import com.zhuoyue.platform.mapper.WikiReportMapper;
import com.zhuoyue.platform.service.WikiService;
import com.zhuoyue.platform.vo.WikiCommentVO;
import com.zhuoyue.platform.vo.WikiPostVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiServiceImpl implements WikiService {

    private final WikiPostMapper postMapper;
    private final WikiCommentMapper commentMapper;
    private final WikiLikeMapper likeMapper;
    private final WikiReportMapper reportMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Page<WikiPostVO> listPosts(int pageNum, int pageSize, String sort, String keyword, String tag, String voterToken) {
        LambdaQueryWrapper<WikiPost> wrapper = new LambdaQueryWrapper<WikiPost>()
                .eq(WikiPost::getIsHidden, false);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(WikiPost::getTitle, keyword).or().like(WikiPost::getContent, keyword));
        }
        if (tag != null && !tag.isBlank() && !"全部".equals(tag)) {
            wrapper.eq(WikiPost::getTag, tag);
        }

        if ("new".equals(sort)) {
            wrapper.orderByDesc(WikiPost::getCreatedAt);
        } else {
            wrapper.orderByDesc(WikiPost::getHeatScore);
        }

        Page<WikiPost> page = postMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 查询当前用户点赞集合
        Set<Long> likedPostIds = getLikedIds("POST", voterToken);

        Page<WikiPostVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(p -> toPostVO(p, likedPostIds.contains(p.getId()), voterToken))
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public WikiPostVO getPost(Long id, String voterToken) {
        WikiPost post = postMapper.selectById(id);
        if (post == null || Boolean.TRUE.equals(post.getIsHidden())) {
            throw new BizException("NOT_FOUND", "文章不存在");
        }
        Set<Long> likedPostIds = getLikedIds("POST", voterToken);
        return toPostVO(post, likedPostIds.contains(id), voterToken);
    }

    @Override
    @Transactional
    public WikiPostVO createPost(WikiPostRequest request) {
        validatePostRequest(request);
        WikiPost post = new WikiPost();
        post.setTitle(request.getTitle().trim());
        post.setContent(request.getContent().trim());
        post.setAuthorName(blankDefault(request.getAuthorName(), "匿名"));
        post.setAuthorToken(request.getAuthorToken());
        post.setTag(blankDefault(request.getTag(), "文章"));
        post.setVersion(1);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setReportCount(0);
        post.setHeatScore(0.0);
        post.setIsHidden(false);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.insert(post);
        refreshHeatScore(post);
        return toPostVO(post, false, request.getAuthorToken());
    }

    /** 编辑锁有效期：5 分钟 */
    private static final int LOCK_MINUTES = 5;

    @Override
    @Transactional
    public WikiPostVO lockPost(Long id, String token) {
        if (token == null || token.isBlank()) throw new BizException("INVALID_PARAM", "缺少身份标识");
        WikiPost post = postMapper.selectById(id);
        if (post == null || Boolean.TRUE.equals(post.getIsHidden())) throw new BizException("NOT_FOUND", "文章不存在");

        // 判断锁是否被他人持有且未超时
        if (isLockedByOther(post, token)) {
            throw new BizException("LOCKED", "当前有人正在编辑此文章，请稍后再试");
        }

        // 加锁（自己已持有则刷新时间）
        postMapper.update(null, new LambdaUpdateWrapper<WikiPost>()
                .eq(WikiPost::getId, id)
                .set(WikiPost::getEditingByToken, token)
                .set(WikiPost::getEditingSince, LocalDateTime.now()));

        WikiPost updated = postMapper.selectById(id);
        Set<Long> liked = getLikedIds("POST", token);
        return toPostVO(updated, liked.contains(id), token);
    }

    @Override
    @Transactional
    public void unlockPost(Long id, String token) {
        WikiPost post = postMapper.selectById(id);
        if (post == null) return;
        // 只有锁持有者或锁已超时才能解锁
        if (token != null && token.equals(post.getEditingByToken()) || isLockExpired(post)) {
            postMapper.update(null, new LambdaUpdateWrapper<WikiPost>()
                    .eq(WikiPost::getId, id)
                    .set(WikiPost::getEditingByToken, (Object) null)
                    .set(WikiPost::getEditingSince, (Object) null));
        }
    }

    @Override
    @Transactional
    public WikiPostVO updatePost(Long id, WikiPostRequest request) {
        validatePostRequest(request);
        if (request.getVersion() == null) {
            throw new BizException("INVALID_PARAM", "缺少版本号");
        }
        WikiPost post = postMapper.selectById(id);
        if (post == null || Boolean.TRUE.equals(post.getIsHidden())) {
            throw new BizException("NOT_FOUND", "文章不存在");
        }
        // 乐观锁：version 必须匹配
        int updated = postMapper.update(null, new LambdaUpdateWrapper<WikiPost>()
                .eq(WikiPost::getId, id)
                .eq(WikiPost::getVersion, request.getVersion())
                .set(WikiPost::getTitle, request.getTitle().trim())
                .set(WikiPost::getContent, request.getContent().trim())
                .set(WikiPost::getAuthorName, blankDefault(request.getAuthorName(), post.getAuthorName()))
                .set(WikiPost::getTag, blankDefault(request.getTag(), post.getTag()))
                .set(WikiPost::getEditReason, request.getEditReason())
                .set(WikiPost::getEditingByToken, (Object) null)
                .set(WikiPost::getEditingSince, (Object) null)
                .set(WikiPost::getVersion, request.getVersion() + 1)
                .set(WikiPost::getUpdatedAt, LocalDateTime.now()));
        if (updated == 0) {
            throw new BizException("CONFLICT", "文章已被他人修改，请刷新后重试");
        }
        WikiPost updated2 = postMapper.selectById(id);
        refreshHeatScore(updated2);
        return toPostVO(updated2, false, request.getAuthorToken());
    }

    @Override
    public List<WikiCommentVO> listComments(Long postId, String voterToken) {
        List<WikiComment> all = commentMapper.selectList(
                new LambdaQueryWrapper<WikiComment>()
                        .eq(WikiComment::getPostId, postId)
                        .eq(WikiComment::getIsHidden, false)
                        .orderByAsc(WikiComment::getCreatedAt));

        Set<Long> likedIds = getLikedIds("COMMENT", voterToken);

        // 构建树形结构
        Map<Long, WikiCommentVO> voMap = all.stream()
                .collect(Collectors.toMap(WikiComment::getId, c -> toCommentVO(c, likedIds.contains(c.getId()), voterToken)));

        return all.stream()
                .filter(c -> c.getParentId() == null)
                .map(c -> {
                    WikiCommentVO vo = voMap.get(c.getId());
                    vo.setChildren(all.stream()
                            .filter(child -> c.getId().equals(child.getParentId()))
                            .map(child -> voMap.get(child.getId()))
                            .collect(Collectors.toList()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WikiCommentVO addComment(Long postId, WikiCommentRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BizException("INVALID_PARAM", "评论内容不能为空");
        }
        if (request.getAuthorToken() == null || request.getAuthorToken().isBlank()) {
            throw new BizException("INVALID_PARAM", "缺少身份标识");
        }
        WikiPost post = postMapper.selectById(postId);
        if (post == null || Boolean.TRUE.equals(post.getIsHidden())) {
            throw new BizException("NOT_FOUND", "文章不存在");
        }
        WikiComment comment = new WikiComment();
        comment.setPostId(postId);
        comment.setParentId(request.getParentId());
        comment.setAuthorName(blankDefault(request.getAuthorName(), "匿名"));
        comment.setAuthorToken(request.getAuthorToken());
        comment.setContent(request.getContent().trim());
        comment.setLikeCount(0);
        comment.setReportCount(0);
        comment.setIsHidden(false);
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);

        // 更新文章评论数和热度
        postMapper.update(null, new LambdaUpdateWrapper<WikiPost>()
                .eq(WikiPost::getId, postId)
                .setSql("comment_count = comment_count + 1"));
        WikiPost updated = postMapper.selectById(postId);
        refreshHeatScore(updated);

        return toCommentVO(comment, false, request.getAuthorToken());
    }

    @Override
    @Transactional
    public WikiPostVO toggleLike(WikiLikeRequest request, String voterToken) {
        if (request.getTargetId() == null || request.getTargetType() == null) {
            throw new BizException("INVALID_PARAM", "参数不完整");
        }
        String token = voterToken != null ? voterToken : request.getVoterToken();
        if (token == null || token.isBlank()) {
            throw new BizException("INVALID_PARAM", "缺少身份标识");
        }

        WikiLike existing = likeMapper.selectOne(new LambdaQueryWrapper<WikiLike>()
                .eq(WikiLike::getTargetType, request.getTargetType())
                .eq(WikiLike::getTargetId, request.getTargetId())
                .eq(WikiLike::getVoterToken, token));

        boolean isPost = "POST".equals(request.getTargetType());
        String likesKey = CacheConstants.WIKI_LIKES_PREFIX + request.getTargetType() + ":" + token;
        String targetIdStr = String.valueOf(request.getTargetId());
        if (existing != null) {
            // 取消点赞
            likeMapper.deleteById(existing.getId());
            redisTemplate.opsForSet().remove(likesKey, targetIdStr);
            if (isPost) {
                postMapper.update(null, new LambdaUpdateWrapper<WikiPost>()
                        .eq(WikiPost::getId, request.getTargetId())
                        .setSql("like_count = GREATEST(like_count - 1, 0)"));
            } else {
                commentMapper.update(null, new LambdaUpdateWrapper<WikiComment>()
                        .eq(WikiComment::getId, request.getTargetId())
                        .setSql("like_count = GREATEST(like_count - 1, 0)"));
            }
        } else {
            // 点赞
            WikiLike like = new WikiLike();
            like.setTargetType(request.getTargetType());
            like.setTargetId(request.getTargetId());
            like.setVoterToken(token);
            like.setCreatedAt(LocalDateTime.now());
            likeMapper.insert(like);
            redisTemplate.opsForSet().add(likesKey, targetIdStr);
            redisTemplate.expire(likesKey, LIKES_TTL);
            if (isPost) {
                postMapper.update(null, new LambdaUpdateWrapper<WikiPost>()
                        .eq(WikiPost::getId, request.getTargetId())
                        .setSql("like_count = like_count + 1"));
            } else {
                commentMapper.update(null, new LambdaUpdateWrapper<WikiComment>()
                        .eq(WikiComment::getId, request.getTargetId())
                        .setSql("like_count = like_count + 1"));
            }
        }

        if (isPost) {
            WikiPost post = postMapper.selectById(request.getTargetId());
            refreshHeatScore(post);
            return toPostVO(post, existing == null, token);
        }
        return null;
    }

    @Override
    @Transactional
    public void report(WikiReportRequest request) {
        if (request.getTargetId() == null || request.getTargetType() == null) {
            throw new BizException("INVALID_PARAM", "参数不完整");
        }
        WikiReport report = new WikiReport();
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setReporterToken(request.getReporterToken());
        report.setIsHandled(false);
        report.setCreatedAt(LocalDateTime.now());
        reportMapper.insert(report);

        // 更新举报数
        if ("POST".equals(request.getTargetType())) {
            postMapper.update(null, new LambdaUpdateWrapper<WikiPost>()
                    .eq(WikiPost::getId, request.getTargetId())
                    .setSql("report_count = report_count + 1"));
            WikiPost post = postMapper.selectById(request.getTargetId());
            if (post != null) refreshHeatScore(post);
        } else {
            commentMapper.update(null, new LambdaUpdateWrapper<WikiComment>()
                    .eq(WikiComment::getId, request.getTargetId())
                    .setSql("report_count = report_count + 1"));
        }
    }

    @Override
    public void refreshHeatScores() {
        List<WikiPost> posts = postMapper.selectList(
                new LambdaQueryWrapper<WikiPost>().eq(WikiPost::getIsHidden, false));
        for (WikiPost post : posts) {
            refreshHeatScore(post);
        }
        log.info("[Wiki] 热度刷新完成，共 {} 篇", posts.size());
    }

    @Override
    @Transactional
    public void adminDeletePost(Long id) {
        postMapper.update(null, new LambdaUpdateWrapper<WikiPost>()
                .eq(WikiPost::getId, id)
                .set(WikiPost::getIsHidden, true));
    }

    @Override
    @Transactional
    public void adminDeleteComment(Long id) {
        commentMapper.update(null, new LambdaUpdateWrapper<WikiComment>()
                .eq(WikiComment::getId, id)
                .set(WikiComment::getIsHidden, true));
    }

    @Override
    public Page<com.zhuoyue.platform.vo.WikiReportVO> adminListReports(int pageNum, int pageSize) {
        Page<WikiReport> page = reportMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<WikiReport>()
                        .eq(WikiReport::getIsHandled, false)
                        .orderByDesc(WikiReport::getCreatedAt));

        // 批量查评论的 postId
        List<Long> commentIds = page.getRecords().stream()
                .filter(r -> "COMMENT".equals(r.getTargetType()))
                .map(WikiReport::getTargetId)
                .distinct().toList();
        Map<Long, Long> commentPostIds = commentIds.isEmpty() ? Map.of() :
                commentMapper.selectBatchIds(commentIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                WikiComment::getId, WikiComment::getPostId));

        List<com.zhuoyue.platform.vo.WikiReportVO> vos = page.getRecords().stream().map(r -> {
            com.zhuoyue.platform.vo.WikiReportVO vo = new com.zhuoyue.platform.vo.WikiReportVO();
            vo.setId(r.getId());
            vo.setTargetType(r.getTargetType());
            vo.setTargetId(r.getTargetId());
            vo.setReason(r.getReason());
            vo.setReporterToken(r.getReporterToken());
            vo.setIsHandled(r.getIsHandled());
            vo.setCreatedAt(r.getCreatedAt());
            if ("COMMENT".equals(r.getTargetType())) {
                vo.setPostId(commentPostIds.get(r.getTargetId()));
            } else {
                vo.setPostId(r.getTargetId());
            }
            return vo;
        }).toList();

        Page<com.zhuoyue.platform.vo.WikiReportVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    @Transactional
    public void adminHandleReport(Long reportId) {
        reportMapper.update(null, new LambdaUpdateWrapper<WikiReport>()
                .eq(WikiReport::getId, reportId)
                .set(WikiReport::getIsHandled, true));
    }

    // ── 内部工具方法 ──────────────────────────────────────────────────────────

    /**
     * 热度公式：点赞×3 + 评论×2 - 举报×5 + 时间衰减
     * 时间衰减：100 / log10(发布距今小时数 + 10)
     */
    private void refreshHeatScore(WikiPost post) {
        long hoursAgo = ChronoUnit.HOURS.between(post.getCreatedAt(), LocalDateTime.now());
        double decay = 100.0 / Math.log10(hoursAgo + 10);
        double heat = post.getLikeCount() * 3.0
                + post.getCommentCount() * 2.0
                - post.getReportCount() * 5.0
                + decay;
        postMapper.update(null, new LambdaUpdateWrapper<WikiPost>()
                .eq(WikiPost::getId, post.getId())
                .set(WikiPost::getHeatScore, Math.max(heat, 0)));
    }

    private static final Duration LIKES_TTL = Duration.ofHours(2);

    private Set<Long> getLikedIds(String targetType, String voterToken) {
        if (voterToken == null || voterToken.isBlank()) return Set.of();
        String key = CacheConstants.WIKI_LIKES_PREFIX + targetType + ":" + voterToken;
        Set<String> cached = redisTemplate.opsForSet().members(key);
        if (cached != null && !cached.isEmpty()) {
            return cached.stream().map(Long::valueOf).collect(Collectors.toSet());
        }
        Set<Long> ids = likeMapper.selectList(new LambdaQueryWrapper<WikiLike>()
                        .eq(WikiLike::getTargetType, targetType)
                        .eq(WikiLike::getVoterToken, voterToken))
                .stream().map(WikiLike::getTargetId).collect(Collectors.toSet());
        if (!ids.isEmpty()) {
            redisTemplate.opsForSet().add(key, ids.stream().map(String::valueOf).toArray(String[]::new));
            redisTemplate.expire(key, LIKES_TTL);
        }
        return ids;
    }

    private WikiPostVO toPostVO(WikiPost post, boolean liked, String voterToken) {
        WikiPostVO vo = new WikiPostVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setAuthorName(post.getAuthorName());
        vo.setTag(post.getTag());
        vo.setVersion(post.getVersion());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setHeatScore(post.getHeatScore());
        vo.setCreatedAt(post.getCreatedAt());
        vo.setUpdatedAt(post.getUpdatedAt());
        vo.setLiked(liked);
        vo.setIsAuthor(voterToken != null && voterToken.equals(post.getAuthorToken()));
        vo.setIsEditing(isLockedByOther(post, voterToken));
        vo.setEditReason(post.getEditReason());
        return vo;
    }

    private WikiCommentVO toCommentVO(WikiComment comment, boolean liked, String voterToken) {
        WikiCommentVO vo = new WikiCommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setParentId(comment.getParentId());
        vo.setAuthorName(comment.getAuthorName());
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikeCount());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setLiked(liked);
        vo.setIsAuthor(voterToken != null && voterToken.equals(comment.getAuthorToken()));
        return vo;
    }

    private boolean isLockExpired(WikiPost post) {
        return post.getEditingSince() == null ||
               post.getEditingSince().plusMinutes(LOCK_MINUTES).isBefore(LocalDateTime.now());
    }

    private boolean isLockedByOther(WikiPost post, String token) {
        if (post.getEditingByToken() == null || isLockExpired(post)) return false;
        return !post.getEditingByToken().equals(token);
    }

    private void validatePostRequest(WikiPostRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BizException("INVALID_PARAM", "标题不能为空");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BizException("INVALID_PARAM", "内容不能为空");
        }
        if (request.getAuthorToken() == null || request.getAuthorToken().isBlank()) {
            throw new BizException("INVALID_PARAM", "缺少身份标识");
        }
        if (request.getTitle().length() > 200) {
            throw new BizException("INVALID_PARAM", "标题不能超过200字");
        }
    }

    private String blankDefault(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }
}
