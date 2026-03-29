package com.zhuoyue.platform.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuoyue.platform.mapper.StudentStageProgressMapper;
import com.zhuoyue.platform.service.LeaderboardService;
import com.zhuoyue.platform.vo.LeaderboardItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 排行榜 Service 实现。
 * 查询结果写入 Redis，TTL 5 分钟，同步完成后主动清除缓存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    /** Redis 缓存 key：Top10 榜单。 */
    private static final String CACHE_KEY_TOP10 = "leaderboard:top10";
    /** Redis 缓存 key：全员榜单。 */
    private static final String CACHE_KEY_FULL  = "leaderboard:full";
    /** 缓存有效期：5 分钟。 */
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final StudentStageProgressMapper progressMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<LeaderboardItemVO> getTop10() {
        return getFromCacheOrDb(CACHE_KEY_TOP10, () -> progressMapper.queryTop10());
    }

    @Override
    public List<LeaderboardItemVO> getFull() {
        return getFromCacheOrDb(CACHE_KEY_FULL, () -> progressMapper.queryFull());
    }

    @Override
    public void evictCache() {
        redisTemplate.delete(CACHE_KEY_TOP10);
        redisTemplate.delete(CACHE_KEY_FULL);
        log.info("榜单缓存已清除");
    }

    /**
     * 通用缓存读取逻辑：优先从 Redis 取，缓存缺失则查数据库并回填。
     */
    private List<LeaderboardItemVO> getFromCacheOrDb(String key, java.util.function.Supplier<List<LeaderboardItemVO>> dbQuery) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<LeaderboardItemVO>>() {});
            }
        } catch (Exception e) {
            log.warn("读取榜单缓存失败，key={}，原因：{}", key, e.getMessage());
        }

        // 缓存未命中，查询数据库
        List<LeaderboardItemVO> result = dbQuery.get();

        // 回填缓存（Redis 不可用时跳过，不影响正常流程）
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result), CACHE_TTL);
        } catch (Exception e) {
            log.warn("写入榜单缓存失败，key={}，原因：{}", key, e.getMessage());
        }
        return result;
    }
}
