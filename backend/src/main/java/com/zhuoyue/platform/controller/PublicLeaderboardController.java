package com.zhuoyue.platform.controller;

import com.zhuoyue.platform.common.result.Result;
import com.zhuoyue.platform.service.LeaderboardService;
import com.zhuoyue.platform.vo.LeaderboardItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公开排行榜接口，无需鉴权。
 * AdminAuthInterceptor 只拦截 /api/admin/**，本接口路径为 /api/public/**，自动放行。
 */
@RestController
@RequestMapping("/api/public/leaderboard")
@RequiredArgsConstructor
public class PublicLeaderboardController {

    private final LeaderboardService leaderboardService;

    /**
     * 获取 Top10 排行榜。
     * 结果由 LeaderboardService 缓存在 Redis，TTL 5 分钟。
     */
    @GetMapping
    public Result<List<LeaderboardItemVO>> top10() {
        return Result.success(leaderboardService.getTop10());
    }

    /**
     * 获取全员排行榜（用于前端 ECharts 图表展示各阶段完成人数分布）。
     */
    @GetMapping("/full")
    public Result<List<LeaderboardItemVO>> full() {
        return Result.success(leaderboardService.getFull());
    }
}
