/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.equalSampleService.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.guide.equalSampleMapper.EqualSampleMapper;
import com.persee.workstation.uv_inexus.dto.concise.common.EqualAndSample;
import com.persee.workstation.uv_inexus.dto.concise.common.EqualSampleDto;
import com.persee.workstation.uv_inexus.dto.concise.common.EqualSampleParam;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
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
                //创建平行样失败
                return Result.fail("00801001");
            }
            EqualSampleDto equalSampleDto = new EqualSampleDto();
            equalSampleDto.setEqualSampleKey(equalSampleKey);
            equalSampleDto.setEqualSampleIds(sampleIds);
            Map<String, List<String>> map = new HashMap<>();
            map.put(equalSampleKey, sampleIds);
            equalSamplePool.add(map);
            return Result.ok(equalSamplePool);
        } catch (Exception e) {
            //平行样名称存在
            return Result.fail("00801002");
        }
    }
}
