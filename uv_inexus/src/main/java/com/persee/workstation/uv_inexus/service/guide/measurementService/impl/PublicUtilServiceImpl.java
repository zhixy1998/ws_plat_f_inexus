package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTypeEnum;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureSample;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskSpecimen;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureSample;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.guide.measurementService.PublicUtilService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureSampleService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
@Slf4j
@Service
public class PublicUtilServiceImpl implements PublicUtilService {


    @Autowired
    TMeasureTaskService  tMeasureTaskService;

    @Autowired
    TMeasureSampleService tMeasureSampleService;

    @Value("1")
    String tested;

    @Override
    public R<Boolean> changeSampleLocation(String taskId, List<Integer> usefulLocation) {
        if (!StringUtils.hasText(taskId)) {
            //请创建任务
            return R.fail(YamlMessageSource.getMessageStatic("00100016"));
        }

        if (CollectionUtils.isEmpty(usefulLocation)) {
            log.info("没有可用的位置");
            //没有可用的位置
            return R.fail(YamlMessageSource.getMessageStatic("00100017"));
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null || !StringUtils.hasText(task.getSampleKey())) {
            log.info("没有样品,不需要处理");
            //没有样品,不需要处理
            return R.fail(YamlMessageSource.getMessageStatic("00100018"));
        }
        List<TaskSpecimen> taskSpecimen;
        try {
            taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        } catch (Exception e) {
            log.error("样品信息解析失败，任务ID: {}", taskId, e);
            //样品信息解析失败
            return R.fail(YamlMessageSource.getMessageStatic("00100019"));
        }
        if (task.getMeasureType().equals(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription())) {

            List<Integer> stand = new ArrayList<>(usefulLocation);

            for (TaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(tested) || specimen.getIsTested().equals(tested)) {
                    log.info("删除或测量过的样品跳过");
                    continue;
                }

                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                if (sample == null) {
                    log.info("没有找到样品");
                    continue;
                }

                if (sample.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                    Integer currentLocate = usefulLocation.get(0);
                    sample.setSampleLocation(currentLocate.toString());
                    usefulLocation.remove(0);
                    usefulLocation.add(currentLocate);
                    tMeasureSampleService.updateById(sample);
                } else if (sample.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                    Integer currentLocate = stand.get(0);
                    sample.setSampleLocation(currentLocate.toString());
                    stand.remove(0);
                    stand.add(currentLocate);
                    tMeasureSampleService.updateById(sample);
                }
            }

        }else {
            for (TaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(tested) || specimen.getIsTested().equals(tested)) {
                    log.info("删除或测量过的样品跳过");
                    continue;
                }
                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                if (sample == null) {
                    log.info("没有找到样品");
                    continue;
                }
                Integer currentLocate = usefulLocation.get(0);
                sample.setSampleLocation(currentLocate.toString());
                usefulLocation.remove(0);
                usefulLocation.add(currentLocate);
                tMeasureSampleService.updateById(sample);
            }
        }
        return R.ok(true);
    }
}
