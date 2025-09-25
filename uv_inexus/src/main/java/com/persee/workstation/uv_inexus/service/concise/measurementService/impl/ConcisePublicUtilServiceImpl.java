package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTypeEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureSample;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskSpecimen;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ConcisePublicUtilServiceImpl implements ConcisePublicUtilService {
    @Autowired
    FMeasureProjectService fMeasureProjectService;
    @Autowired
    FMeasureTaskService fMeasureTaskService;
    @Autowired
    FMeasureSampleService fMeasureSampleService;
    @Autowired
    SysUserMapper userManageDAO;
    @Autowired
    FMeasureTaskMapper fMeasureTaskMapper;
    @Autowired
    FMeasureMethodService fMeasureMethodService;
    @Autowired
    FMeasureDataService fMeasureDataService;

    @Autowired
    FMeasureAnalysisDataService fMeasureAnalysisDataService;

    @Autowired
    FMeasureTaskAttachmentService fMeasureTaskAttachmentService;

    @Autowired
    DeviceInfoService deviceInfoService;

    @Autowired
    DevMessageUtil devMessageUtil;

    @Value("1")
    String tested;

    /**
     * 修改样本位置
     * @param taskId
     * @param usefulLocation
     * @return
     */
    @Override
    public R<Boolean> changeSampleLocation(String taskId, List<Integer> usefulLocation) {
        if (!StringUtils.hasText(taskId)) {
            return R.fail("请创建任务");
        }

        if (CollectionUtils.isEmpty(usefulLocation)) {
            log.info("没有可用的位置");
            return R.fail("没有可用的位置");
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null || !StringUtils.hasText(task.getSampleKey())) {
            log.info("没有样品,不需要处理");
            return R.fail("没有样品,不需要处理");
        }
        List<FTaskSpecimen> taskSpecimen;
        try {
            taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        } catch (Exception e) {
            log.error("样品信息解析失败，任务ID: {}", taskId, e);
            return R.fail("样品信息解析失败");
        }
        if (task.getMeasureType().equals(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription())) {

            List<Integer> stand = new ArrayList<>(usefulLocation);

            for (FTaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(tested) || specimen.getIsTested().equals(tested)) {
                    log.info("删除或测量过的样品跳过");
                    continue;
                }

                FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());
                if (sample == null) {
                    log.info("没有找到样品");
                    continue;
                }

                if (sample.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                    Integer currentLocate = usefulLocation.get(0);
                    sample.setSampleLocation(currentLocate.toString());
                    usefulLocation.remove(0);
                    usefulLocation.add(currentLocate);
                    fMeasureSampleService.updateById(sample);
                } else if (sample.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                    Integer currentLocate = stand.get(0);
                    sample.setSampleLocation(currentLocate.toString());
                    stand.remove(0);
                    stand.add(currentLocate);
                    fMeasureSampleService.updateById(sample);
                }
            }

        } else {

            for (FTaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(tested) || specimen.getIsTested().equals(tested)) {
                    log.info("删除或测量过的样品跳过");
                    continue;
                }
                FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());
                if (sample == null) {
                    log.info("没有找到样品");
                    continue;
                }
                Integer currentLocate = usefulLocation.get(0);
                sample.setSampleLocation(currentLocate.toString());
                usefulLocation.remove(0);
                usefulLocation.add(currentLocate);
                fMeasureSampleService.updateById(sample);
            }
        }

        return R.ok(true);
    }

}
