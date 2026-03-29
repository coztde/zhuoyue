package com.zhuoyue.platform.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Gitee API 客户端。
 * 调用 gitee.com/api/v5 获取仓库 commit 列表、commit diff、用户信息、用户所有公开仓库。
 * 频率限制：认证请求 1000次/小时，请务必配置 GITEE_TOKEN。
 */
@Slf4j
@Component
public class GiteeApiClient {

    private static final String BASE_URL = "https://gitee.com/api/v5";

    /** Gitee 私人令牌，通过环境变量 GITEE_TOKEN 注入。 */
    @Value("${git.sync.gitee-token:}")
    private String giteeToken;

    /** 构建 token 参数字符串（Gitee 用 query param 方式传 token）。 */
    private String tokenParam(boolean first) {
        if (giteeToken.isBlank()) return "";
        return (first ? "?" : "&") + "access_token=" + giteeToken;
    }

    /**
     * 获取指定仓库在给定时间范围内的 commit 列表（含 sha、时间、message）。
     *
     * @param repoName 仓库全名，格式 owner/repo
     * @param since    起始时间（可为 null）
     * @param until    截止时间（可为 null）
     * @return commit 信息列表
     */
    @SuppressWarnings("unchecked")
    public List<CommitInfo> fetchCommits(String repoName, LocalDateTime since, LocalDateTime until) {
        try {
            StringBuilder url = new StringBuilder(BASE_URL)
                .append("/repos/").append(repoName).append("/commits?limit=100")
                .append(tokenParam(false));
            if (since != null) url.append("&since=").append(since).append("+00:00");
            if (until != null) url.append("&until=").append(until).append("+00:00");

            List<Map<String, Object>> raw = RestClient.create().get()
                .uri(url.toString()).retrieve().body(List.class);

            if (raw == null || raw.isEmpty()) return Collections.emptyList();

            // Gitee commit 响应结构：commit.author.date / commit.message
            return raw.stream().map(c -> {
                try {
                    String sha = (String) c.get("sha");
                    Map<String, Object> inner = (Map<String, Object>) c.get("commit");
                    Map<String, Object> author = (Map<String, Object>) inner.get("author");
                    String message = (String) inner.get("message");
                    LocalDateTime time = OffsetDateTime.parse((String) author.get("date")).toLocalDateTime();
                    return new CommitInfo(sha, time, message);
                } catch (Exception e) {
                    return null;
                }
            }).filter(c -> c != null).toList();

        } catch (Exception e) {
            log.warn("Gitee 获取 commit 列表失败，仓库={}，原因：{}", repoName, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取单个 commit 的 diff 内容，供 AI 分析代码变更。
     * Gitee API: GET /repos/{owner}/{repo}/commits/{sha}
     * 每个文件 patch 最多保留 300 行，防止超出大模型 token 限制。
     *
     * @param repoName 仓库全名，格式 owner/repo
     * @param sha      commit SHA
     * @return 拼接后的 diff 字符串
     */
    @SuppressWarnings("unchecked")
    public String fetchCommitDiff(String repoName, String sha) {
        try {
            String url = BASE_URL + "/repos/" + repoName + "/commits/" + sha + tokenParam(true);
            Map<String, Object> detail = RestClient.create().get().uri(url).retrieve().body(Map.class);
            if (detail == null) return "";

            List<Map<String, Object>> files = (List<Map<String, Object>>) detail.get("files");
            if (files == null || files.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> file : files) {
                sb.append("文件：").append(file.get("filename")).append("\n");
                sb.append(truncateLines((String) file.getOrDefault("patch", ""), 300));
                sb.append("\n---\n");
            }
            return sb.toString();

        } catch (Exception e) {
            log.warn("Gitee 获取 commit diff 失败，仓库={}，sha={}，原因：{}", repoName, sha, e.getMessage());
            return "";
        }
    }

    /**
     * 获取指定用户的所有公开仓库全名列表（owner/repo 格式）。
     * 用于「一键扫描」功能，自动导入用户所有公开仓库。
     *
     * @param username Gitee 用户名
     * @return 仓库全名列表
     */
    @SuppressWarnings("unchecked")
    public List<String> fetchAllPublicRepos(String username) {
        try {
            String url = BASE_URL + "/users/" + username + "/repos?type=public&limit=100" + tokenParam(false);
            List<Map<String, Object>> repos = RestClient.create().get().uri(url).retrieve().body(List.class);
            if (repos == null || repos.isEmpty()) return Collections.emptyList();
            return repos.stream().map(r -> (String) r.get("full_name")).filter(n -> n != null).toList();
        } catch (Exception e) {
            log.warn("Gitee 获取用户仓库列表失败，username={}，原因：{}", username, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取 Gitee 用户信息（头像、昵称）。
     *
     * @param username Gitee 用户名
     * @return 包含 avatar_url 和 name 的 Map，失败返回空 Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchUserInfo(String username) {
        try {
            String url = BASE_URL + "/users/" + username + tokenParam(true);
            return RestClient.create().get().uri(url).retrieve().body(Map.class);
        } catch (Exception e) {
            log.warn("获取 Gitee 用户信息失败，username={}，原因：{}", username, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /** 截取字符串前 maxLines 行，防止 diff 内容过长超出 token 限制。 */
    private String truncateLines(String text, int maxLines) {
        if (text == null || text.isBlank()) return "";
        String[] lines = text.split("\n");
        if (lines.length <= maxLines) return text;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLines; i++) sb.append(lines[i]).append("\n");
        sb.append("...(已截断)");
        return sb.toString();
    }

}
