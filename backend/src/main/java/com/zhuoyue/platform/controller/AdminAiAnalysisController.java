package com.zhuoyue.platform.controller;

import com.zhuoyue.platform.common.result.Result;
import com.zhuoyue.platform.service.AiAnalysisService;
import com.zhuoyue.platform.vo.StageProgressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 分析接口。
 * 支持单阶段分析、批量分析单学生、全员分析、查询进度、人工覆盖评语。
 * 所有接口均需 Bearer Token，由 AdminAuthInterceptor 统一拦截鉴权。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    /**
     * 分析单个学生在单个阶段的完成情况。
     * 请求体可选传 repoId，不传则使用主仓库。
     */
    @PostMapping("/analyze/{studentId}/{stageId}")
    public Result<StageProgressVO> analyze(@PathVariable Long studentId,
                                           @PathVariable Long stageId,
                                           @RequestBody(required = false) Map<String, Object> body) {
        Long repoId = body != null && body.get("repoId") != null
            ? Long.parseLong(body.get("repoId").toString()) : null;
        return Result.success(aiAnalysisService.analyzeStageProgress(studentId, stageId, repoId));
    }

    /**
     * 批量分析指定学生所有阶段。
     * 请求体可选传 repoId，不传则使用主仓库。异步执行，立即返回。
     */
    @PostMapping("/analyze/{studentId}")
    public Result<String> analyzeStudent(@PathVariable Long studentId,
                                         @RequestBody(required = false) Map<String, Object> body) {
        Long repoId = body != null && body.get("repoId") != null
            ? Long.parseLong(body.get("repoId").toString()) : null;
        new Thread(() -> {
            try {
                aiAnalysisService.analyzeStudent(studentId, repoId);
            } catch (Exception e) {
                log.error("批量分析学生异常，studentId={}：{}", studentId, e.getMessage(), e);
            }
        }, "ai-analyze-student-" + studentId).start();
        return Result.success("分析已在后台启动");
    }

    /**
     * 全量分析所有学生所有阶段（24小时内已分析的自动跳过）。
     * 异步执行，立即返回。
     */
    @PostMapping("/analyze/all")
    public Result<String> analyzeAll() {
        new Thread(() -> {
            try {
                aiAnalysisService.analyzeAll();
            } catch (Exception e) {
                log.error("全量 AI 分析异常：{}", e.getMessage(), e);
            }
        }, "ai-analyze-all").start();
        return Result.success("全量分析已在后台启动");
    }

    /**
     * 查询指定学生所有阶段的进度和 AI 分析结果。
     */
    @GetMapping("/progress/{studentId}")
    public Result<List<StageProgressVO>> listProgress(@PathVariable Long studentId) {
        return Result.success(aiAnalysisService.listProgressByStudent(studentId));
    }

    /**
     * 人工覆盖某条进度记录的评分和评语。
     * manualScore/manualFeedback 传 null 表示清除人工覆盖，恢复显示 AI 结果。
     */
    @PutMapping("/progress/{progressId}/manual")
    public Result<StageProgressVO> updateManual(@PathVariable Long progressId,
                                                @RequestBody Map<String, Object> body) {
        Integer manualScore = body.get("manualScore") != null
            ? ((Number) body.get("manualScore")).intValue() : null;
        String manualFeedback = (String) body.get("manualFeedback");
        return Result.success(aiAnalysisService.updateManual(progressId, manualScore, manualFeedback));
    }
}
