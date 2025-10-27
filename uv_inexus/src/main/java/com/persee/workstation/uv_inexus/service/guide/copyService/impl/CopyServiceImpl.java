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

package com.persee.workstation.uv_inexus.service.guide.copyService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dto.concise.common.CopyParam;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureAnalysisData;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureData;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureSample;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureAnalysisDataService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureDataService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureSampleService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskService;
import com.persee.workstation.uv_inexus.service.guide.copyService.CopyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class CopyServiceImpl implements CopyService {
    @Autowired
    private FMeasureTaskService fMeasureTaskService;
    @Autowired
    private FMeasureDataService fMeasureDataService;
    @Autowired
    private FMeasureAnalysisDataService fMeasureAnalysisDataService;
    @Autowired
    private FMeasureSampleService fMeasureSampleService;


    @Override
    public Result<String> copy(CopyParam copyParam) {
        String taskName = copyParam.getTaskName();
        String taskId = copyParam.getTaskId();
        try {
            /*
             * 复制任务
             */
            FMeasureTask task = fMeasureTaskService.getById(taskId);
                task.setId(null);
                task.setTaskName(taskName);
                fMeasureTaskService.save(task);


            /*
             * 复制样品
             */
            String sampleKey = task.getSampleKey();
            List<TaskSpecimen> taskSpecimen =JSON.parseArray(sampleKey, TaskSpecimen.class);
            taskSpecimen.forEach(sample ->{
                String specimenKey = sample.getSpecimenKey();
                FMeasureSample newSample = fMeasureSampleService.getById(specimenKey);
                newSample.setId(null);
                fMeasureSampleService.save(newSample);
                    }
            );
            /*
             * 复制数据
             */
            LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FMeasureData::getTaskId, taskId);
            List<FMeasureData> fMeasureDataList = fMeasureDataService.list(queryWrapper);
            for (FMeasureData fMeasureData : fMeasureDataList) {
                fMeasureData.setId(null);
                fMeasureDataService.save(fMeasureData);
            }
            LambdaQueryWrapper<FMeasureAnalysisData> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(FMeasureAnalysisData::getTaskId, taskId);
            List<FMeasureAnalysisData> fMeasureAnalysisDataList = fMeasureAnalysisDataService.list(queryWrapper1);
            for (FMeasureAnalysisData fMeasureAnalysisData : fMeasureAnalysisDataList) {
                fMeasureAnalysisData.setId(null);
                fMeasureAnalysisDataService.save(fMeasureAnalysisData);
            }
            return Result.ok("拷贝成功");

        } catch (Exception e) {
            return Result.fail("拷贝成功");
        }

    }
}
