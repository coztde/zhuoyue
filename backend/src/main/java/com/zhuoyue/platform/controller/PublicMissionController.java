package com.zhuoyue.platform.controller;

import com.zhuoyue.platform.common.result.Result;
import com.zhuoyue.platform.service.MissionQueryService;
import com.zhuoyue.platform.vo.MissionStageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Mission 课程展览接口。
 */
@RestController
@RequestMapping("/api/public/missions")
public class PublicMissionController {

    private final MissionQueryService missionQueryService;

    public PublicMissionController(MissionQueryService missionQueryService) {
        this.missionQueryService = missionQueryService;
    }

    @GetMapping
    public Result<List<MissionStageVO>> list() {
        return Result.success(missionQueryService.listMissionStages());
    }
}

