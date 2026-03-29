package com.zhuoyue.platform.controller;

import com.zhuoyue.platform.common.result.Result;
import com.zhuoyue.platform.service.GitSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员手动触发同步接口。
 * 由 AdminAuthInterceptor 统一鉴权，无需额外处理 token。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
public class AdminSyncController {

    private final GitSyncService gitSyncService;

    /**
     * 手动触发全量同步。
     * 同步为同步执行，数据量大时前端需展示 loading 状态等待。
     */
    @PostMapping("/all")
    public Result<Void> syncAll() {
        log.info("管理员手动触发全量同步");
        gitSyncService.syncAll();
        return Result.success(null);
    }

    /** 手动同步指定学生。 */
    @PostMapping("/{studentId}")
    public Result<Void> syncOne(@PathVariable Long studentId) {
        log.info("管理员手动同步学生，id={}", studentId);
        gitSyncService.syncStudent(studentId);
        return Result.success(null);
    }
}
