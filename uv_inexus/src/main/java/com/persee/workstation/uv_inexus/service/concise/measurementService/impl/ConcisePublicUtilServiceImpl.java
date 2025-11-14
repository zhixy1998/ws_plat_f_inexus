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

package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTypeEnum;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureSample;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRoundingParamVO;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRoundingVO;
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
import java.util.stream.Collectors;

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
            //请创建任务
            return R.fail(YamlMessageSource.getMessageStatic("00100016"));
        }

        if (CollectionUtils.isEmpty(usefulLocation)) {
            log.info("没有可用的位置");
            //没有可用的位置
            return R.fail(YamlMessageSource.getMessageStatic("00100017"));
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null || !StringUtils.hasText(task.getSampleKey())) {
            log.info("没有样品,不需要处理");
            //没有样品,不需要处理
            return R.fail(YamlMessageSource.getMessageStatic("00100018"));
        }
        List<FTaskSpecimen> taskSpecimen;
        try {
            taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        } catch (Exception e) {
            log.error("样品信息解析失败，任务ID: {}", taskId, e);
            //样品信息解析失败
            return R.fail(YamlMessageSource.getMessageStatic("00100019"));
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


    @Override
    public R<Boolean> checkApproximateRange(FRoundingVO vo) {
        List<FRoundingParamVO> params = vo.getRoundingParams();

        int size =  params.size();
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        // 1. 检查每个区间内部的合法性
        for (FRoundingParamVO param : params) {
            Double lowerLimit = param.getLowerLimit();
            Double upperLimit = param.getUpperLimit();

            if (lowerLimit == null) {
                min = Double.MIN_VALUE;
            }
            if (upperLimit == null) {
                max = Double.MAX_VALUE;
            }

            if (lowerLimit == null && upperLimit == null && size != 1) {
                //区间重叠，请检查！
                return R.fail(YamlMessageSource.getMessageStatic("00100023"));
            }
            // 只有当下限和上限都不为空时才进行检查
            if (lowerLimit != null && upperLimit != null) {
                // 下限值不能大于上限值
                if (lowerLimit > upperLimit) {
                    return R.fail(YamlMessageSource.getMessageStatic("00100020"));
                }
                // 如果下限值等于上限值且为有限值，必须同时包含两端点
                if (lowerLimit.equals(upperLimit)) {
                    if (!param.getIsLowerLimitIncluded() || !param.getIsUpperLimitIncluded()) {
                        return R.fail(YamlMessageSource.getMessageStatic("00100021"));
                    }
                }
            }
            if (lowerLimit != null && lowerLimit < min) {
                min = lowerLimit;
            }
            if (upperLimit != null && upperLimit > max) {
                max = upperLimit;
            }
        }

        if (min != Double.MIN_VALUE && max != Double.MAX_VALUE) {
            //修约范围未覆盖所有区间！
            return R.fail(YamlMessageSource.getMessageStatic("00100024"));
        }

        // 2. 检查区间之间是否重叠
        // 过滤掉无效区间（上下限都为null）
        List<FRoundingParamVO> validParams = params.stream()
                .filter(p -> p.getLowerLimit() != null || p.getUpperLimit() != null)
                .collect(Collectors.toList());

        // 按下限值排序，null值（负无穷）排在最前
        List<FRoundingParamVO> sortedParams = validParams.stream()
                .sorted((p1, p2) -> {
                    Double lower1 = p1.getLowerLimit();
                    Double lower2 = p2.getLowerLimit();

                    // null值处理（视为负无穷）
                    if (lower1 == null) return -1;
                    if (lower2 == null) return 1;

                    return lower1.compareTo(lower2);
                })
                .collect(Collectors.toList());

        // 检查相邻区间是否有重叠
        for (int i = 0; i < sortedParams.size() - 1; i++) {
            if (isOverlapping(sortedParams.get(i), sortedParams.get(i + 1))) {
                //区间重叠，请检查！
                return R.fail(YamlMessageSource.getMessageStatic("00100023"));
            }
        }

        return R.ok(true);
    }

    /**
     * 判断两个区间是否重叠
     *
     * @param interval1 第一个区间
     * @param interval2 第二个区间
     * @return 如果重叠返回true，否则返回false
     */
    private boolean isOverlapping(FRoundingParamVO interval1, FRoundingParamVO interval2) {

        Double upper1 = interval1.getUpperLimit();
        boolean includeUpper1 = Boolean.TRUE.equals(interval1.getIsUpperLimitIncluded());

        Double lower2 = interval2.getLowerLimit();
        boolean includeLower2 = Boolean.TRUE.equals(interval2.getIsLowerLimitIncluded());

        // 处理null边界值（null表示无穷）
        if (upper1 == null || lower2 == null) {
            return true;
        }

        if (upper1 > lower2) {
            return true;
        } else if (upper1.equals(lower2)) {
            return includeUpper1 && includeLower2;
        }

        return false;
    }

}
