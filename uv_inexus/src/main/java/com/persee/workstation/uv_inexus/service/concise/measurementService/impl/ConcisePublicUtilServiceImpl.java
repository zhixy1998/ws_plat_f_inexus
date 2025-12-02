/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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

        if (params == null || params.isEmpty()) {
            return R.fail(YamlMessageSource.getMessageStatic("00100023"));
        }

        // 1. 检查每个区间内部的合法性
        for (FRoundingParamVO param : params) {
            Double lowerLimit = param.getLowerLimit();
            Double upperLimit = param.getUpperLimit();

            // 检查无穷边界的包含性设置
            if (lowerLimit == null && Boolean.TRUE.equals(param.getIsLowerLimitIncluded())) {
                return R.fail(YamlMessageSource.getMessageStatic("00100026")); // 下限为负无穷时不能包含端点
            }
            if (upperLimit == null && Boolean.TRUE.equals(param.getIsUpperLimitIncluded())) {
                return R.fail(YamlMessageSource.getMessageStatic("00100026")); // 上限为正无穷时不能包含端点
            }

            if (lowerLimit == null && upperLimit == null && params.size() != 1) {
                return R.fail(YamlMessageSource.getMessageStatic("00100023"));
            }

            if (lowerLimit != null && upperLimit != null) {
                if (lowerLimit > upperLimit) {
                    return R.fail(YamlMessageSource.getMessageStatic("00100020"));
                }
                if (lowerLimit.equals(upperLimit)) {
                    if (!param.getIsLowerLimitIncluded() || !param.getIsUpperLimitIncluded()) {
                        return R.fail(YamlMessageSource.getMessageStatic("00100021"));
                    }
                }
            }
        }

        // 2. 综合检查区间重叠和覆盖
        return validateIntervals(params);
    }

    private R<Boolean> validateIntervals(List<FRoundingParamVO> params) {
        List<FRoundingParamVO> validParams = params.stream()
                .filter(p -> p.getLowerLimit() != null || p.getUpperLimit() != null)
                .sorted((p1, p2) -> {
                    if (p1.getLowerLimit() == null) return -1;
                    if (p2.getLowerLimit() == null) return 1;
                    return p1.getLowerLimit().compareTo(p2.getLowerLimit());
                })
                .collect(Collectors.toList());

        if (validParams.isEmpty()) {
            return params.size() == 1 &&
                    params.get(0).getLowerLimit() == null &&
                    params.get(0).getUpperLimit() == null ?
                    R.ok(true) : R.fail(YamlMessageSource.getMessageStatic("00100024"));
        }

        // 检查是否完整覆盖
        if (validParams.get(0).getLowerLimit() != null ||
                validParams.get(validParams.size() - 1).getUpperLimit() != null) {
            return R.fail(YamlMessageSource.getMessageStatic("00100024"));
        }

        // 检查重叠和连续性
        for (int i = 0; i < validParams.size() - 1; i++) {
            FRoundingParamVO current = validParams.get(i);
            FRoundingParamVO next = validParams.get(i + 1);

            Double upper1 = current.getUpperLimit();
            Double lower2 = next.getLowerLimit();
            boolean includeUpper1 = Boolean.TRUE.equals(current.getIsUpperLimitIncluded());
            boolean includeLower2 = Boolean.TRUE.equals(next.getIsLowerLimitIncluded());

            if (upper1 != null && lower2 != null) {
                int compare = upper1.compareTo(lower2);
                if (compare > 0) {
                    return R.fail(YamlMessageSource.getMessageStatic("00100023")); // 重叠
                } else if (compare < 0) {
                    return R.fail(YamlMessageSource.getMessageStatic("00100024")); // 不连续(未覆盖)
                } else {
                    // 边界相等时，必须恰好一个包含端点
                    if (includeUpper1 && includeLower2) {
                        return R.fail(YamlMessageSource.getMessageStatic("00100023")); // 重叠
                    } else if (!includeUpper1 && !includeLower2) {
                        return R.fail(YamlMessageSource.getMessageStatic("00100024")); // 不连续(未覆盖)
                    }
                    // 恰好一个包含端点是正确的
                }
            }
        }

        return R.ok(true);
    }
}
