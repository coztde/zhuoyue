package com.zhuoyue.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhuoyue.platform.entity.MissionStage;
import com.zhuoyue.platform.mapper.MissionStageMapper;
import com.zhuoyue.platform.service.MissionQueryService;
import com.zhuoyue.platform.vo.MissionStageVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程展览查询服务。
 */
@Service
public class MissionQueryServiceImpl implements MissionQueryService {

    private final MissionStageMapper missionStageMapper;

    public MissionQueryServiceImpl(MissionStageMapper missionStageMapper) {
        this.missionStageMapper = missionStageMapper;
    }

    @Override
    public List<MissionStageVO> listMissionStages() {
        List<MissionStage> stages = missionStageMapper.selectList(new LambdaQueryWrapper<MissionStage>()
                .orderByAsc(MissionStage::getStageOrder));
        return stages.stream()
                .map(stage -> MissionStageVO.builder()
                        .id(stage.getId())
                        .title(stage.getTitle())
                        .taskTitle(stage.getTaskTitle())
                        .stageOrder(stage.getStageOrder())
                        .build())
                .toList();
    }
}
