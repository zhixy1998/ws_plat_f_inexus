/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTypeEnum;
import com.persee.workstation.uv_inexus.i18n.I18nMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskSpecimen;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureSample;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RoundingParamVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RoundingVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GuidePublicUtilServiceImpl implements GuidePublicUtilService {
    @Autowired
    TMeasureProjectService tMeasureProjectService;
    @Autowired
    TMeasureTaskService tMeasureTaskService;
    @Autowired
    TMeasureSampleService tMeasureSampleService;
    @Autowired
    SystemUserManageDAO userManageDAO;
    @Autowired
    TMeasureTaskMapper tMeasureTaskMapper;
    @Autowired
    TMeasureMethodService tMeasureMethodService;
    @Autowired
    TMeasureDataService tMeasureDataService;

    @Autowired
    TMeasureAnalysisDataService tMeasureAnalysisDataService;

    @Autowired
    TMeasureTaskAttachmentService tMeasureTaskAttachmentService;

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
            return R.fail("00100016");
        }

        if (CollectionUtils.isEmpty(usefulLocation)) {
            log.info("没有可用的位置");
            //没有可用的位置
            return R.fail("00100017");
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null || !StringUtils.hasText(task.getSampleKey())) {
            log.info("没有样品,不需要处理");
            //没有样品,不需要处理
            return R.fail("00100018");
        }
        List<FTaskSpecimen> taskSpecimen;
        try {
            taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        } catch (Exception e) {
            log.error("样品信息解析失败，任务ID: {}", taskId, e);
            //样品信息解析失败
            return R.fail("00100019");
        }
        if (task.getMeasureType().equals(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription())) {

            List<Integer> stand = new ArrayList<>(usefulLocation);

            for (FTaskSpecimen specimen : taskSpecimen) {
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

        } else {

            for (FTaskSpecimen specimen : taskSpecimen) {
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

    @Override
    public R<Boolean> checkApproximateRange(RoundingVO vo) {
        List<RoundingParamVO> params = vo.getRoundingParams();

        if (params == null || params.isEmpty()) {
            return R.fail("00100023");
        }

        // 1. 检查每个区间内部的合法性
        for (RoundingParamVO param : params) {
            Double lowerLimit = param.getLowerLimit();
            Double upperLimit = param.getUpperLimit();

            // 检查无穷边界的包含性设置
            if (lowerLimit == null && Boolean.TRUE.equals(param.getIsLowerLimitIncluded())) {
                return R.fail("00100026"); // 下限为负无穷时不能包含端点
            }
            if (upperLimit == null && Boolean.TRUE.equals(param.getIsUpperLimitIncluded())) {
                return R.fail("00100026"); // 上限为正无穷时不能包含端点
            }

            if (lowerLimit == null && upperLimit == null && params.size() != 1) {
                return R.fail("00100023");
            }

            if (lowerLimit != null && upperLimit != null) {
                if (lowerLimit > upperLimit) {
                    return R.fail("00100020");
                }
                if (lowerLimit.equals(upperLimit)) {
                    if (!param.getIsLowerLimitIncluded() || !param.getIsUpperLimitIncluded()) {
                        return R.fail("00100021");
                    }
                }
            }
        }

        // 2. 综合检查区间重叠和覆盖
        return validateIntervals(params);
    }

    private R<Boolean> validateIntervals(List<RoundingParamVO> params) {
        List<RoundingParamVO> validParams = params.stream()
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
                    R.ok(true) : R.fail("00100024");
        }

        // 检查是否完整覆盖
        if (validParams.get(0).getLowerLimit() != null ||
                validParams.get(validParams.size() - 1).getUpperLimit() != null) {
            return R.fail("00100024");
        }

        // 检查重叠和连续性
        for (int i = 0; i < validParams.size() - 1; i++) {
            RoundingParamVO current = validParams.get(i);
            RoundingParamVO next = validParams.get(i + 1);

            Double upper1 = current.getUpperLimit();
            Double lower2 = next.getLowerLimit();
            boolean includeUpper1 = Boolean.TRUE.equals(current.getIsUpperLimitIncluded());
            boolean includeLower2 = Boolean.TRUE.equals(next.getIsLowerLimitIncluded());

            if (upper1 != null && lower2 != null) {
                int compare = upper1.compareTo(lower2);
                if (compare > 0) {
                    return R.fail("00100023"); // 重叠
                } else if (compare < 0) {
                    return R.fail("00100024"); // 不连续(未覆盖)
                } else {
                    // 边界相等时，必须恰好一个包含端点
                    if (includeUpper1 && includeLower2) {
                        return R.fail("00100023"); // 重叠
                    } else if (!includeUpper1 && !includeLower2) {
                        return R.fail("00100024"); // 不连续(未覆盖)
                    }
                    // 恰好一个包含端点是正确的
                }
            }
        }

        return R.ok(true);
    }

    @Override
    public boolean parseRoundingCondition(String condition, RoundingParamVO paramVO, String variableName) {
        if (condition == null || condition.trim().isEmpty()) {
            return false;
        }
        // 如果未指定变量名，默认使用 x
        if (variableName == null || variableName.trim().isEmpty()) {
            variableName = "x";
        }
        
        System.out.println("正在解析修约条件：" + condition + " (变量名: " + variableName + ")");
        
        // 统一处理：去除空格，替换中文符号
        // 注意：这里不再统一转小写 condition.toLowerCase()，因为要支持 variableName 可能包含大写
        // 但为了后续正则匹配（Pattern.CASE_INSENSITIVE），这里的大小写保留原样即可
        String cleanCond = condition.replaceAll("\\s+", "")
                .replace("≤", "<=")
                .replace("≥", ">=")
                .replace("＜", "<")
                .replace("＞", ">")
                .replace("＝", "=");

        // 对变量名进行正则转义，防止包含特殊字符
        String escapedVar = Pattern.quote(variableName);

        // 模式1：双边区间 (例如 1<x<=100)
        // 使用 (?i) 开启忽略大小写模式，或在 compile 时指定 Pattern.CASE_INSENSITIVE
        Pattern rangePattern = Pattern.compile("^(-?[\\d\\.]+)([<]=?|>=?)" + escapedVar + "([<]=?|>=?)(-?[\\d\\.]+)$", Pattern.CASE_INSENSITIVE);
        Matcher rangeMatcher = rangePattern.matcher(cleanCond);

        if (rangeMatcher.find()) {
            try {
                Double leftVal = Double.parseDouble(rangeMatcher.group(1));
                String leftOp = rangeMatcher.group(2);
                String rightOp = rangeMatcher.group(3);
                Double rightVal = Double.parseDouble(rangeMatcher.group(4));

                paramVO.setLowerLimit(leftVal);
                paramVO.setIsLowerLimitIncluded(leftOp.contains("="));
                paramVO.setUpperLimit(rightVal);
                paramVO.setIsUpperLimitIncluded(rightOp.contains("="));
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // 模式2：单边条件 (例如 x<=1 或 x>100)
        Pattern singlePattern = Pattern.compile("^" + escapedVar + "([<]=?|>=?)(-?[\\d\\.]+)$", Pattern.CASE_INSENSITIVE);
        Matcher singleMatcher = singlePattern.matcher(cleanCond);

        if (singleMatcher.find()) {
            try {
                String op = singleMatcher.group(1);
                Double val = Double.parseDouble(singleMatcher.group(2));

                if (op.startsWith("<")) {
                    paramVO.setUpperLimit(val);
                    paramVO.setIsUpperLimitIncluded(op.contains("="));
                } else if (op.startsWith(">")) {
                    paramVO.setLowerLimit(val);
                    paramVO.setIsLowerLimitIncluded(op.contains("="));
                }
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        // 模式3：单边条件倒写 (例如 1>=x)
        Pattern reverseSinglePattern = Pattern.compile("^(-?[\\d\\.]+)([<]=?|>=?)" + escapedVar + "$", Pattern.CASE_INSENSITIVE);
        Matcher reverseSingleMatcher = reverseSinglePattern.matcher(cleanCond);
        
        if (reverseSingleMatcher.find()) {
            try {
                Double val = Double.parseDouble(reverseSingleMatcher.group(1));
                String op = reverseSingleMatcher.group(2);
                
                if (op.startsWith(">")) { 
                    paramVO.setUpperLimit(val);
                    paramVO.setIsUpperLimitIncluded(op.contains("="));
                } 
                else if (op.startsWith("<")) {
                    paramVO.setLowerLimit(val);
                    paramVO.setIsLowerLimitIncluded(op.contains("="));
                }
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // 如果三种模式都不匹配
        return false;
    }
}
