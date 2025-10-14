/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.service.guide.equalSampleService.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.guide.equalSampleMapper.EqualSampleMapper;
import com.persee.workstation.uv_inexus.dto.concise.common.EqualAndSample;
import com.persee.workstation.uv_inexus.dto.concise.common.EqualSampleDto;
import com.persee.workstation.uv_inexus.dto.concise.common.EqualSampleParam;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskService;
import com.persee.workstation.uv_inexus.service.guide.equalSampleService.EqualSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EqualSampleServiceImpl extends ServiceImpl<EqualSampleMapper, EqualAndSample> implements EqualSampleService {
    @Autowired
    private EqualSampleMapper equalSampleMapper;
    @Autowired
    private FMeasureTaskService fMeasureTaskService;

    public static final List<Map<String, List<String>>> equalSamplePool = new ArrayList<>();


    @Override
    public Result<List<Map<String, List<String>>>> addEqualSample(EqualSampleParam equalSampleParam) {
        String equalSampleKey = equalSampleParam.getEqualSampleKey();
        List<String> sampleIds = equalSampleParam.getSampleIds();
        String sampleId = sampleIds.get(0);
        EqualAndSample equalAndSample = new EqualAndSample();
        List<FMeasureTask> fMeasureTasks = fMeasureTaskService.list();
        for (FMeasureTask fMeasureTask : fMeasureTasks) {
            String sampleKey = fMeasureTask.getSampleKey();
            if (sampleKey == null) {
                continue;
            }
            String taskId = fMeasureTask.getId();
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(sampleKey, TaskSpecimen.class);
            for (TaskSpecimen newtaskSpecimen : taskSpecimen) {
                String specimenKey = newtaskSpecimen.getSpecimenKey();
                if (sampleId.equals(specimenKey)) {
                    equalAndSample.setTaskId(taskId);
                    break;
                }
            }
        }
        String sampleIdsJSON = JSON.toJSON(sampleIds).toString();
        String userName = equalSampleParam.getUpdateBy();
        equalAndSample.setCreateBy(userName);
        equalAndSample.setCreated(LocalDateTime.now());
        equalAndSample.setSampleId(sampleIdsJSON);
        equalAndSample.setEqualSampleKey(equalSampleKey);
        try {
            int insert = equalSampleMapper.insertin(equalAndSample);
            if (insert == 0) {
                return Result.fail("创建平行样失败");
            }
            EqualSampleDto equalSampleDto = new EqualSampleDto();
            equalSampleDto.setEqualSampleKey(equalSampleKey);
            equalSampleDto.setEqualSampleIds(sampleIds);
            Map<String, List<String>> map = new HashMap<>();
            map.put(equalSampleKey, sampleIds);
            equalSamplePool.add(map);
            return Result.ok(equalSamplePool);
        } catch (Exception e) {
            return Result.fail("平行样名称存在");
        }
    }
}
