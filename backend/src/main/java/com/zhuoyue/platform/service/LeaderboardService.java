package com.zhuoyue.platform.service;

import com.zhuoyue.platform.vo.LeaderboardItemVO;

import java.util.List;

/**
 * 排行榜 Service 接口。
 * 提供带 Redis 缓存的榜单查询。
 */
public interface LeaderboardService {

    /** 获取 Top10 榜单，结果缓存 5 分钟。 */
    List<LeaderboardItemVO> getTop10();

    /** 获取全员榜单，结果缓存 5 分钟。 */
    List<LeaderboardItemVO> getFull();

    /** 主动清除榜单缓存（同步完成后调用）。 */
    void evictCache();
}
