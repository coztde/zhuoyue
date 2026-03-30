package com.zhuoyue.platform.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * GitHub API 客户端。
 * 调用 api.github.com 获取仓库 commit 列表、commit diff、用户信息、用户所有公开仓库。
 * 频率限制：认证请求 5000次/小时，未认证 60次/小时，请务必配置 GITHUB_TOKEN。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubApiClient {

    private final OkHttp3ClientHttpRequestFactory okHttp3RequestFactory;

    private static final String BASE_URL = "https://api.github.com";

    /** GitHub Personal Access Token，通过环境变量 GITHUB_TOKEN 注入。 */
    @Value("${git.sync.github-token:}")
    private String githubToken;

    /** 构建带认证头的 RestClient，使用共享 OkHttp Bean 解决 TLS 问题。 */
    private RestClient buildClient() {
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(okHttp3RequestFactory)
                .baseUrl(BASE_URL);
        if (!githubToken.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + githubToken);
        }
        builder.defaultHeader("Accept", "application/vnd.github+json");
        builder.defaultHeader("X-GitHub-Api-Version", "2022-11-28");
        return builder.build();
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
            StringBuilder url = new StringBuilder("/repos/").append(repoName).append("/commits?per_page=100");
            if (since != null) url.append("&since=").append(since).append("Z");
            if (until != null) url.append("&until=").append(until).append("Z");

            List<Map<String, Object>> raw = buildClient().get()
                .uri(url.toString())
                .retrieve()
                .body(List.class);

            if (raw == null || raw.isEmpty()) return Collections.emptyList();

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
            log.warn("GitHub 获取 commit 列表失败，仓库={}，原因：{}", repoName, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取单个 commit 的 diff 内容（文件名 + patch），供 AI 分析代码变更。
     * 每个文件 patch 最多保留 300 行，防止超出大模型 token 限制。
     *
     * @param repoName 仓库全名，格式 owner/repo
     * @param sha      commit SHA
     * @return 拼接后的 diff 字符串
     */
    @SuppressWarnings("unchecked")
    public String fetchCommitDiff(String repoName, String sha) {
        try {
            Map<String, Object> detail = buildClient().get()
                .uri("/repos/" + repoName + "/commits/" + sha)
                .retrieve()
                .body(Map.class);

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
            log.warn("GitHub 获取 commit diff 失败，仓库={}，sha={}，原因：{}", repoName, sha, e.getMessage());
            return "";
        }
    }

    /**
     * 获取指定用户的所有公开仓库全名列表（owner/repo 格式）。
     * 用于「一键扫描」功能，自动导入用户所有公开仓库。
     *
     * @param username GitHub 用户名
     * @return 仓库全名列表
     */
    @SuppressWarnings("unchecked")
    public List<String> fetchAllPublicRepos(String username) {
        try {
            List<Map<String, Object>> repos = buildClient().get()
                .uri("/users/" + username + "/repos?type=public&per_page=100&sort=updated")
                .retrieve()
                .body(List.class);

            if (repos == null || repos.isEmpty()) return Collections.emptyList();
            return repos.stream().map(r -> (String) r.get("full_name")).filter(n -> n != null).toList();

        } catch (Exception e) {
            log.warn("GitHub 获取用户仓库列表失败，username={}，原因：{}", username, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取 GitHub 用户信息（头像、昵称）。
     *
     * @param username GitHub 用户名
     * @return 包含 avatar_url 和 name 的 Map，失败返回空 Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchUserInfo(String username) {
        try {
            return buildClient().get().uri("/users/" + username).retrieve().body(Map.class);
        } catch (Exception e) {
            log.warn("获取 GitHub 用户信息失败，username={}，原因：{}", username, e.getMessage());
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
