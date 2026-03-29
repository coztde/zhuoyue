package com.zhuoyue.platform.client;

import java.time.LocalDateTime;

/** Shared commit info record for GitHub and Gitee clients. */
public record CommitInfo(String sha, LocalDateTime commitTime, String message) {}
