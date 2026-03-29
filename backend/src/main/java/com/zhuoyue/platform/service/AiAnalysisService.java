package com.zhuoyue.platform.service;

import com.zhuoyue.platform.vo.StageProgressVO;

import java.util.List;

/**
 * AI 分析 Service 接口。
 * 负责将学生 commit 记录（message + diff）发给千问大模型，
 * 对照阶段任务要求给出完成度评分和评语，并支持人工覆盖修改。
 */
public interface AiAnalysisService {

    /**
     * 分析单个学生在单个阶段的完成情况。
     * repoId 为 null 时使用主仓库，不为 null 时使用指定仓库。
     */
    StageProgressVO analyzeStageProgress(Long studentId, Long stageId, Long repoId);

    /**
     * 批量分析单个学生所有阶段。
     * repoId 为 null 时使用主仓库，不为 null 时使用指定仓库。
     */
    void analyzeStudent(Long studentId, Long repoId);

    /**
     * 全量分析所有 ACTIVE 学生的所有阶段（供定时任务调用）。
     * 跳过 24 小时内已分析过的记录，避免重复消耗 token。
     */
    void analyzeAll();

    /**
     * 查询某学生所有阶段的进度和 AI 分析结果。
     *
     * @param studentId 学生 ID
     * @return 进度 VO 列表
     */
    List<StageProgressVO> listProgressByStudent(Long studentId);

    /**
     * 人工覆盖某条进度记录的评分和评语。
     *
     * @param progressId    进度记录 ID
     * @param manualScore   人工评分（0-100，null 表示清除人工覆盖）
     * @param manualFeedback 人工评语（null 表示清除人工覆盖）
     * @return 更新后的进度 VO
     */
    StageProgressVO updateManual(Long progressId, Integer manualScore, String manualFeedback);
}
