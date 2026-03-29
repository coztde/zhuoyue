package com.zhuoyue.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhuoyue.platform.entity.StudentStageProgress;
import com.zhuoyue.platform.vo.LeaderboardItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** Student stage progress mapper. Provides leaderboard queries in addition to basic CRUD. */
@Mapper
public interface StudentStageProgressMapper extends BaseMapper<StudentStageProgress> {

    /** Query top 10 leaderboard: order by completed stages desc, then latest commit desc. */
    @Select("""
        SELECT
            s.id,
            s.real_name,
            s.avatar_url,
            s.display_name,
            COUNT(CASE WHEN p.is_completed THEN 1 END)  AS completed_stages,
            (SELECT COUNT(*) FROM mission_stage)         AS total_stages,
            MAX(p.last_commit_time)                      AS latest_commit,
            MAX(p.synced_at)                             AS last_synced_at
        FROM student_profile s
        LEFT JOIN student_stage_progress p ON s.id = p.student_id
        WHERE s.active_status = 'ACTIVE'
        GROUP BY s.id, s.real_name, s.avatar_url, s.display_name
        ORDER BY completed_stages DESC, latest_commit DESC
        LIMIT 10
        """)
    List<LeaderboardItemVO> queryTop10();

    /** Query full leaderboard without limit. */
    @Select("""
        SELECT
            s.id,
            s.real_name,
            s.avatar_url,
            s.display_name,
            COUNT(CASE WHEN p.is_completed THEN 1 END)  AS completed_stages,
            (SELECT COUNT(*) FROM mission_stage)         AS total_stages,
            MAX(p.last_commit_time)                      AS latest_commit,
            MAX(p.synced_at)                             AS last_synced_at
        FROM student_profile s
        LEFT JOIN student_stage_progress p ON s.id = p.student_id
        WHERE s.active_status = 'ACTIVE'
        GROUP BY s.id, s.real_name, s.avatar_url, s.display_name
        ORDER BY completed_stages DESC, latest_commit DESC
        """)
    List<LeaderboardItemVO> queryFull();
}
