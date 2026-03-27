/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskHeadSnapshot;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.parseReport.LaTeXConverter;
import com.persee.workstation.uv_inexus.service.guide.standardService.TReportStandardDetectionService;
import com.persee.workstation.uv_inexus.service.guide.standardService.TReportStandardPretreatmentService;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemBusinessParamService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.DevOperationInputParam;
import com.persee.workstation.uv_inexus.util.deviceUtil.DevOperationUtilService;
import com.persee.workstation.uv_inexus.util.deviceUtil.OperationResultEntity;
import com.persee.workstation.uv_inexus.util.deviceUtil.OperationTypeEnums;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.util.pythonUtil.RestUtil;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.*;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.*;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemBusinessParamVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocument1;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service("photometricService")
public class PhotometricServiceImpl implements PhotometricService {
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
    DevOperationUtilService devOperationUtilService;
    @Autowired
    PublicUtilService publicUtilService;

    @Autowired
    SamplePoolConfigService tSamplePoolConfigService;
    @Autowired
    TMeasureAnalysisDataService tMeasureAnalysisDataService;

    @Autowired
    TReportStandardDetectionService tReportStandardDetectionService;

    @Autowired
    TReportStandardPretreatmentService tReportStandardPretreatmentService;

    @Autowired
    TMeasureTaskAttachmentService tMeasureTaskAttachmentService;

    @Autowired
    DeviceInfoService deviceInfoService;

    @Autowired
    DeviceManageService manageService;

    @Autowired
    DevMessageUtil devMessageUtil;
    @Autowired
    SystemBusinessParamService businessParamService;


    @Autowired
    RestUtil restUtil;

    @Autowired
    LaTeXConverter latexConverter;

    @Value("默认任务")
    String defaultTaskKey;

    @Value("任务")
    String newTaskName;

    @Value("1")
    String deleteType;

    @Value("0")
    String createType;

    @Value("0")
    String noTested;

    @Value("1")
    String tested;

    private R<Boolean> checkParam(PhotometricParameter parameter) {
        if (parameter.getLuminosity() == null || parameter.getLuminosity().trim().isEmpty()) {
            return R.fail(YamlMessageSource.getMessageStatic("00105001"));
        }
        if (parameter.getLuminosity().equals(WaveLengthEnum.es.getCode()) || parameter.getLuminosity().equals(WaveLengthEnum.er.getCode())) {
            String energyLamp = parameter.getEnergyLamp();
            Integer gain = parameter.getGain();
            if (!StringUtils.hasText(energyLamp)) {
                return R.fail(YamlMessageSource.getMessageStatic("00103002", WaveLengthEnum.getName(parameter.getLuminosity())));
            }
            if (gain == null) {
                return R.fail(YamlMessageSource.getMessageStatic("00103003", WaveLengthEnum.getName(parameter.getLuminosity())));
            }
        }
        if (WaveLengthEnum.getName(parameter.getLuminosity()) == null) {
            return R.fail(YamlMessageSource.getMessageStatic("00103004"));
        }
        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            return R.fail(YamlMessageSource.getMessageStatic("00105003"));
        }
        if (parameter.getWaveLength().size() > 26) {
            return R.fail(YamlMessageSource.getMessageStatic("00103005"));
        }
        if (!StringUtils.hasText(parameter.getChangeLampWaveLength())) {
            return R.fail("换灯波长不能为空");
        }
        if (StringUtils.hasText(parameter.getChangeLampWaveLength())) {
            double v = Double.parseDouble(parameter.getChangeLampWaveLength());
            if (v < 330 || v > 390) {
                return R.fail(YamlMessageSource.getMessageStatic("0010202A", v));
            }
            if (parameter.getWaveLength().contains(parameter.getChangeLampWaveLength())) {
                return R.fail(YamlMessageSource.getMessageStatic("0010202B"));
            }
        }
        if (parameter.getRepetitionPattern() == null || parameter.getRepetitionPattern().trim().isEmpty()) {
            return R.fail(YamlMessageSource.getMessageStatic("00103007"));
        }
        if (!java.util.Arrays.asList("单池重复", "不重复").contains(parameter.getRepetitionPattern())) {
            return R.fail(YamlMessageSource.getMessageStatic("00103008"));
        }
        if ("单池重复".equals(parameter.getRepetitionPattern())) {
            if (parameter.getRepetitionTime() == null || parameter.getRepetitionTime().trim().isEmpty()) {
                return R.fail(YamlMessageSource.getMessageStatic("00103009"));
            }
            try {
                int repetitionTime = Integer.parseInt(parameter.getRepetitionTime());
                if (repetitionTime < 2 || repetitionTime > 10) {
                    return R.fail(YamlMessageSource.getMessageStatic("0010300A"));
                }
            } catch (NumberFormatException e) {
                return R.fail(YamlMessageSource.getMessageStatic("0010300B"));
            }
        }
        if ("单池重复".equals(parameter.getRepetitionPattern())) {
            if (parameter.getTimeInterval() == null || parameter.getTimeInterval().trim().isEmpty()) {
                return R.fail(YamlMessageSource.getMessageStatic("0010300C"));
            }
            try {
                double timeInterval = Double.parseDouble(parameter.getTimeInterval());
                if (timeInterval < 0 || timeInterval > 60) {
                    return R.fail(YamlMessageSource.getMessageStatic("0010300D"));
                }
                String[] parts = parameter.getTimeInterval().split("\\.");
                if (parts.length > 1 && parts[1].length() > 1) {
                    return R.fail(YamlMessageSource.getMessageStatic("0010500F"));
                }
            } catch (NumberFormatException e) {
                return R.fail(YamlMessageSource.getMessageStatic("00105010"));
            }
        }
        if (parameter.getSampleInputParams() != null && parameter.getSampleInputParams().size() > 40) {
            return R.fail(YamlMessageSource.getMessageStatic("0010300E"));
        }
        if (parameter.getResultParams() != null && parameter.getResultParams().size() > 5) {
            return R.fail(YamlMessageSource.getMessageStatic("0010300F"));
        }
        java.util.List<RoundingVO> approxParams = parameter.getApproxParams();
        if (approxParams != null && !approxParams.isEmpty()) {
            if (approxParams.size() > 10) {
                return R.fail(YamlMessageSource.getMessageStatic("00103010", approxParams.size()));
            }
            for (RoundingVO approxParam : approxParams) {
                if (approxParam.getRoundingParams() == null || approxParam.getRoundingParams().isEmpty()) {
                    return R.fail(YamlMessageSource.getMessageStatic("00103011", approxParam.getResultIdentity()));
                }
                if (approxParam.getRoundingParams().size() > 10) {
                    return R.fail(YamlMessageSource.getMessageStatic("00103012", approxParam.getResultIdentity()));
                }
                R<Boolean> r = checkApproximateRange(approxParam);
                if (!r.getSuccess()) {
                    return r;
                }
            }
        }
        if (parameter.getQcParams() != null && parameter.getQcParams().size() > 10) {
            return R.fail(YamlMessageSource.getMessageStatic("00103013"));
        }

        if (parameter.getResultParams() != null && !parameter.getResultParams().isEmpty()) {
            java.util.List<InputParam> inputParams = parameter.getSampleInputParams();
            java.util.List<PreCountResultParam> resultParams = parameter.getResultParams();
            for (PreCountResultParam resultParam : resultParams) {
                if (resultParam == null) {
                    continue;
                }
                Result<Boolean> checkResult = checkFormula(inputParams, resultParam);
                if (checkResult.getCode() != 200 || !java.lang.Boolean.TRUE.equals(checkResult.getData())) {
                    return R.fail(checkResult.getMessage());
                }
            }
        }
        return R.ok(true);
    }

    private R<Boolean> checkApproximateRange(RoundingVO vo) {
        java.util.List<RoundingParamVO> params = vo.getRoundingParams();
        if (params == null || params.isEmpty()) {
            return R.fail(YamlMessageSource.getMessageStatic("00100023"));
        }
        for (RoundingParamVO param : params) {
            Double lowerLimit = param.getLowerLimit();
            Double upperLimit = param.getUpperLimit();
            if (lowerLimit == null && Boolean.TRUE.equals(param.getIsLowerLimitIncluded())) {
                return R.fail(YamlMessageSource.getMessageStatic("00100026"));
            }
            if (upperLimit == null && Boolean.TRUE.equals(param.getIsUpperLimitIncluded())) {
                return R.fail(YamlMessageSource.getMessageStatic("00100026"));
            }
            if (lowerLimit == null && upperLimit == null && params.size() != 1) {
                return R.fail(YamlMessageSource.getMessageStatic("00100023"));
            }
            if (lowerLimit != null && upperLimit != null) {
                if (lowerLimit > upperLimit) {
                    return R.fail(YamlMessageSource.getMessageStatic("00100020"));
                }
                if (lowerLimit.equals(upperLimit)) {
                    if (Boolean.TRUE.equals(param.getIsLowerLimitIncluded()) && Boolean.TRUE.equals(param.getIsUpperLimitIncluded())) {
                        return R.ok(true);
                    } else {
                        return R.fail(YamlMessageSource.getMessageStatic("00100021"));
                    }
                }
            }
        }
        java.util.List<RoundingParamVO> validParams = params.stream()
                .filter(p -> p.getLowerLimit() != null || p.getUpperLimit() != null)
                .sorted((p1, p2) -> {
                    if (p1.getLowerLimit() == null) {
                        return -1;
                    }
                    if (p2.getLowerLimit() == null) {
                        return 1;
                    }
                    return p1.getLowerLimit().compareTo(p2.getLowerLimit());
                })
                .collect(java.util.stream.Collectors.toList());
        if (validParams.isEmpty()) {
            if (params.size() == 1 && params.get(0).getLowerLimit() == null && params.get(0).getUpperLimit() == null) {
                return R.ok(true);
            } else {
                return R.fail(YamlMessageSource.getMessageStatic("00100024"));
            }
        }
        if (validParams.get(0).getLowerLimit() != null ||
                validParams.get(validParams.size() - 1).getUpperLimit() != null) {
            return R.fail(YamlMessageSource.getMessageStatic("00100024"));
        }
        for (int i = 0; i < validParams.size() - 1; i++) {
            RoundingParamVO current = validParams.get(i);
            RoundingParamVO next = validParams.get(i + 1);
            Double upper1 = current.getUpperLimit();
            Double lower2 = next.getLowerLimit();
            if (upper1 == null || lower2 == null) {
                return R.fail(YamlMessageSource.getMessageStatic("00100025"));
            }
            if (upper1 > lower2) {
                return R.fail(YamlMessageSource.getMessageStatic("00100022"));
            }
            if (upper1.equals(lower2)) {
                boolean includeUpper = Boolean.TRUE.equals(current.getIsUpperLimitIncluded());
                boolean includeLower = Boolean.TRUE.equals(next.getIsLowerLimitIncluded());
                if (includeUpper == includeLower) {
                    return R.fail(YamlMessageSource.getMessageStatic("00100022"));
                }
            }
        }
        return R.ok(true);
    }


    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, PhotometricParameter photometricParameter) {
        R<Boolean> r = checkParam(photometricParameter);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        }
        if (photometricParameter.getWaveLength() == null || photometricParameter.getWaveLength().isEmpty()) {
            //请填写波长值
            return Result.fail(YamlMessageSource.getMessageStatic("0000000E"));
        }

        // 创建测量方法表
        TMeasureMethod tMeasureMethod = new TMeasureMethod();
        tMeasureMethod.setProjectItemKey(methodDto.getProjectId());
        tMeasureMethod.setMethodName(methodDto.getMethodName());
        tMeasureMethod.setMeasurementType(methodDto.getMethodType());
        tMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(photometricParameter));
        tMeasureMethod.setMethodCreator(updateBy);
        tMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        tMeasureMethod.setIsDelete(Constants.PARAM_ZERO);
        boolean save = tMeasureMethodService.save(tMeasureMethod);
        if (save) {
            //创建成功
            return Result.ok(YamlMessageSource.getMessageStatic("0000000F"));
        } else {
            //创建失败
            return Result.fail(YamlMessageSource.getMessageStatic("00000010"));
        }
    }

    @Override
    public Result<String> updateMethodParameter(PhotometricMethodParameterDto methodDto) {
        // 根据方法id查询当前方法
        if (!StringUtils.hasText(methodDto.getCurrentMethodId())) {
            //缺少方法
            return Result.fail(YamlMessageSource.getMessageStatic("00302001"));
        }
        if(methodDto.getMethodName().length() >= 256){
            //方法名格式错误
            return Result.fail(YamlMessageSource.getMessageStatic("00300002"));
        }
        // 根据方法id查询方法
        TMeasureMethod measureMethod = tMeasureMethodService.getById(methodDto.getCurrentMethodId());
        if (measureMethod==null){
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        // 将前端传过来的值复制到SpectrumParameter类中
        PhotometricParameter parameter = new PhotometricParameter();
        BeanUtils.copyProperties(methodDto, parameter);
        measureMethod.setMeasurementParamInfo(JSON.toJSONString(parameter));
        measureMethod.setMethodName(methodDto.getMethodName());
        boolean update = tMeasureMethodService.updateById(measureMethod);
        if (!update){
            //更新失败
            return Result.fail(YamlMessageSource.getMessageStatic("00000048"));
        }
        return Result.ok(measureMethod.getId());
    }

    /**
     * 设置预测量参数接口
     *
     * @param methodId 方法ID
     * @param deviceSerialNumber 设备序列号
     * @return 操作结果
     */
    public Result setUpParametersPort(String methodId, String deviceSerialNumber) {
        // 参数校验
        if (methodId == null || methodId.isEmpty()) {
            return Result.fail("方法ID不能为空");
        }
        if (deviceSerialNumber == null || deviceSerialNumber.isEmpty()) {
            return Result.fail("设备序列号不能为空");
        }

        // 根据methodId去方法表中查询数据
        TMeasureMethod tMeasureMethod = tMeasureMethodService.getById(methodId);

        if (tMeasureMethod == null) {
            // 没有找到该方法
            return Result.fail("未找到对应的测量方法");
        }

        // 解析参数
        PhotometricParameter parameter = JSON.parseObject(tMeasureMethod.getMeasurementParamInfo(), PhotometricParameter.class);
        if (parameter == null) {
            return Result.fail("参数解析失败");
        }

        // 构建操作输入参数
        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());

        // 设置具体参数
        SetParam param = new SetParam();

        // 1. 设置光度模式（使用吸收模式）
        param.setPhotoMode(Integer.valueOf(parameter.getLuminosity()));

        // 2. 设置换灯波长
        param.setChangeLampWl(Double.valueOf(parameter.getChangeLampWaveLength()));

        // 3. 设置狭缝
        if (StringUtils.hasText(parameter.getSpectralBandwidth()) && deviceInfoService.isNeedSetSit(deviceSerialNumber)) {
            param.setSlit1(Double.valueOf(parameter.getSpectralBandwidth()));
        }

        // 4. 设置响应时间
        param.setResponseTime(Double.valueOf(parameter.getResponseTime()));

        // 9. 如果是能量模式（Es/Er），设置能量灯和增益
        if (parameter.getLuminosity().equals(WaveLengthEnum.er.getCode())
                || parameter.getLuminosity().equals(WaveLengthEnum.es.getCode())) {

            if (StringUtils.hasText(parameter.getEnergyLamp())) {
                param.setLampPos(Integer.valueOf(parameter.getEnergyLamp()));
            }

            if (parameter.getGain() != null) {
                param.setGain(parameter.getGain());
            }
        }

        inputParam.setParam(param);

        // 执行操作
        R<OperationResultEntity> operation = devOperationUtilService.operation(inputParam);

        if (!operation.getSuccess()) {
            return Result.fail(operation.getMessage());
        }

        return Result.ok("设置参数成功");
    }

    @Override
    public Result<MethodChangeCheckVO> checkMethodParam(PhotometricMethodParameterDto checkDTO) {

        if (!StringUtils.hasText(checkDTO.getCurrentMethodId())) {
            //请选择当前方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000023"));
        }

        PhotometricParameter newMethodParam = new PhotometricParameter();
        BeanUtils.copyProperties(checkDTO, newMethodParam);

        R<Boolean> r = checkParam(newMethodParam);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        }

        double minWaveLength = 190.0;
        double maxWaveLength = 1100.0;

        if (StringUtils.hasText(checkDTO.getDeviceSerialNumber())) {
            R<DeviceMeasureVO> typeInfo = deviceInfoService.queryDeviceTypeInfo(checkDTO.getDeviceSerialNumber());
            if (typeInfo.getSuccess()) {
                DeviceMeasureVO vo = typeInfo.getData();
                if (vo != null) {
                    minWaveLength = StringUtils.hasText(vo.getMinWavelength()) ? Double.parseDouble(vo.getMinWavelength()) : minWaveLength;
                    maxWaveLength = StringUtils.hasText(vo.getMaxWavelength()) ? Double.parseDouble(vo.getMaxWavelength()) : maxWaveLength;
                }
            }
        }
        List<String> waveLengths = newMethodParam.getWaveLength();

        for (int i = 0; i < waveLengths.size(); i++) {
            String waveStr = waveLengths.get(i);
            if (isBlank(waveStr)) {
                //"第" + (i + 1) + "个波长值不能为空字符串"
                return Result.fail(YamlMessageSource.getMessageStatic("00102005", (i + 1)));
            }
            try {
                double waveVal = Double.parseDouble(waveStr);
                if (waveVal < minWaveLength || waveVal > maxWaveLength) {
                    //"第" + (i + 1) + "个波长值超出范围（190-900），当前值：" + waveVal
                    return Result.fail(YamlMessageSource.getMessageStatic("00102006", (i + 1), waveVal, minWaveLength, maxWaveLength));
                }
                // 校验小数点后最多1位
                String[] parts = waveStr.split("\\.");
                if (parts.length > 1 && parts[1].length() > 1) {
                    //"波长值" + waveStr + "小数点后最多保留1位"
                    return Result.fail(YamlMessageSource.getMessageStatic("00105005", waveStr));
                }
            } catch (NumberFormatException e) {
                return Result.fail(YamlMessageSource.getMessageStatic("00102007", (i + 1), waveStr));
            }
        }

        MethodChangeCheckVO vo = new MethodChangeCheckVO();
        vo.setCurrentMethodId(checkDTO.getCurrentMethodId());
        vo.setParamChange(false);
        vo.setClearDataRange(RationChangeMethodTypeEnums.noChange.getIdentity());
        vo.setMessage(RationChangeMethodTypeEnums.noChange.getMessage());

        TMeasureMethod currentMethod = tMeasureMethodService.getById(checkDTO.getCurrentMethodId());

        boolean isTaskEnd = false;

        if (StringUtils.hasText(checkDTO.getTaskId())) {

            TMeasureTask task = tMeasureTaskService.getById(checkDTO.getTaskId());
            isTaskEnd = task != null && task.getTaskStatus() != null && task.getTaskStatus().equals(TaskStatusEnum.COMPLETED.getCode());
        }

        PhotometricParameter oldParam = JSON.parseObject(currentMethod.getMeasurementParamInfo(), PhotometricParameter.class);

        String dbJson = JSON.toJSONString(oldParam, SerializerFeature.SortField);
        String frontJson = JSON.toJSONString(newMethodParam, SerializerFeature.SortField);

        if (dbJson.equals(frontJson)) {
            vo.setParamChange(false);
            vo.setClearDataRange(RationChangeMethodTypeEnums.noChange.getIdentity());
            vo.setMessage(RationChangeMethodTypeEnums.noChange.getMessage());
            return Result.ok(vo);
        }
        //检查测量数据
        boolean b = checkMeasureParam(oldParam, newMethodParam);
        System.out.println(b);
        if (!b) {
            log.info("测量参数有改变");
            vo.setClearDataRange(RationChangeMethodTypeEnums.measure.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.measure.getMessage());
            return Result.ok(vo);
        }

        //检查分析参数
        boolean analyze = checkAnalyzeParam(oldParam, newMethodParam);
        if (!analyze && !isTaskEnd) {
            log.info("分析参数有改变");
            vo.setClearDataRange(RationChangeMethodTypeEnums.analyze.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.analyze.getMessage());
            return Result.ok(vo);
        }

        if (isTaskEnd) {
            log.info("当前任务已结束");
            vo.setClearDataRange(RationChangeMethodTypeEnums.endTask.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.endTask.getMessage());
        }

        return Result.ok(vo);
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }


    private boolean checkMeasureParam(PhotometricParameter oldParam, PhotometricParameter newParam) {
        java.util.List<String> oldWave = oldParam.getWaveLength();
        java.util.List<String> newWave = newParam.getWaveLength();
        if (!Objects.equals(oldWave, newWave)) {
            return false;
        }
        if (!Objects.equals(oldParam.getLuminosity(), newParam.getLuminosity())) {
            return false;
        }
        if (!Objects.equals(oldParam.getEnergyLamp(), newParam.getEnergyLamp())) {
            return false;
        }
        if (!Objects.equals(oldParam.getGain(), newParam.getGain())) {
            return false;
        }
        return Objects.equals(oldParam.getSpectralBandwidth(), newParam.getSpectralBandwidth()) &&
                Objects.equals(oldParam.getResponseTime(), newParam.getResponseTime()) &&
                Objects.equals(oldParam.getChangeLampWaveLength(), newParam.getChangeLampWaveLength()) &&
                Objects.equals(oldParam.getRepetitionPattern(), newParam.getRepetitionPattern()) &&
                Objects.equals(oldParam.getRepetitionTime(), newParam.getRepetitionTime()) &&
                Objects.equals(oldParam.getTimeInterval(), newParam.getTimeInterval());
    }

    private boolean checkAnalyzeParam(PhotometricParameter oldParam, PhotometricParameter newParam) {
        return Objects.equals(oldParam.getSampleInputParams(), newParam.getSampleInputParams()) &&
                Objects.equals(oldParam.getQcParams(), newParam.getQcParams()) &&
                Objects.equals(oldParam.getResultParams(), newParam.getResultParams()) &&
                Objects.equals(oldParam.getPreProcessParameters(), newParam.getPreProcessParameters()) &&
                Objects.equals(oldParam.getApproxParams(), newParam.getApproxParams());
    }

    private Result<Boolean> checkFormula(java.util.List<InputParam> inputParamList, PreCountResultParam newFormula) {
        if (newFormula == null || !StringUtils.hasText(newFormula.getFormulaText())) {
            return Result.fail(YamlMessageSource.getMessageStatic("0010100C"));
        }
        if (!StringUtils.hasText(newFormula.getResultName())) {
            return Result.fail(YamlMessageSource.getMessageStatic("00101017"));
        }
        if (!StringUtils.hasText(newFormula.getResultIdentity())) {
            return Result.fail(YamlMessageSource.getMessageStatic("00101018"));
        }
        if (!StringUtils.hasText(newFormula.getUnit())) {
            return Result.fail(YamlMessageSource.getMessageStatic("00101019"));
        }
        if (inputParamList == null || inputParamList.isEmpty()) {
            return Result.fail(YamlMessageSource.getMessageStatic("0010100D"));
        }
        java.util.List<String> paramList = new java.util.ArrayList<>();
        for (InputParam param : inputParamList) {
            paramList.add(param.getResultIdentity());
        }
        java.util.List<String> formulaList = new java.util.ArrayList<>();
        formulaList.add(newFormula.getFormulaText());
        R<String> verified = latexConverter.verifyFormulasAgainstParamList(paramList, formulaList);
        if (!verified.getSuccess()) {
            return Result.fail(verified.getMessage());
        }
        return Result.ok(true);
    }

    @Override
    public Result<MeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType) {

        MeasurementCurrentInfoVO vo = new MeasurementCurrentInfoVO();

        SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));

        if (po == null) {
            //用户不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00000001"));
        }
        MeasureTypeEnum measureTypeEnum = MeasureTypeEnum.fromDescription(measureType);
        if (measureTypeEnum == null) {
            //暂不支持
            return Result.fail(YamlMessageSource.getMessageStatic("0000002B"));
        }

        String id = po.getId();

        LambdaQueryWrapper<TMeasureTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TMeasureTask::getUserId, id)
                .eq(TMeasureTask::getMeasureType, measureType)
                .eq(TMeasureTask::getIsDelete, createType)
                .orderByDesc(TMeasureTask::getCreateTime).last("limit 1");

        TMeasureTask task = tMeasureTaskService.getOne(wrapper);
        if (task != null) {

            vo.setTaskId(task.getId());
            vo.setTaskName(task.getTaskName());
            vo.setTaskStatus(task.getTaskStatus());
            String projectItemKey = task.getProjectItemKey();
            String methodKey = task.getMethodKey();
            TMeasureMethod method = tMeasureMethodService.getById(methodKey);
            if (method != null) {
                vo.setMethodId(method.getId());
                vo.setMethodName(method.getMethodName());
            }

            TMeasureProject project = tMeasureProjectService.getById(projectItemKey);
            if (project != null) {
                vo.setProjectId(project.getId());
                vo.setProjectName(project.getProjectName());
            }
            return Result.ok(vo);
        }

        LambdaQueryWrapper<TMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureMethod::getMethodCreator, updateBy)
                .eq(TMeasureMethod::getMeasurementType, measureType)
                .eq(TMeasureMethod::getIsDelete, createType)
                .orderByDesc(TMeasureMethod::getMethodCreateTime).last("limit 1");
        TMeasureMethod method = tMeasureMethodService.getOne(queryWrapper);

        if (method != null) {
            vo.setMethodId(method.getId());
            vo.setMethodName(method.getMethodName());

            wrapper.clear();
            wrapper.eq(TMeasureTask::getMethodKey, method.getId())
                    .eq(TMeasureTask::getIsDelete, createType)
                    .orderByDesc(TMeasureTask::getCreateTime).last("limit 1");
            task = tMeasureTaskService.getOne(wrapper);
            if (task != null) {
                vo.setTaskId(task.getId());
                vo.setTaskName(task.getTaskName());
                vo.setTaskStatus(task.getTaskStatus());
            }

            TMeasureProject project = tMeasureProjectService.getById(method.getProjectItemKey());
            if (project != null) {
                vo.setProjectId(project.getId());
                vo.setProjectName(project.getProjectName());
            }
            return Result.ok(vo);
        }
        wrapper.clear();
        wrapper.eq(TMeasureTask::getTaskName, Constants.DEFAULT_TASK)
                .eq(TMeasureTask::getMeasureType, measureType).orderByDesc(TMeasureTask::getCreateTime)
                .last("limit 1");
        task = tMeasureTaskService.getOne(wrapper);
        if (task == null) {
            //请创建方法
            return Result.fail(YamlMessageSource.getMessageStatic("00101006"));
        }
        vo.setTaskId(task.getId());
        vo.setTaskName(task.getTaskName());
        vo.setTaskStatus(task.getTaskStatus());
        method = tMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            return Result.fail(YamlMessageSource.getMessageStatic("00301010"));
        }
        TMeasureProject project = tMeasureProjectService.getById(method.getProjectItemKey());
        if (project == null) {
            return Result.fail(YamlMessageSource.getMessageStatic("00000011"));
        }
        vo.setMethodId(method.getId());
        vo.setMethodName(method.getMethodName());
        vo.setProjectId(project.getId());
        vo.setProjectName(project.getProjectName());
        return Result.ok(vo);
    }

    @Override
    public Result<MeasurementCurrentInfoVO> updateMethodParam(PhotometricMethodParameterDto methodDto) {
        PhotometricParameter parameter = new PhotometricParameter();
        BeanUtils.copyProperties(methodDto, parameter);
        MeasurementCurrentInfoVO vo = new MeasurementCurrentInfoVO();
        String clearDataRange = methodDto.getClearDataRange();
        if (StringUtils.hasText(clearDataRange) && RationChangeMethodTypeEnums.noChange.getIdentity().equals(clearDataRange)) {
            //设置参数给仪器。。。待定
            return Result.ok(vo);
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodDto.getCurrentMethodId());
        TMeasureMethod newMethod = new TMeasureMethod();
        newMethod.setMethodName(StringUtils.hasText(methodDto.getMethodName())? methodDto.getMethodName() : method.getMethodName() + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        newMethod.setMethodCreator(methodDto.getUpdateBy());
        newMethod.setMethodCreateTime(LocalDateTime.now());
        newMethod.setIsDelete(createType);
        newMethod.setProjectItemKey(method.getProjectItemKey());
        newMethod.setMeasurementType(method.getMeasurementType());
        newMethod.setMeasurementParamInfo(JSON.toJSONString(parameter));
        newMethod.setPreprocessKey(methodDto.getPreprocessKey());
        boolean save = tMeasureMethodService.save(newMethod);
        if (!save) {
            //创建失败
            return Result.fail(YamlMessageSource.getMessageStatic("方法修改失败"));
        }

        vo.setMethodId(newMethod.getId());
        vo.setMethodName(newMethod.getMethodName());
        vo.setProjectId(newMethod.getProjectItemKey());
        vo.setProjectName(tMeasureProjectService.getById(newMethod.getProjectItemKey()).getProjectName());

        newMethod = tMeasureMethodService.getById(newMethod.getId());

        if (!StringUtils.hasText(methodDto.getTaskId())) {
            return Result.ok(vo);
        }

        TMeasureTask task = tMeasureTaskService.getById(methodDto.getTaskId());
        if (task == null || task.getTaskStatus() == null) {

            return Result.ok(vo);
        }
        if (TaskStatusEnum.UNTESTED.getCode().equals(task.getTaskStatus())) {
            //任务是新建状态
            task.setMethodKey(newMethod.getId());
            task.setCreateTime(LocalDateTime.now());
            if (StringUtils.hasText(methodDto.getNewTaskName())) {
                task.setTaskName(methodDto.getNewTaskName());
            }
            tMeasureTaskService.updateById(task);
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getTaskName());
            vo.setTaskStatus(task.getTaskStatus());
            return Result.ok(vo);
        }

        if (TaskStatusEnum.TESTED.getCode().equals(task.getTaskStatus())) {
            //任务已测

            if (StringUtils.hasText(clearDataRange)) {

                if (RationChangeMethodTypeEnums.analyze.getIdentity().equals(clearDataRange) && methodDto.getIsApplyToCurrent()) {
                    task.setMethodKey(newMethod.getId());
                    task.setCreateTime(LocalDateTime.now());
                    tMeasureTaskService.updateById(task);

                    generateTableInfo(task.getId(), newMethod.getId(),false);
                    task = tMeasureTaskService.getById(task.getId());

                    List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
                    if (taskSpecimen != null && !taskSpecimen.isEmpty()) {
                        for (TaskSpecimen specimen : taskSpecimen) {
                            if (specimen.getIsTested().equals(noTested) || specimen.getDeleteType().equals(deleteType)) {
                                continue;
                            }
                            dealData(task, specimen.getSpecimenKey(), parameter);
                        }
                    }

                    vo.setTaskId(task.getId());
                    vo.setTaskName(task.getTaskName());
                    vo.setTaskStatus(task.getTaskStatus());
                    return Result.ok(vo);
                }
            }
        }

        TMeasureTask newTask = new TMeasureTask();
        newTask.setMethodKey(newMethod.getId());
        newTask.setCreateTime(LocalDateTime.now());

        SysUserPO sysUserPo = userManageDAO.selectByUsername(methodDto.getUpdateBy());
        newTask.setUserId(sysUserPo.getId());

        String taskName = task.getTaskName() + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        if (StringUtils.hasText(methodDto.getNewTaskName())) {
            taskName = methodDto.getNewTaskName();
        }

        newTask.setTaskName(taskName);
        newTask.setProjectItemKey(newMethod.getProjectItemKey());
        newTask.setTaskStatus(TaskStatusEnum.UNTESTED.getCode());
        newTask.setMeasureType(newMethod.getMeasurementType());
        newTask.setTableInfo(StringUtils.hasText(task.getTableInfo())?task.getTableInfo() : null);
        tMeasureTaskService.save(newTask);

        //生成表头和映射关系
        generateTableInfo(newTask.getId(), newTask.getMethodKey(),true);
        vo.setTaskId(newTask.getId());
        vo.setTaskName(newTask.getTaskName());
        vo.setTaskStatus(newTask.getTaskStatus());
        return Result.ok(vo);
    }

    private R<Boolean> dealData(TMeasureTask task, String sampleId, PhotometricParameter parameter) {

        /*
        分析数据，不保存数据
        查找测量数据，公式计算
         */
        TMeasureSample sample = tMeasureSampleService.getById(sampleId);
        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, task.getId())
                .eq(TMeasureData::getSampleKey, sampleId)
                .orderByDesc(TMeasureData::getSerialNumber)
                .last("limit 1");
        TMeasureData one = tMeasureDataService.getOne(queryWrapper);
        if (one == null || !StringUtils.hasText(one.getOriginalCalculationData())) {
            log.info("没有测量数据");
            //没有测量数据
            return R.fail(YamlMessageSource.getMessageStatic("00103016"));
        }

        int digit = Constants.DEFAULT_DECIMAL_NUM;
        List<PhotometricOriginDataInfo> listData = JSON.parseArray(one.getOriginalCalculationData(), PhotometricOriginDataInfo.class);
        R<TMeasureTaskAttachment> attachmentR = tMeasureTaskAttachmentService.queryByTaskId(task.getId());
        if (attachmentR.getSuccess()) {
            TMeasureTaskAttachment attachment = attachmentR.getData();
            if (StringUtils.hasText(attachment.getDataParam())) {
                FQueryDataDTO queryDataDTO = JSON.parseObject(attachment.getDataParam(), FQueryDataDTO.class);
                if (queryDataDTO.getDigit() != null) {
                    digit = queryDataDTO.getDigit();
                }
            }
        }

        R<List<PhotometricCountDataInfo>> formula = formula(listData, sample, task, parameter, digit);
        if (!formula.getSuccess()) {
            return R.fail(formula.getMessage());
        }

        List<PhotometricCountDataInfo> data = formula.getData();
        data = dataRounding(data, parameter);

        LambdaQueryWrapper<TMeasureAnalysisData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TMeasureAnalysisData::getTaskId, task.getId())
                .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                .eq(TMeasureAnalysisData::getMeasureDataId, one.getId())
                .orderByAsc(TMeasureAnalysisData::getDataVersion)
                .last("limit 1");

        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(wrapper);
        if (analysisData != null) {
            analysisData.setAnalysisResult(JSON.toJSONString(data));
            boolean b = tMeasureAnalysisDataService.updateById(analysisData);
            if (!b) {
                log.warn("更新数据失败");
                //更新数据失败
                return R.fail(YamlMessageSource.getMessageStatic("00103017"));
            }
        }
        return R.ok(true);

    }

    /**
     *  生成表头和映射信息
     */
    private R<Boolean> generateTableInfo(String taskId, String methodId, Boolean clearOld) {

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null || !StringUtils.hasText(method.getMeasurementParamInfo())) {
            //方法不存在
            return R.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //任务不存在
            return R.fail(YamlMessageSource.getMessageStatic("00000008"));
        }
        PhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
        if (parameter == null || parameter.getSampleInputParams() == null || parameter.getSampleInputParams().size() <= parameter.getWaveLength().size()) {
            log.info("参数信息不存在");
            return R.ok();
        }
        List<InputParam> inputParams = parameter.getSampleInputParams();


        TaskHeadSnapshot taskHeadSnapshot = new TaskHeadSnapshot();
        if (StringUtils.hasText(task.getTableInfo()) && !clearOld) {
            taskHeadSnapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
            if (taskHeadSnapshot == null) {
                taskHeadSnapshot = new TaskHeadSnapshot();
            }

        }
        List<HeadVO> heads = taskHeadSnapshot.getSampleHeads();
        if (heads == null|| heads.isEmpty()) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
            heads = new ArrayList<>(preHead);
        }

        Set<String> allHeadIdentity = new HashSet<>();

        for (HeadVO head : heads) {
            allHeadIdentity.add(head.getIdentity());
        }

        List<InputParamMapTableHead> mapRelation = taskHeadSnapshot.getMapRelation();
        Map<String, String> map = new HashMap<>();
        for (InputParamMapTableHead relation : mapRelation) {
            map.put(relation.getParamIdentity(), relation.getHeadIdentity());
        }

        String luminosity = parameter.getLuminosity();
        List<String> waveLength = parameter.getWaveLength();

        for (InputParam param : inputParams) {
            String resultIdentity = param.getResultIdentity();
            if (StringUtils.hasText(map.get(resultIdentity))) {
                log.info("当前输入参数{}已存在", resultIdentity);
                if (allHeadIdentity.contains(map.get(resultIdentity))) {
                    log.info("当前输入参数{}绑定的表头{}存在", resultIdentity, map.get(resultIdentity));
                    continue;
                }
            }

            if (resultIdentity.matches("[A-Z]")) {
                log.info("大写字母，只绑定，不添加");
                int letterIndex = resultIdentity.charAt(0) - 'A' + 1;

                if (letterIndex <= waveLength.size()) {
                    InputParamMapTableHead relation = new InputParamMapTableHead();
                    relation.setHeadName(param.getParamName());
                    relation.setHeadIdentity(waveLength.get(letterIndex - 1) + "-" + luminosity);
                    relation.setParamName(param.getParamName());
                    relation.setParamIdentity(resultIdentity);
                    mapRelation.add(relation);
                    continue;
                }
            }

            String newHeadIdentity = randomString(8);
            while (allHeadIdentity.contains(newHeadIdentity)) {
                newHeadIdentity = randomString(8);
            }

            //创建表头
            HeadVO newHead = new HeadVO();
            newHead.setName(param.getParamName());
            newHead.setPrintTitle(param.getParamName());
            newHead.setIdentity(newHeadIdentity);
            newHead.setUnit(param.getUnit());
            newHead.setLocate(TableHeadILocateEnums.sample.getIdentity());

            heads.add(newHead);
            InputParamMapTableHead relation = new InputParamMapTableHead();
            relation.setHeadName(param.getParamName());
            relation.setHeadIdentity(newHeadIdentity);
            relation.setParamName(param.getParamName());
            relation.setParamIdentity(resultIdentity);
            mapRelation.add(relation);
            allHeadIdentity.add(newHeadIdentity);
        }
        taskHeadSnapshot.setSampleHeads(heads);
        taskHeadSnapshot.setMapRelation(mapRelation);
        task.setTableInfo(JSON.toJSONString(taskHeadSnapshot));
        boolean update = tMeasureTaskService.updateById(task);

        return update ? R.ok(true) : R.fail(YamlMessageSource.getMessageStatic("00000010"));


    }

    private String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }

    @Override
    public Result addOrDeleteSample(AddOrDeleteSampleDTO dto) {

        if (!StringUtils.hasText(dto.getMethodId())) {
            //请先创建方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000011"));
        }
        if (dto.getHeads() == null || dto.getHeads().isEmpty()) {
            //至少需要一个表头
            return Result.fail(YamlMessageSource.getMessageStatic("00000012"));
        }

        if (dto.getHeads().size() > 10) {
            //表格新增列数不能超过10列
            return Result.fail(YamlMessageSource.getMessageStatic("00000013"));
        }

        if (dto.getNewSamples()!=null&&!dto.getNewSamples().isEmpty()) {
            for (Map<String, String> map : dto.getNewSamples()) {
                String jsonString = JSON.toJSONString(map);
                SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);
                if (sample.getSampleNumber()!=null&&sample.getSampleNumber().length()>256){
                    return Result.fail("新增样品编号超出限制");
                }
                if (sample.getSampleName()!=null&&sample.getSampleName().length()>256){
                    return Result.fail("新增样品名称超出限制");
                }

            }
        }
        if (!StringUtils.hasText(dto.getTaskId())) {
            //创建任务
            TMeasureTask newTask = new TMeasureTask();

            TMeasureMethod method = tMeasureMethodService.getById(dto.getMethodId());
            SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, dto.getUpdateBy()));
            String id = po.getId();
            newTask.setUserId(id);
            newTask.setProjectItemKey(method.getProjectItemKey());
            newTask.setMethodKey(method.getId());
            newTask.setMeasureType(method.getMeasurementType());
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            String collect = new Random().ints(8, 0, chars.length())
                    .mapToObj(i -> String.valueOf(chars.charAt(i)))
                    .collect(Collectors.joining());

            newTask.setTaskName(method.getMeasurementType() + "task_" + collect);
            newTask.setCreateTime(LocalDateTime.now());
            boolean save = false;
            for (int i = 0; i < 3; i++) {
                if (tMeasureTaskService.save(newTask)) {
                    save = true;
                    break;
                }
            }
            if (!save) {
                //系统出错
                return Result.fail(YamlMessageSource.getMessageStatic("00000014"));
            }
            dto.setTaskId(newTask.getId());
        }


        TMeasureTask task = tMeasureTaskService.getById(dto.getTaskId());
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        if (taskSpecimen == null) {
            taskSpecimen = new ArrayList<>();
        }
        for (TaskSpecimen specimen : taskSpecimen) {
            if (dto.getDelSampleIds().contains(specimen.getSpecimenKey())) {
                specimen.setDeleteType(deleteType);
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(TMeasureData::getTaskId, task.getId())
                        .orderByDesc(TMeasureData::getSerialNumber);
                List<TMeasureData> list = tMeasureDataService.list(queryWrapper);
                if (list != null && !list.isEmpty()) {
                    TMeasureData one = list.get(0);
                    one.setIsDelete(deleteType);
                    tMeasureDataService.updateById(one);
                }
            }
        }

        TaskHeadSnapshot taskHeadSnapshot = new TaskHeadSnapshot();

        if (StringUtils.hasText(task.getTableInfo())) {
            taskHeadSnapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
        }

        if (dto.getHeads() != null && !dto.getHeads().isEmpty()) {
            taskHeadSnapshot.setSampleHeads(dto.getHeads());
        }

        List<InputParamMapTableHead> mapRelation = taskHeadSnapshot.getMapRelation();
        List<String> allHeadIdentity = new ArrayList<>();

        if (mapRelation != null && !mapRelation.isEmpty()) {
            taskHeadSnapshot.getSampleHeads().forEach(head -> {
                allHeadIdentity.add(head.getIdentity());
            });
            taskHeadSnapshot.getTaskHeads().forEach(head -> {
                allHeadIdentity.add(head.getIdentity());
            });
            List<InputParamMapTableHead> newMapRelation = new ArrayList<>();
            mapRelation.forEach(map -> { if (allHeadIdentity.contains(map.getHeadIdentity())) {
                newMapRelation.add(map);
            }});

            taskHeadSnapshot.setMapRelation(newMapRelation);

        }

        taskHeadSnapshot.setTaskId(task.getId());
        task.setTableInfo(JSON.toJSONString(taskHeadSnapshot));

        List<String> sampleNos = dto.getNewSamples().stream()
                .map(m -> m.get(SampleTableEnum.SAMPLE_NUMBER.getIdentity()))
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());

        if (sampleNos.size() < dto.getNewSamples().size()) {
            return Result.fail(YamlMessageSource.getMessageStatic("0000004E"));
        }
        // 查重：找到重复的样品编号
        Set<String> duplicates = sampleNos.stream()
                .filter(no -> Collections.frequency(sampleNos, no) > 1)
                .collect(Collectors.toSet());
        // 判断如果isTested为true，
        if (!duplicates.isEmpty()) {
            //标准样品编号重复
            return Result.fail(YamlMessageSource.getMessageStatic("00102001"));
        }

        for (Map<String, String> map : dto.getNewSamples()) {
            String jsonString = JSON.toJSONString(map);
            SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);
            TMeasureSample newSample = new TMeasureSample();
            BeanUtils.copyProperties(sample, newSample);
            newSample.setId(sample.getSampleId());

            if (dto.getHeads() != null) {
                JSONObject newColData = new JSONObject();

                for (HeadVO head : dto.getHeads()) {
                    if (head.getLocate().equals(TableHeadILocateEnums.system.getIdentity())) {
                        continue;
                    }
                    String s = map.get(head.getIdentity());
                    newColData.put(head.getIdentity(), s);
                }
                newSample.setHeads(JSON.toJSONString(dto.getHeads()));
                newSample.setNewColData(JSON.toJSONString(newColData));
            }

            if (StringUtils.hasText(newSample.getId())) {
                tMeasureSampleService.updateById(newSample);
            } else {
                TaskSpecimen specimen = new TaskSpecimen();
                specimen.setDeleteType(createType);
                specimen.setIsTested(noTested);
                specimen.setSampleType(newSample.getSampleType());
                log.info("新增的样品：{}", newSample);
                tMeasureSampleService.save(newSample);
                specimen.setSpecimenKey(newSample.getId());
                taskSpecimen.add(specimen);
            }
        }

        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);

        return Result.ok(tMeasureTaskService.updateById(task));
    }

    @Override
    public Result<Boolean> deleteSample(String taskId, String sampleId) {

        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(sampleId)) {
            //请选择需要删除的任务和样品
            return Result.fail(YamlMessageSource.getMessageStatic("00000015"));
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            //任务无样品
            return Result.fail(YamlMessageSource.getMessageStatic("00000016"));
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen specimen : taskSpecimen) {
            if (sampleId.equals(specimen.getSpecimenKey())) {
                specimen.setDeleteType(deleteType);
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(TMeasureData::getTaskId, task.getId())
                         .orderByDesc(TMeasureData::getSerialNumber);
                List<TMeasureData> list = tMeasureDataService.list(queryWrapper);
                if (list != null && !list.isEmpty()) {
                    TMeasureData one = list.get(0);
                    one.setIsDelete(deleteType);
                    tMeasureDataService.updateById(one);
                }
            }
        }
        task.setSampleKey(JSON.toJSONString(taskSpecimen));
        tMeasureTaskService.updateById(task);
        return Result.ok(true);
    }

    @Override
    public Result revokeDel(String taskId, String sampleId) {
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        String sampleKey = task.getSampleKey();

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(sampleKey, TaskSpecimen.class);
        for (TaskSpecimen specimen : taskSpecimen) {
            if (specimen.getSpecimenKey().equals(sampleId)) {
                specimen.setDeleteType(createType);
            }
            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                    .eq(TMeasureData::getTaskId, task.getId())
                     .orderByDesc(TMeasureData::getSerialNumber);
            List<TMeasureData> list = tMeasureDataService.list(queryWrapper);
            if (list != null && !list.isEmpty()) {
                TMeasureData one = list.get(0);
                one.setIsDelete(createType);
                tMeasureDataService.updateById(one);
            }

        }
        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);

        return Result.ok(tMeasureTaskService.updateById(task));
    }

    @Override
    public Result<List<JSONObject>> newQuerySampleInfo(String taskId) {
        List<JSONObject> sampleVOList = new ArrayList<>();
        if (!StringUtils.hasText(taskId)) {
            return Result.ok(sampleVOList);
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        if (StringUtils.hasText(task.getSampleKey())) {
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(deleteType)) {
                    continue;
                }
                TMeasureSample measureSample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                SampleVO vo = new SampleVO();
                BeanUtils.copyProperties(measureSample, vo);
                vo.setSampleId(measureSample.getId());
                String jsonString = JSONObject.toJSONString(vo);

                JSONObject jsonObject = JSONObject.parseObject(jsonString);
                if (StringUtils.hasText(measureSample.getNewColData())) {
                    JSONObject object = JSONObject.parseObject(measureSample.getNewColData());
                    jsonObject.putAll(object);
                }
                sampleVOList.add(jsonObject);

            }
        }
        return Result.ok(sampleVOList);
    }

    @Override
    public Result<List<HeadVO>> querySampleHeads(String taskId) {

        if (!StringUtils.hasText(taskId)) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
            return Result.ok(preHead);
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        if (!StringUtils.hasText(task.getTableInfo())) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
            return Result.ok(preHead);
        } else {
            TaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
            if (snapshot.getSampleHeads() != null && !snapshot.getSampleHeads().isEmpty()) {
                return Result.ok(snapshot.getSampleHeads());
            } else {
                List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
                return Result.ok(preHead);
            }
        }
    }

    @Override
    public Result<MethodParam> queryMethodParam(String methodId) {

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }

        if (!StringUtils.hasText(method.getMeasurementParamInfo())) {
            //未查询到该方法参数
            return Result.fail(YamlMessageSource.getMessageStatic("00000017"));
        }
        if(methodId.length() != 32){
            //方法参数格式错误
            return  Result.fail(YamlMessageSource.getMessageStatic("00000018"));
        }
        PhotometricParameter photometricParameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);

        MethodParam param = new MethodParam();
        BeanUtils.copyProperties(photometricParameter, param);
        param.setMethodId(method.getId());
        param.setMethodName(method.getMethodName());
        param.setMeasurementType(method.getMeasurementType());
        param.setLuminosityName(WaveLengthEnum.getName(photometricParameter.getLuminosity()));
        param.setSamplePoolWayName(RepeatConfigEnum.getName(photometricParameter.getSamplePoolWay()));
//        param.setPhotometricParameter(photometricParameter);
        return Result.ok(param);
    }

    @Override
    public R<PhotometricReportDataVO> getReportCount(String taskId) {

        if (!StringUtils.hasText(taskId)) {
            //缺少任务id
            return R.fail(YamlMessageSource.getMessageStatic("00000019"));
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        PhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);

        if (parameter.getResultParams() == null || parameter.getResultParams().isEmpty()) {
            //缺少结果参数
            return R.fail(YamlMessageSource.getMessageStatic("00302002"));
        }

        List<HeadVO> heads = new ArrayList<>();

        List<Map<String, Object>> data = new ArrayList<>();
        PhotometricReportDataVO ans = new PhotometricReportDataVO();

        heads.add(new HeadVO("样品编号", "sampleNumber"));
        heads.add(new HeadVO("样品名称", "sampleName"));
        heads.add(new HeadVO("样品类型", "sampleType"));
        for (PreCountResultParam param : parameter.getResultParams()) {
            heads.add(new HeadVO(param.getResultName(), param.getResultIdentity() + "-" +param.getFormulaText()));
        }
        ans.setHeadList(heads);

        if (StringUtils.hasText(task.getSampleKey())) {
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

            for (TaskSpecimen specimen : taskSpecimen) {
                Map<String, Object> map = new HashMap<>();
                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                map.put("sampleName", sample.getSampleName());
                map.put("sampleNumber", sample.getSampleNumber());
                map.put("sampleType", SampleTypeEnum.getName(sample.getSampleType()));

                LambdaQueryWrapper<TMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                        .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey());
                List<TMeasureAnalysisData> list = tMeasureAnalysisDataService.list(queryWrapper);
                if (list != null && !list.isEmpty()) {
                    TMeasureAnalysisData analysisData = list.get(0);
                    List<PhotometricCountDataInfo> countDataInfos = JSON.parseArray(analysisData.getAnalysisResult(), PhotometricCountDataInfo.class);

                    if (countDataInfos != null && !countDataInfos.isEmpty()) {
                        for (PhotometricCountDataInfo count : countDataInfos) {
                            double value = Double.parseDouble(count.getCountValue());
                            map.put(count.getResultIdentity() + "-" + count.getFormulaText(), String.format("%.2f",value));
                        }
                    }
                    data.add(map);
                }
            }
        }
        ans.setDataList(data);

        return R.ok(ans);
    }

    @Override
    public Result<List<HeadVO>> tableHead(String taskId, String methodId, Boolean allDisplay, String deviceSerialNumber) {
        if (!StringUtils.hasText(taskId)) {
            //请选择任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000006"));
        }
        if (!StringUtils.hasText(methodId)){
            //请选择方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000002"));
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        PhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
        LinkedHashSet<HeadVO> ans = new LinkedHashSet<>();
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(method.getMeasurementType(), null);
            ans.addAll(preHead);
            return Result.ok(new ArrayList<>(ans));
        }

        if (StringUtils.hasText(task.getTableInfo())) {
            TaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
            if (snapshot != null) {
                if (snapshot.getTaskHeads()!= null && !snapshot.getTaskHeads().isEmpty()) {
                    ans.addAll(snapshot.getTaskHeads());
                }
                if (snapshot.getSampleHeads() != null && !snapshot.getSampleHeads().isEmpty()) {
                    ans.addAll(snapshot.getSampleHeads());
                }
            }
        } else {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(method.getMeasurementType(), null);
            ans.addAll(preHead);
        }

        if (StringUtils.hasText(task.getSampleKey())) {

            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen specimen : taskSpecimen) {
                //过滤掉已删除的内容
                if (specimen.getDeleteType().equals(deleteType)) {
                    continue;
                }

                TMeasureSample measureSample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                if (StringUtils.hasText(measureSample.getHeads())){
                    List<HeadVO> heads = JSON.parseArray(measureSample.getHeads(), HeadVO.class);

                    if (heads != null && !heads.isEmpty()) {
                        ans.addAll(heads);
                    }
                }
            }

        }

        List<HeadVO> vos = dealHeads(new ArrayList<>(ans), parameter, allDisplay, deviceSerialNumber);

        return Result.ok(vos);
    }

    private List<HeadVO> dealHeads(List<HeadVO> heads, PhotometricParameter parameter, Boolean allDisplay, String deviceSerialNumber) {
        List<String> waveLength = parameter.getWaveLength();
        Map<String, HeadVO> map = heads.stream().collect(Collectors.toMap(HeadVO::getIdentity, head -> head));

        Map<String, List<HeadVO>> headsMap = new HashMap<>();

        Map<String, HeadVO> analyzeMap = new HashMap<>();

        for (HeadVO head : heads) {

            if (head.getIdentity().equals("remark")) {
                head.setLocate(TableHeadILocateEnums.remark.getIdentity());
            }
            headsMap.computeIfAbsent(head.getLocate(), k -> new ArrayList<>()).add(head);
            if (head.getLocate().equals(TableHeadILocateEnums.measure.getIdentity())) {
                map.put(head.getIdentity(), head);
            }
            if (head.getLocate().equals(TableHeadILocateEnums.analyze.getIdentity())) {
                analyzeMap.put(head.getIdentity(), head);
            }
        }

        List<HeadVO> allMeasureHeads = new ArrayList<>();

        String mode = parameter.getLuminosity();
        if (mode.equals(WaveLengthEnum.es.getCode()) || mode.equals(WaveLengthEnum.er.getCode())) {
            for (String s : waveLength) {
                String identity = s + "-"+ mode;
                String name = s + WaveLengthEnum.getName(mode);
                String unit = s + "nm";
                HeadVO newHead = new HeadVO(name, identity, unit, TableHeadILocateEnums.measure.getIdentity(), name);
                if (map.containsKey(identity)) {
                    newHead = map.get(identity);
                }
                allMeasureHeads.add(newHead);
            }
        } else if (mode.equals(WaveLengthEnum.abs.getCode()) || mode.equals(WaveLengthEnum.tPercent.getCode())) {
            for (String s : waveLength) {
                String absIdentity = s + "-"+ WaveLengthEnum.abs.getCode();
                String tPercentIdentity = s + "-"+ WaveLengthEnum.tPercent.getCode();
                String absName = WaveLengthEnum.abs.getName();
                String tPercentName = WaveLengthEnum.tPercent.getName();
                String unit = s + "nm";

                HeadVO absHead = new HeadVO(absName, absIdentity,unit, TableHeadILocateEnums.measure.getIdentity(), absName);
                HeadVO tPercentHead = new HeadVO(tPercentName, tPercentIdentity, unit, TableHeadILocateEnums.measure.getIdentity(), tPercentName);

                if (map.containsKey(absIdentity)) {
                    absHead = map.get(absIdentity);
                }
                if (allDisplay) {
                    if (map.containsKey(tPercentIdentity)) {
                        tPercentHead = map.get(tPercentIdentity);
                    }
                    allMeasureHeads.add(absHead);
                    allMeasureHeads.add(tPercentHead);
                } else {
                    if (mode.equals(WaveLengthEnum.tPercent.getCode())) {
                        allMeasureHeads.add(tPercentHead);
                    } else {
                        allMeasureHeads.add(absHead);
                    }
                }
            }
        }
        List<HeadVO> ans = new ArrayList<>();
        List<HeadVO> systemHeads = headsMap.get(TableHeadILocateEnums.system.getIdentity());
        if (systemHeads != null) {
            ans.addAll(systemHeads);
        }
        for (HeadVO an : ans) {
            if (an.getIdentity().equals(SampleTableEnum.SAMPLE_LOCATION.getIdentity())) {
                ans.remove(an);
                break;
            }
        }

        if (StringUtils.hasText(deviceSerialNumber)) {

            R<FSamplePoolInfoVO> poolInfo = tSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
            if (poolInfo.getSuccess()) {
                FSamplePoolInfoVO data = poolInfo.getData();
                if (data.getIsAutoChangePool() ){
                    String sampleLocationName = SampleTableEnum.SAMPLE_LOCATION.getName();
                    String sampleLocationIdentity = SampleTableEnum.SAMPLE_LOCATION.getIdentity();
                    HeadVO sampleLocation = new HeadVO(sampleLocationName, sampleLocationIdentity, TableHeadILocateEnums.location.getIdentity(), sampleLocationName);
                    ans.add(sampleLocation);
                }
            }
        }
        List<HeadVO> sampleHeads = headsMap.get(TableHeadILocateEnums.sample.getIdentity());
        if (sampleHeads != null) {
            ans.addAll(sampleHeads);
        }
        ans.addAll(allMeasureHeads);
        List<HeadVO> taskHeads = headsMap.get(TableHeadILocateEnums.data.getIdentity());
        if (taskHeads != null) {
            ans.addAll(taskHeads);
        }

        List<PreCountResultParam> resultParams = parameter.getResultParams();
        List<HeadVO> headAnalyze = new ArrayList<>();
        if (resultParams == null) {
            resultParams = new ArrayList<>();
        }
        for (PreCountResultParam results : resultParams) {
            //TODO 方法参数标识待定，后续再改动
            if (!StringUtils.hasText(results.getParam())) {
                results.setParam(results.getResultIdentity() + "-" + results.getFormulaText());
            }
            String name = results.getResultName();
            String unit = results.getUnit();
            HeadVO analyzeHead = new HeadVO(name, results.getParam(), unit, TableHeadILocateEnums.analyze.getIdentity(), name);

            if (analyzeMap.containsKey(results.getParam())) {
                analyzeHead = analyzeMap.get(results.getParam());
            }
            headAnalyze.add(analyzeHead);
        }
        ans.addAll(headAnalyze);

        List<HeadVO> other = headsMap.get(TableHeadILocateEnums.other.getIdentity());
        if (other == null || other.isEmpty()) {
            other = new ArrayList<>();
            HeadVO samplingTimeHead = new HeadVO(SampleTableEnum.SAMPLING_TIME.getName(), SampleTableEnum.SAMPLING_TIME.getIdentity(), TableHeadILocateEnums.other.getIdentity(), SampleTableEnum.SAMPLING_TIME.getName());
            other.add(samplingTimeHead);
        }
        ans.addAll(other);
        List<HeadVO> remarks = headsMap.get(TableHeadILocateEnums.remark.getIdentity());
        if (remarks == null || remarks.isEmpty()) {
            remarks = new ArrayList<>();
            HeadVO remark = new HeadVO(SampleTableEnum.REMARK.getName(), SampleTableEnum.REMARK.getIdentity(), TableHeadILocateEnums.remark.getIdentity(), SampleTableEnum.REMARK.getName());
            remarks.add(remark);
        }
        ans.addAll(remarks);

        return ans;
    }

    @Override
    public R<List<PhotometricCountDataInfo>> formula(List<PhotometricOriginDataInfo> vo, TMeasureSample sample, TMeasureTask task, PhotometricParameter parameter, Integer digit) {

        if (!StringUtils.hasText(task.getTableInfo())) {
            //缺少分析参数，无法计算分析
            return R.fail(YamlMessageSource.getMessageStatic("00300003"));
        }
        TaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
        if (snapshot.getMapRelation() == null || snapshot.getMapRelation().isEmpty()) {
            //缺少映射关系，无法计算分析
            return R.fail(YamlMessageSource.getMessageStatic("00300004"));
        }
        String jsonString = JSON.toJSONString(sample);
        JSONObject allData = JSON.parseObject(jsonString);
        if (StringUtils.hasText(sample.getNewColData())) {
            JSONObject object = JSONObject.parseObject(sample.getNewColData());
            allData.putAll(object);
        }
        LambdaQueryWrapper<TMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureAnalysisData::getTaskId, task.getId())
                .eq(TMeasureAnalysisData::getSampleKey, sample.getId())
                .orderByDesc(TMeasureAnalysisData::getDataVersion)
                .last("limit 1");
        TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(queryWrapper);
        if (one != null && StringUtils.hasText(one.getAnalysisParam())) {
            String analysisParam = one.getAnalysisParam();
            JSONObject object = JSON.parseObject(analysisParam);
            allData.putAll(object);
        }

        for (PhotometricOriginDataInfo dataInfo : vo) {
            if (dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                allData.putAll(dataInfo.getData());
            }
        }

        StringBuilder values = new StringBuilder();

        StringBuilder identity = new StringBuilder();

        List<InputParamMapTableHead> mapRelation = snapshot.getMapRelation();
        for (InputParamMapTableHead relation : mapRelation) {

            String s = allData.getString(relation.getHeadIdentity());
            if (!StringUtils.hasText(s)) {
                //缺少数据！
                return R.fail(YamlMessageSource.getMessageStatic("00300006"));
            }
            identity.append(relation.getParamIdentity()).append(",");
            s = DataCountUtil.formatValueWithBigDecimal(digit, s);
            values.append(s).append(",");
        }

        identity.deleteCharAt(identity.length()-1);
        values.deleteCharAt(values.length()-1);

        List<PhotometricCountDataInfo> countList = new ArrayList<>();
        for (PreCountResultParam param : parameter.getResultParams()) {
            String[] split = param.getFormulaText().split("=");
            String text = split[split.length - 1];
            String result = restUtil.sum(text, identity.toString(), values.toString());
            PhotometricCountDataInfo countDataInfo = new PhotometricCountDataInfo();
            BeanUtils.copyProperties(param, countDataInfo);
            JSONObject resultJson = JSONObject.parseObject(result);

            if (!resultJson.get("code").equals("0") && !resultJson.get("code").equals(200)) {
                //"公式计算出错！" + resultJson.get("message")！
                return R.fail(YamlMessageSource.getMessageStatic("00300007",resultJson.get("message")));
            }
            countDataInfo.setCountValue(resultJson.get("result").toString());
            countList.add(countDataInfo);
        }

        return R.ok(countList);
    }


    @Override
    public Result<List<Map<String, Object>>> queryData(QueryDataDTO queryDataDTO) {
        String taskId = queryDataDTO.getTaskId();
        Boolean allDisplay = queryDataDTO.getAllDisplay();
        Integer digit = queryDataDTO.getDigit();
        Integer otherDigit = queryDataDTO.getOtherDigit();
        List<String> option = queryDataDTO.getOption();

        if (!StringUtils.hasText(taskId) || allDisplay == null) {
            //未选择任务
            return Result.fail(YamlMessageSource.getMessageStatic("0000001B"));
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        List<Map<String, Object>> ans = new ArrayList<>();
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("未找到样品");
            return Result.ok(ans);
        }

        if (digit == null || digit < 0) {
            digit = Constants.DEFAULT_DECIMAL_NUM;
            queryDataDTO.setDigit(digit);
        }

        R<TMeasureTaskAttachment> attachmentResult = tMeasureTaskAttachmentService.queryByTaskId(taskId);
        if (!attachmentResult.getSuccess()) {
            TMeasureTaskAttachment attachment = new TMeasureTaskAttachment();
            attachment.setTaskId(taskId);
            attachment.setDataParam(JSON.toJSONString(queryDataDTO));
            tMeasureTaskAttachmentService.save(attachment);
            attachmentResult = tMeasureTaskAttachmentService.queryByTaskId(taskId);

        }

        TMeasureTaskAttachment attachment = attachmentResult.getData();
        if (attachment == null) {
            attachment = new TMeasureTaskAttachment();
        }

        attachment.setTaskId(taskId);
        attachment.setDataParam(JSON.toJSONString(queryDataDTO));
        tMeasureTaskAttachmentService.saveOrUpdate(attachment);

        boolean repeat = false;
        PhotometricParameter parameter = null;
        String luminosity = WaveLengthEnum.abs.getCode();
        if (StringUtils.hasText(task.getMethodKey())) {
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                parameter= JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
                if (StringUtils.hasText(parameter.getRepetitionPattern()) && parameter.getRepetitionPattern().equals("单池重复")) {
                    repeat = true;
                }
                luminosity = parameter.getLuminosity();
            }
        }

        String deviceSerialNumber = queryDataDTO.getDeviceSerialNumber();

        List<Integer> usefulLocation = new ArrayList<>();



        if (StringUtils.hasText(deviceSerialNumber)) {
            R<FSamplePoolInfoVO> poolInfo = tSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
            if (poolInfo.getSuccess()) {
                FSamplePoolInfoVO data = poolInfo.getData();

                if (data.getIsAutoChangePool()) {
                    usefulLocation = tSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
                    publicUtilService.changeSampleLocation(task.getId(), usefulLocation);
                }
            }
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        for (TaskSpecimen specimen : taskSpecimen) {
            if (!allDisplay && specimen.getDeleteType().equals(deleteType)) {
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

            //样品信息
            map.put(SampleTableEnum.SAMPLE_ID.getIdentity(), sample.getId());
            map.put(SampleTableEnum.SAMPLE_NAME.getIdentity(), sample.getSampleName());
            map.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber());
            map.put(SampleTableEnum.SAMPLE_TYPE.getIdentity(), SampleTypeEnum.getName(sample.getSampleType()));

            map.put("deleteType", specimen.getDeleteType());
            if (StringUtils.hasText(queryDataDTO.getDeviceSerialNumber())) {

                if (specimen.getIsTested().equals(noTested) && !usefulLocation.isEmpty()) {
                    R<FSamplePoolInfoVO> poolInfo = tSamplePoolConfigService.querySamplePoolInfo(queryDataDTO.getDeviceSerialNumber());
                    if (poolInfo.getSuccess()) {
                        FSamplePoolInfoVO data = poolInfo.getData();
                        if (data.getIsAutoChangePool()) {
                            map.put(SampleTableEnum.SAMPLE_LOCATION.getIdentity(), "样品池" + sample.getSampleLocation());
                        }
                    }
                }
            }

            if (StringUtils.hasText(sample.getNewColData())){
                Map<String, String> object = JSONObject.parseObject(sample.getNewColData(), new TypeReference<Map<String, String>>() {});
                map.putAll(object);
            }

            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getTaskId, taskId)
                    .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                    .orderByDesc(TMeasureData::getSerialNumber)
                    .last("limit 1");
            TMeasureData data = tMeasureDataService.getOne(queryWrapper);
            if (!repeat) {
                if (data != null) {
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        List<PhotometricOriginDataInfo> wlDataList = JSON.parseArray(data.getOriginalCalculationData(), PhotometricOriginDataInfo.class);

                        for (PhotometricOriginDataInfo wlData : wlDataList) {
                            Map<String, String> originData = wlData.getData();
                            // 创建一个新的Map来存储格式化后的数据
                            Map<String, String> formattedData = new HashMap<>();
                            // 使用现有的工具类 DataCountUtil.formatValueWithBigDecimal 进行格式化
                            for (Map.Entry<String, String> entry : originData.entrySet()) {

                                int currentDigit = digit;
                                if (!entry.getKey().endsWith(luminosity)) {
                                    currentDigit = otherDigit;
                                }
                                Double v = DataCountUtil.formatValue(currentDigit, Double.valueOf(entry.getValue()));
                                String s = DataCountUtil.addZero(v, currentDigit);
                                formattedData.put(entry.getKey(), s);
                            }
                            map.putAll(formattedData);

                            Map<String, String> qcAns = checkValueQuantity(parameter, wlData, QualityConfEnums.value_qc);
                            Object object = map.get(SampleTableEnum.QUALITY.getIdentity());
                            if (object != null) {
                                Map<String, String> quality = new ObjectMapper().convertValue(object,
                                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                                qcAns.putAll(quality);
                            }
                            map.put(SampleTableEnum.QUALITY.getIdentity(), qcAns);
                            map.put("samplingTime", data.getSamplingTime());
                        }
                        Map<String, String> countData = getCountData(task, sample, parameter, digit, wlDataList);
                        map.putAll(countData);
                    }
                }
            } else {
                if (data != null) {
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        // 原先childrenList使用HashMap，子项无序
                        // Map<String, Map<String, String>> childrenList = new HashMap<>();

                        // 改为使用TreeMap并按identity分段排序
                        Map<String, Map<String, String>> childrenList = new TreeMap<>((key1, key2) -> {
                            String[] parts1 = key1.split("-");
                            String[] parts2 = key2.split("-");

                            for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
                                if (isNumeric(parts1[i]) && isNumeric(parts2[i])) {
                                    int num1 = Integer.parseInt(parts1[i]);
                                    int num2 = Integer.parseInt(parts2[i]);
                                    int compare = Integer.compare(num1, num2);
                                    if (compare != 0) {
                                        return compare;
                                    }
                                } else {
                                    int compare = parts1[i].compareTo(parts2[i]);
                                    if (compare != 0) {
                                        return compare;
                                    }
                                }
                            }

                            return Integer.compare(parts1.length, parts2.length);
                        });

                        List<Map<String, String>> middle = new ArrayList<>();
                        List<PhotometricOriginDataInfo> wlDataList = JSON.parseArray(data.getOriginalCalculationData(), PhotometricOriginDataInfo.class);

                        Map<String, Map<String, String>> countDataList = new TreeMap<>();
                        List<PhotometricOriginDataInfo> dataInfo = reCountData(wlDataList, digit);
                        for (PhotometricOriginDataInfo wlData : dataInfo) {

                            Map<String, String> originData = wlData.getData();
                            Map<String, String> formattedData = new HashMap<>();
                            if (wlData.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {

                                for (Map.Entry<String, String> entry : originData.entrySet()) {
//                                String formattedValue = DataCountUtil.formatValueWithBigDecimal(digit, entry.getValue());
                                    int currentDigit = digit;
                                    if (!entry.getKey().endsWith(luminosity)) {
                                        currentDigit = otherDigit;
                                    }
                                    Double v = DataCountUtil.formatValue(currentDigit, Double.valueOf(entry.getValue()));
                                    String s = DataCountUtil.addZero(v, currentDigit);
                                    formattedData.put(entry.getKey(), s);
                                }
                                map.putAll(formattedData);
                                Map<String, String> qcAns = checkValueQuantity(parameter, wlData, QualityConfEnums.value_qc);
                                Object object = map.get(SampleTableEnum.QUALITY.getIdentity());
                                if (object != null) {
                                    Map<String, String> quality = new ObjectMapper().convertValue(object,
                                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                                    qcAns.putAll(quality);
                                }
                                map.put(SampleTableEnum.QUALITY.getIdentity(), qcAns);

                                Map<String, Map<String, String>> ch = extracted(option, wlData, sample, digit, otherDigit);
                                if (!ch.isEmpty()) {
                                    for (Map.Entry<String, Map<String, String>> entry : ch.entrySet()) {
                                        String key = entry.getKey();
                                        Map<String, String> value = entry.getValue();

                                        if (countDataList.containsKey(key)) {
                                            // 如果键已存在，将新值中的键值对合并到现有Map中
                                            Map<String, String> existingMap = countDataList.get(key);
                                            // 将value中的所有键值对放入existingMap中（相同键会覆盖，新键会添加）
                                            existingMap.putAll(value);
                                        } else {
                                            // 如果键不存在，直接添加（创建副本以避免引用问题）
                                            countDataList.put(key, new HashMap<>(value));
                                        }
                                    }
                                }
                            } else {

                                // 使用现有的工具类 DataCountUtil.formatValueWithBigDecimal 进行格式化
                                for (Map.Entry<String, String> entry : originData.entrySet()) {
//                                    String formattedValue = DataCountUtil.formatValueWithBigDecimal(digit, entry.getValue());
                                    int currentDigit = digit;
                                    if (!entry.getKey().endsWith(luminosity)) {
                                        currentDigit = otherDigit;
                                    }

                                    Double v = DataCountUtil.formatValue(currentDigit, Double.valueOf(entry.getValue()));
                                    String s = DataCountUtil.addZero(v, currentDigit);
                                    formattedData.put(entry.getKey(), s);
                                }

                                Map<String, String> children = new HashMap<>(formattedData);
                                String[] split = wlData.getIdentity().split("-");
                                String pre = StringUtils.hasText(sample.getSampleNumber()) ? sample.getSampleNumber() + "-" : "";
                                children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + split[split.length - 1]);
                                children.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), wlData.getMeasureTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                                childrenList.computeIfAbsent(wlData.getIdentity(), k -> new HashMap<>()).putAll(children);

//                                childrenList.putIfAbsent(wlData.getIdentity(), children);
                            }
                        }

                        map.put("samplingTime", data.getSamplingTime());

                        childrenList.forEach((key, value) -> middle.add(value));
                        countDataList.forEach((key, value) -> middle.add(value));

                        map.put("children", middle);
                        Map<String, String> countData = getCountData(task, sample, parameter, digit, wlDataList);
                        map.putAll(countData);
                    }
                    map.put("remark", data.getRemark());
                }
            }

            LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
            analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, task.getId())
                    .eq(TMeasureAnalysisData::getSampleKey, sample.getId());
            if (data != null) {
                analysisDataQueryWrapper.eq(TMeasureAnalysisData::getMeasureDataId, data.getId());
            }
            analysisDataQueryWrapper.orderByDesc(TMeasureAnalysisData::getDataVersion).last("limit 1");

            TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
            if (one != null) {
                if (StringUtils.hasText(one.getAnalysisParam())) {
                    Map<String, String> jsonObject = JSON.parseObject(one.getAnalysisParam(), new TypeReference<Map<String, String>>() {});
                    map.putAll(jsonObject);
                }
            } else {
                analysisDataQueryWrapper.clear();
                analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, task.getId())
                        .eq(TMeasureAnalysisData::getSampleKey, sample.getId())
                        .orderByDesc(TMeasureAnalysisData::getDataVersion).last("limit 1");
                one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                if (one != null && StringUtils.hasText(one.getAnalysisParam())) {
                    Map<String, String> jsonObject = JSON.parseObject(one.getAnalysisParam(), new TypeReference<Map<String, String>>() {});
                    map.putAll(jsonObject);
                }
            }

            ans.add(map);
        }

        return Result.ok(ans);
    }

    public Map<String,String> getCountData(TMeasureTask task, TMeasureSample sample, PhotometricParameter parameter, Integer digit, List<PhotometricOriginDataInfo> wlDataList) {

        Map<String, String> map = new HashMap<>();


        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, task.getId())
                .eq(TMeasureData::getSampleKey, sample.getId())
                .orderByDesc(TMeasureData::getSerialNumber)
                .last("limit 1");
        TMeasureData data = tMeasureDataService.getOne(queryWrapper);

        R<List<PhotometricCountDataInfo>> formula = formula(wlDataList, sample, task, parameter, digit);
        if (formula.getSuccess()) {
            List<PhotometricCountDataInfo> formulaData = formula.getData();

            formulaData = dataRounding(formulaData, parameter);
            for (PhotometricCountDataInfo count : formulaData) {
                String s = count.getCountValue();
                map.put(count.getResultIdentity() + "-" + count.getFormulaText(), s);
            }
        } else {
            LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
            analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, task.getId())
                    .eq(TMeasureAnalysisData::getSampleKey, sample.getId());
            if (data != null) {
                analysisDataQueryWrapper.eq(TMeasureAnalysisData::getMeasureDataId, data.getId());
            }
            analysisDataQueryWrapper.orderByDesc(TMeasureAnalysisData::getDataVersion).last("limit 1");

            TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
            if (one != null) {
                List<PhotometricCountDataInfo> countList = JSON.parseArray(one.getAnalysisResult(), PhotometricCountDataInfo.class);

                countList = dataRounding(countList, parameter);

                if (countList != null) {
                    for (PhotometricCountDataInfo count : countList) {
                        if (count.getCountValue() != null) {
                            String s = count.getCountValue();
                            map.put(count.getResultIdentity() + "-" + count.getFormulaText(), s);
                        }
                    }
                }
            }
        }

        return map;
    }


    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.matches("-?\\d+");
    }

    private List<PhotometricOriginDataInfo> reCountData(List<PhotometricOriginDataInfo> wlDataList, Integer digit) {

        Map<String, List<Double>> data = new HashMap<>();
        Map<String, RepeatMeasureData> otherData = new HashMap<>();
        Map<String, String> ans = new HashMap<>();
        List<PhotometricOriginDataInfo> result = new ArrayList<>();
        List<PhotometricOriginDataInfo> middle = new ArrayList<>();

        for (PhotometricOriginDataInfo wlData : wlDataList) {
            if (wlData.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                result.add(wlData);
                continue;
            }
            middle.add(wlData);

            wlData.getData().forEach((key, value) -> {
                data.computeIfAbsent(key, k -> new ArrayList<>()).add(Double.parseDouble(value));
            });
        }

        for (Map.Entry<String, List<Double>> entry : data.entrySet()) {
            List<Double> values = entry.getValue();
            double mean = DataCountUtil.calculateMean(values, digit);
            Double sd = DataCountUtil.calculateSD(values, digit);
            Double rsd = DataCountUtil.calculateRSD(values, digit);

            RepeatMeasureData repeatMeasureData = new RepeatMeasureData();
            repeatMeasureData.setSd(sd == null ? "未检出" :DataCountUtil.addZero(sd, digit));
            repeatMeasureData.setRsd(rsd == null ? "未检出" :DataCountUtil.addZero(rsd, digit));
            otherData.put(entry.getKey(), repeatMeasureData);
            ans.put(entry.getKey(), DataCountUtil.addZero(mean, digit));
        }

        for (PhotometricOriginDataInfo info : result) {
            Map<String, String> ansMap = info.getData();
            ans.forEach((key, value) -> {
                if (ansMap.containsKey(key)) {
                    ansMap.put(key, value);
                }
            });
            info.setData(ans);

            Map<String, RepeatMeasureData> other = info.getOtherData();
            otherData.forEach((key, value) -> {
                if (other.containsKey(key)) {
                    other.put(key, value);
                }
            });
            info.setOtherData(other);
        }

        result.addAll(middle);


        return result;
    }


    private Map<String, Map<String, String>> extracted(List<String> option, PhotometricOriginDataInfo wlData,  TMeasureSample sample, Integer digit, Integer otherDigit) {

        Map<String, String> originData = wlData.getData();
        Map<String, Map<String, String>> ans = new HashMap<>();
        String mapName = wlData.getWavelength() + "-" + wlData.getMode();

        RepeatMeasureData absData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode());
        RepeatMeasureData tPercentData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode());
        String pre = StringUtils.hasText(sample.getSampleNumber()) ? sample.getSampleNumber() + "-" : "";

        SystemBusinessParamVO vo = businessParamService.queryBusinessParam();
        Integer sdDigits = vo.getSdDigits();

        if (wlData.getMode().equals(WaveLengthEnum.abs.getCode()) || wlData.getMode().equals(WaveLengthEnum.tPercent.getCode())) {

            Integer absDigit = digit;
            Integer tDigit = digit;
            if (wlData.getMode().equals(WaveLengthEnum.abs.getCode())) {
                tDigit = otherDigit;
            } else {
                absDigit = otherDigit;
            }


            if (!option.isEmpty()) {
                for (String s : option) {
                    Map<String, String> children = new HashMap<>();
                    if (DataIdentityEnums.avg.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + DataIdentityEnums.avg.getName());
                        String abs = DataCountUtil.formatValueWithBigDecimal(absDigit, originData.get(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode()));
                        String t = DataCountUtil.formatValueWithBigDecimal(tDigit, originData.get(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode()));

                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), DataCountUtil.addZero(Double.parseDouble(abs), absDigit));
                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), DataCountUtil.addZero(Double.parseDouble(t), tDigit));
                        ans.putIfAbsent(pre + "-" + DataIdentityEnums.avg.getIdentity(), children);
                    }
                    if (DataIdentityEnums.sd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + DataIdentityEnums.sd.getName());

                        String sdA = DataCountUtil.formatValueWithBigDecimal(sdDigits, absData.getSd());
                        String sdT = DataCountUtil.formatValueWithBigDecimal(sdDigits, tPercentData.getSd());

                        if (isNumber(sdA)) {
                            children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), DataCountUtil.addZero(Double.valueOf(sdA), sdDigits));
                        } else {
                            children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), sdA);
                        }
                        if (isNumber(sdT)) {
                            children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), DataCountUtil.addZero(Double.valueOf(sdT), sdDigits));
                        } else {
                            children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), sdT);
                        }

                        ans.putIfAbsent(pre + "-" + DataIdentityEnums.sd.getIdentity(), children);
                    }
                    if (DataIdentityEnums.rsd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + DataIdentityEnums.rsd.getName());

                        String rsdA = DataCountUtil.formatValueWithBigDecimal(sdDigits, absData.getRsd());
                        String rsdT = DataCountUtil.formatValueWithBigDecimal(sdDigits, tPercentData.getRsd());

                        if (isNumber(rsdA)) {
                            children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), DataCountUtil.addZero(Double.valueOf(rsdA), sdDigits));
                        } else {
                            children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), rsdA);
                        }

                        if (isNumber(rsdT)) {
                            children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), DataCountUtil.addZero(Double.valueOf(rsdT), sdDigits));
                        } else {
                            children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), rsdT);
                        }
                        ans.putIfAbsent(pre + "-" + DataIdentityEnums.rsd.getIdentity(), children);
                    }
                }
            }
        } else {
            if (!option.isEmpty()) {
                for (String s : option) {
                    Map<String, String> children = new HashMap<>();
                    if (DataIdentityEnums.avg.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + DataIdentityEnums.avg.getName());
                        children.put(mapName, originData.get(mapName));
                        ans.putIfAbsent(pre + "-" + DataIdentityEnums.avg.getIdentity(), children);
                    }
                    if (DataIdentityEnums.sd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + DataIdentityEnums.sd.getName());
                        RepeatMeasureData measureData = wlData.getOtherData().get(mapName);

                        String sdA = DataCountUtil.formatValueWithBigDecimal(sdDigits, measureData.getRsd());
                        if (isNumber(sdA)) {
                            children.put(mapName, DataCountUtil.addZero(Double.valueOf(sdA), sdDigits));
                        } else {
                            children.put(mapName, sdA);
                        }

                        children.put(mapName, measureData.getSd());
                        ans.putIfAbsent(pre + "-" + DataIdentityEnums.sd.getIdentity(), children);
                    }
                    if (DataIdentityEnums.rsd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + DataIdentityEnums.rsd.getName());
                        RepeatMeasureData measureData = wlData.getOtherData().get(mapName);

                        String rsdA = DataCountUtil.formatValueWithBigDecimal(sdDigits, measureData.getRsd());
                        if (isNumber(rsdA)) {
                            children.put(mapName, DataCountUtil.addZero(Double.valueOf(rsdA), sdDigits));
                        } else {
                            children.put(mapName, rsdA);
                        }
                        children.put(mapName, measureData.getRsd());
                        ans.putIfAbsent(pre + "-" + DataIdentityEnums.rsd.getIdentity(), children);
                    }
                }
            }
        }

        return ans;
    }

    private Boolean isNumber(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Double v = Double.parseDouble(str);
            return !v.isNaN() && !v.isInfinite();
        } catch (NumberFormatException e) {
            return false;
        }
    }


    @Override
    public R<String> savePhotometricMeasureData(PhotometricSampleAndDataVO sampleAndDataVO) {

        String taskId = sampleAndDataVO.getTaskId();
        String sampleId = sampleAndDataVO.getSampleId();

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        task.setTaskStatus(TaskStatusEnum.TESTED.getCode());
        tMeasureTaskService.updateById(task);

        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getSampleKey, sampleId)
                .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
        TMeasureData tMeasureData = tMeasureDataService.getOne(queryWrapper);

        if (tMeasureData == null) {
            tMeasureData = new TMeasureData();
            tMeasureData.setSerialNumber(0);
        }

        if (StringUtils.hasText(tMeasureData.getOriginalCalculationData()) || StringUtils.hasText(tMeasureData.getInstrumentData())) {
            tMeasureData = new TMeasureData();
            tMeasureData.setSerialNumber(tMeasureData.getSerialNumber() + 1);
        }
        tMeasureData.setTaskId(taskId);
        tMeasureData.setSampleKey(sampleId);
        tMeasureData.setInstrumentData(JSON.toJSONString(sampleAndDataVO.getMachineData()));
        tMeasureData.setOriginalCalculationData(JSON.toJSONString(sampleAndDataVO.getWavelengthData()));
        tMeasureData.setMeasurementStartTime(sampleAndDataVO.getStartTime());
        tMeasureData.setMeasurementEndTime(sampleAndDataVO.getEndTime());
        tMeasureData.setSamplingTime(sampleAndDataVO.getSampleTime());

        tMeasureData.setIsDelete(createType);
        tMeasureDataService.saveOrUpdate(tMeasureData);

        return R.ok(tMeasureData.getId());
    }

    @Override
    public R<Boolean> saveCountData(PhotometricSampleAndDataVO countData, String dataId) {

        if (countData.getCountData() == null || countData.getCountData().isEmpty()) {
            //没有要保存的数据
            return R.fail(YamlMessageSource.getMessageStatic("00302003"));
        }
        String taskId = countData.getTaskId();
        String sampleId = countData.getSampleId();

        LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(TMeasureAnalysisData::getDataVersion).last("limit 1");
        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (analysisData != null) {
            if (StringUtils.hasText(analysisData.getMeasureDataId())) {
                analysisData.setId(null);
                analysisData.setDataVersion(analysisData.getDataVersion() + 1);
            }
            analysisData.setMeasureDataId(dataId);

            analysisData.setAnalysisResult(JSON.toJSONString(countData.getCountData()));
        } else {
            analysisData = new TMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setAnalysisResult(JSON.toJSONString(countData.getCountData()));
            analysisData.setDataVersion(0);
            analysisData.setMeasureDataId(dataId);
        }
        analysisData.setIsDelete(createType);
        tMeasureAnalysisDataService.saveOrUpdate(analysisData);
        return R.ok(true);

    }

    @Override
    public Result saveDataInfo(SaveDataVO vo) {
        if (!StringUtils.hasText(vo.getTaskId())) {
            //请创建任务
            return Result.fail(YamlMessageSource.getMessageStatic("00100016"));
        }

        if (!StringUtils.hasText(vo.getSampleId())) {
            //缺少样品id
            return Result.fail(YamlMessageSource.getMessageStatic("0000001C"));
        }

        if (vo.getData() == null || vo.getData().isEmpty()) {
            return Result.ok();
        }
        String taskId = vo.getTaskId();
        String sampleId = vo.getSampleId();

        LambdaQueryWrapper<TMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(TMeasureAnalysisData::getDataVersion).last("limit 1");

        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(queryWrapper);
        if (analysisData != null) {
            Map<String, String> jsonObject;
            if (StringUtils.hasText(analysisData.getAnalysisParam())) {
                jsonObject = JSON.parseObject(analysisData.getAnalysisParam(), new TypeReference<Map<String, String>>(){});
            } else {
                jsonObject = vo.getData();
            }
            analysisData.setAnalysisParam(JSON.toJSONString(jsonObject));
            tMeasureAnalysisDataService.updateById(analysisData);
        } else {
            analysisData = new TMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setAnalysisParam(JSON.toJSONString(vo.getData()));
            analysisData.setDataVersion(0);
            analysisData.setIsDelete(createType);
            tMeasureAnalysisDataService.save(analysisData);
        }

        return Result.ok(true);
    }

    /**
     * 零点校零
     * @param deviceSerialNumber
     * @param methodId
     * @return
     */
    @Override
    public Result zero(String deviceSerialNumber, String methodId, String taskId,String updateBy) {

        if (!StringUtils.hasText(deviceSerialNumber)) {
            //未连接仪器
            return Result.fail(YamlMessageSource.getMessageStatic("0000001D"));
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }

        if (!StringUtils.hasText(taskId)){
            //创建任务

            //创建任务
            TMeasureTask newTask = new TMeasureTask();

            SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
            newTask.setUserId(po.getId());
            newTask.setProjectItemKey(method.getProjectItemKey());
            newTask.setMethodKey(method.getId());
            newTask.setMeasureType(method.getMeasurementType());
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            String collect = new Random().ints(8, 0, chars.length())
                    .mapToObj(i -> String.valueOf(chars.charAt(i)))
                    .collect(Collectors.joining());

            newTask.setTaskName(method.getMeasurementType() + "task_" + collect);
            newTask.setCreateTime(LocalDateTime.now());
            boolean save = false;
            for (int i = 0; i < 3; i++) {
                if (tMeasureTaskService.save(newTask)) {
                    save = true;
                    break;
                }
            }
            if (!save) {
                //系统出错
                return Result.fail(YamlMessageSource.getMessageStatic("00000014"));
            }
            taskId = newTask.getId();
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        PhotometricParameter photometricParameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
        if (photometricParameter.getWaveLength() == null || photometricParameter.getWaveLength().isEmpty()) {
            //缺少波长
            return Result.fail(YamlMessageSource.getMessageStatic("0000001E"));
        }
        List<String> waveLength = photometricParameter.getWaveLength();
        // 通过序列号获取ip

        String wave = "--";

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());
        SetParam param = new SetParam();

        param.setPhotoMode(Integer.valueOf(photometricParameter.getLuminosity()));
        param.setResponseTime(Double.valueOf(photometricParameter.getResponseTime()));
        param.setChangeLampWl(Double.parseDouble(photometricParameter.getChangeLampWaveLength()));

        if (photometricParameter.getGain() != null) {
            param.setGain(photometricParameter.getGain());
        }
        if (StringUtils.hasText(photometricParameter.getEnergyLamp())) {
            param.setLampPos(Integer.valueOf(photometricParameter.getEnergyLamp()));
        }

        if (deviceInfoService.isNeedSetSit(deviceSerialNumber)) {
            param.setSlit1(Double.valueOf(photometricParameter.getSpectralBandwidth()));
        }

        double startWl = Double.MIN_VALUE;

        for (String s : waveLength) {
            double v = Double.parseDouble(s);
            if (startWl < v) {
                startWl = v;
            }
        }
        if (startWl != Double.MIN_VALUE) {
            param.setStartWl(startWl);
        }

        inputParam.setParam(param);

        R<OperationResultEntity> operation = devOperationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return Result.fail(operation.getMessage());
        }

        inputParam.setOperationType(OperationTypeEnums.Zero.getType());
        inputParam.setWavelength(waveLength);
        R<OperationResultEntity> zero = devOperationUtilService.operation(inputParam);
        if (!zero.getSuccess()) {
            return Result.fail(zero.getMessage());
        }
        OperationResultEntity data = zero.getData();
        List<ChangeParam> zeroResult = data.getZeroResult();

        task.setZeroParam(JSON.toJSONString(zeroResult));
        tMeasureTaskService.updateById(task);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("waveLength", wave + "nm");
        return Result.ok(jsonObject);
//        String ip = deviceInfoService.getIp(deviceSerialNumber);
//        List<ChangeParam> list = new ArrayList<>();
//        for (String s : waveLength) {
//
//            double v = Double.parseDouble(s);
//            Command command = new Command(SetParamEnum.SetWl.getComId(), new Object[]{v});
//            devMessageUtil.wavelengthLocate(command, ip);
//
//            command = new Command(SetParamEnum.Zero.getComId(), new Object[]{});
//            R<ChangeParam> zero = null;
//            try {
//                zero = devMessageUtil.zero(command, ip);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//            if (!zero.getSuccess()) {
//                return Result.fail(zero.getMessage());
//            }
//            ChangeParam changeParam = zero.getData();
//            changeParam.setWl(s);
//            list.add(changeParam);
//        }
//        task.setZeroParam(JSON.toJSONString(list));
//
//        boolean update = tMeasureTaskService.updateById(task);
//        return update ? Result.ok() : Result.fail();
    }

    @Override
    public Result<Boolean> saveButton(String taskId, String updateBy) {
        if (!StringUtils.hasText(taskId)) {
            //未创建任务，无法保存
            return Result.fail(YamlMessageSource.getMessageStatic("0000001F"));
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
        String id = po.getId();
        task.setUserId(id);
        task.setIsDelete("1");
        tMeasureTaskService.updateById(task);
        return Result.ok(true);
    }

    @Override
    public Result<MeasurementCurrentInfoVO> getInfoByTaskId(String taskId) {
        if (!StringUtils.hasText(taskId)){
            //缺少任务id
            return Result.fail(YamlMessageSource.getMessageStatic("00000019"));
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        MeasurementCurrentInfoVO measurementCurrentInfoVO = new MeasurementCurrentInfoVO();
        measurementCurrentInfoVO.setTaskId(taskId);
        measurementCurrentInfoVO.setTaskName(task.getTaskName());
        measurementCurrentInfoVO.setMethodId(task.getMethodKey());
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        measurementCurrentInfoVO.setMethodName(method.getMethodName());
        measurementCurrentInfoVO.setProjectId(task.getProjectItemKey());
        TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
        measurementCurrentInfoVO.setProjectName(project.getProjectName());

        return Result.ok(measurementCurrentInfoVO);
    }

    @Override
    public R<File> downloadFileWithChildren(String taskId, Boolean allDisplay, String fileType, Integer digit, Integer otherDigit,List<String> option) {
        log.info("开始导出（集成children和option），taskId: {}, fileType: {}, option: {}", taskId, fileType, option);
        if (!StringUtils.hasText(taskId)) {
            return R.fail(YamlMessageSource.getMessageStatic("00000019"));
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            log.error("导出失败：任务不存在，taskId: {}", taskId);
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        if (!StringUtils.hasText(fileType)) {
            fileType = "word";
        } else {
            fileType = fileType.toLowerCase();
        }
        if (!"word".equals(fileType) && !"txt".equals(fileType) && !"xlsx".equals(fileType)) {
            return R.fail(YamlMessageSource.getMessageStatic("00300001"));
        }
        if (allDisplay == null) {
            allDisplay = true;
        }

        String methodId = task.getMethodKey();
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return R.fail(YamlMessageSource.getMessageStatic("00300009"));
        }

        List<HeadVO> heads = new ArrayList<>();
        List<HeadVO> vos = tableHead(taskId, methodId, allDisplay, task.getDeviceSerialNumber()).getData();
        heads.addAll(vos);
        heads.add(0, new HeadVO("序号", "serialNumber"));

        QueryDataDTO queryDataDTO = new QueryDataDTO();
        queryDataDTO.setOption(option != null ? option : new ArrayList<>());
        queryDataDTO.setTaskId(taskId);
        queryDataDTO.setAllDisplay(allDisplay);
        queryDataDTO.setDigit(digit);
        queryDataDTO.setOtherDigit(otherDigit);
        List<Map<String, Object>> data = queryData(queryDataDTO).getData();

        List<Map<String, Object>> expandedData = new ArrayList<>();
        if (data != null && !data.isEmpty()) {
            for (Map<String, Object> item : data) {
                expandedData.add(item);
                Object childrenObj = item.get("children");
                if (childrenObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) childrenObj;
                    for (Map<String, Object> child : children) {
                        Map<String, Object> childRow = new HashMap<>(child);
                        childRow.put("isChildData", true);
                        expandedData.add(childRow);
                    }
                }
            }
        }

        if (expandedData.isEmpty()) {
            log.warn("无实际数据，构造空行导出，taskId: {}", taskId);
            Map<String, Object> emptyRow = new HashMap<>();
            emptyRow.put("serialNumber", 1);
            for (HeadVO head : vos) {
                if (head != null && StringUtils.hasText(head.getIdentity())) {
                    emptyRow.put(head.getIdentity(), "");
                }
            }
            expandedData.add(emptyRow);
        } else {
            for (int i = 0; i < expandedData.size(); i++) {
                expandedData.get(i).put("serialNumber", i + 1);
            }
        }

        String fileName = task.getTaskName() + "." + getFileSuffix(fileType);
        File file = new File(fileName);
        try {
            switch (fileType) {
                case "word":
                    generateWordFile(file, task, method, heads, expandedData, vos.size());
                    break;
                case "txt":
                    generateTxtFile(file, heads, expandedData);
                    break;
                case "xlsx":
                    generateExcelFile(file, heads, expandedData);
                    break;
                default:
                    break;
            }
            return R.ok(file);
        } catch (Exception e) {
            log.error("文件生成失败", e);
            if (file.exists()) {
                file.delete();
            }
            return R.fail(YamlMessageSource.getMessageStatic("00302006", e.getMessage()));
        }
    }

    /**
     * 多格式文件导出（支持word/txt/xlsx）
     * @param taskId 任务ID
     * @param allDisplay 是否显示全部数据
     * @param fileType 导出格式（word/txt/xlsx）
     * @return 生成的文件对象
     */
    public R<File> downloadFile(String taskId, Boolean allDisplay, String fileType) {
        log.info("开始导出，taskId: {}, fileType: {}", taskId, fileType);
        // 1. 基础参数校验（复用原逻辑）
        if (!StringUtils.hasText(taskId)) {
            //缺少任务id
            return R.fail(YamlMessageSource.getMessageStatic("00000019"));
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            log.error("导出失败：任务不存在，taskId: {}", taskId);
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        // 处理fileType默认值和合法性
        if (!StringUtils.hasText(fileType)) {
            fileType = "word"; // 默认导出word
        } else {
            fileType = fileType.toLowerCase();
        }
        if (!"word".equals(fileType) && !"txt".equals(fileType) && !"xlsx".equals(fileType)) {
            //不支持的导出格式，仅支持word/txt/xlsx
            return R.fail(YamlMessageSource.getMessageStatic("00300001"));
        }
        // 处理allDisplay默认值
        if (allDisplay == null) {
            allDisplay = true;
        }

        // 2. 准备表头和数据（完全复用原downloadWord逻辑）
        String methodId = task.getMethodKey();
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            //测量方法不存在
            return R.fail(YamlMessageSource.getMessageStatic("00300009"));
        }

        // 构建表头（与原逻辑一致）
        List<HeadVO> heads = new ArrayList<>();
        // heads.add(new HeadVO("样品编号", "sampleNumber"));
        // heads.add(new HeadVO("样品名称", "sampleName"));
        // heads.add(new HeadVO("样品类型", "sampleType"));
        // heads.add(new HeadVO("稀释比", "dilutionRatio"));
        // heads.add(new HeadVO("样品体积", "sampleVolume"));
        // heads.add(new HeadVO("样品位置", "sampleLocation"));
        // 添加动态表头
        List<HeadVO> vos = tableHead(taskId, methodId, allDisplay, task.getDeviceSerialNumber()).getData();
        heads.addAll(vos);
        // 添加固定补充表头
        // heads.add(new HeadVO("备注", "remark"));
        // heads.add(new HeadVO("采样时间", "samplingTime"));
        // // 最前面加序号列（所有格式统一需要）
        heads.add(0, new HeadVO("序号", "serialNumber"));

        // 查询数据（复用原逻辑）
        QueryDataDTO queryDataDTO = new QueryDataDTO();
        queryDataDTO.setOption(new ArrayList<>());
        queryDataDTO.setTaskId(taskId);
        queryDataDTO.setAllDisplay(allDisplay);
        List<Map<String, Object>> data = queryData(queryDataDTO).getData();
        if (data == null || data.isEmpty()) {
            //没有可导出的数据
            return R.fail(YamlMessageSource.getMessageStatic("00302005"));
        }
        // 给数据加序号（与序号列对应）
        for (int i = 0; i < data.size(); i++) {
            data.get(i).put("serialNumber", i + 1);
        }

        // 3. 按格式生成文件（核心分支）
        String fileName = task.getTaskName() + "." + getFileSuffix(fileType);
        File file = new File(fileName);
        try {
            switch (fileType) {
                case "word":
                    generateWordFile(file, task, method, heads, data, vos.size());
                    break;
                case "txt":
                    generateTxtFile(file, heads, data);
                    break;
                case "xlsx":
                    generateExcelFile(file, heads, data);
                    break;
            }
            return R.ok(file);
        } catch (Exception e) {
            log.error("文件生成失败", e);
            // 异常时删除无效文件
            if (file.exists()) {
                file.delete();
            }
            //"文件生成失败：" + e.getMessage()
            return R.fail(YamlMessageSource.getMessageStatic("00302006",e.getMessage()));
        }
    }

    // ---------------------- 原有downloadWord方法（可保留或删除，建议用上面的downloadFile替代） ----------------------
    public R<File> downloadWord(String taskId, Boolean allDisplay) {
        // 直接调用多格式方法，指定格式为word
        return downloadFile(taskId, allDisplay, "word");
    }

    // ---------------------- 新增：三格式文件生成具体实现 ----------------------
    /**
     * 生成Word文件（修复原逻辑的表格列错位问题）
     */
    private void generateWordFile(File file, TMeasureTask task, TMeasureMethod method,
                                 List<HeadVO> heads, List<Map<String, Object>> data, int vosSize) throws Exception {
        XWPFDocument doc = new XWPFDocument();
        CTDocument1 document = doc.getDocument();
        CTBody body = document.getBody();

        // 页面设置（复用原逻辑）
        if (!body.isSetSectPr()) {
            body.addNewSectPr();
        }
        CTSectPr section = body.getSectPr();
        if (!section.isSetPgSz()) {
            section.addNewPgSz();
        }
        CTPageSz pageSize = section.getPgSz();
        if (vosSize > 5) {
            pageSize.setW(BigInteger.valueOf(15840));
            pageSize.setH(BigInteger.valueOf(12240));
        }

        // 添加标题（复用原逻辑）
        XWPFParagraph paragraph = doc.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText("任务：" + task.getTaskName() + "\r");
        run.setText("方法：" + method.getMethodName() + "\r");
        run.setText("测试数据：");

        // 修复表格创建逻辑：直接创建1行N列（N=表头数量），避免列错位
        int columnCount = heads.size();
        XWPFTable table = doc.createTable(1, columnCount);
        table.setWidth("100%");

        // 填充表头（修复原逻辑的表头赋值问题）
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < columnCount; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            cell.setText(heads.get(i).getName());
        }

        // 填充数据行（修复原逻辑的索引越界问题）
        int rowIndex = 0;
        for (Map<String, Object> map : data) {
            XWPFTableRow row = table.createRow();
            rowIndex++;
            // 第0列：序号
            row.getCell(0).setText(String.valueOf(rowIndex));

            // 填充其他列（与表头顺序对应）
            for (int i = 1; i < columnCount; i++) {
                HeadVO head = heads.get(i);
                String identity = head.getIdentity();
                
                // 1. 获取原始值（不做默认值替换）
                Object originalValue = map.get(identity);
                
                // 2. 处理空值：null或空字符串用'-'代替，其他值保持原始
                String cellData;
                if (originalValue == null) {
                    // 原始值为null时用'-'
                    cellData = "-";
                } else {
                    // 转换为字符串
                    String originalStr = originalValue.toString();
                    // 空字符串或"null"用'-'，其他值保持原始
                    cellData = originalStr.trim().isEmpty() || "null".equals(originalStr) ? "-" : originalStr;
                }

                // 3. 写入单元格
                row.getCell(i).setText(cellData);
            }
        }

        // 表格样式（复用原逻辑）
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                XWPFParagraph paragraph1 = cell.getParagraphArray(0);
                if (paragraph1 == null) { // 若段落为null，通过单元格的段落列表获取（POI正确用法）
                    List<XWPFParagraph> paras = cell.getParagraphs();
                    if (paras == null || paras.isEmpty()) {
                        paragraph1 = cell.addParagraph(); // 无段落时新增
                    } else {
                        paragraph1 = paras.get(0);
                    }
                }
                paragraph1.setAlignment(ParagraphAlignment.CENTER);
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }

        // 写入文件（修复原逻辑的流关闭问题）
        if (file == null) {
            throw new IllegalArgumentException("文件对象为null，无法写入Word");
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            doc.write(out);
        } finally {
            doc.close();
        }
    }

    /**
     * 生成TXT文件（UTF-8编码，制表符分隔）
     */
    private void generateTxtFile(File file, List<HeadVO> heads, List<Map<String, Object>> data) throws Exception {
        // 使用BufferedWriter保证效率，UTF-8避免中文乱码
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            // 1. 写入表头（制表符分隔，处理空表头）
            String headerLine = heads.stream()
                    .map(head -> StringUtils.hasText(head.getName()) ? head.getName() : "未命名列") // 表头空值保护
                    .collect(Collectors.joining("\t"));
            writer.write(headerLine);
            writer.newLine(); // 换行

            // 2. 写入数据（与表头顺序一致，仅空值用"-"，其他保留原始值）
            for (Map<String, Object> map : data) {
                List<String> rowData = new ArrayList<>();
                for (HeadVO head : heads) {
                    String identity = head.getIdentity();
                    // 1. 获取原始值（不默认替换为空字符串，保留null）
                    Object originalValue = map.get(identity);
                    String cellData;

                    // 2. 空值处理：null/空字符串/"null" 统一替换为"-"
                    if (originalValue == null) {
                        cellData = "-";
                    } else {
                        // 原始值转字符串（不做任何额外格式化）
                        String originalStr = originalValue.toString();
                        // 处理空字符串或"null"（不区分大小写）
                        cellData = originalStr.trim().isEmpty() || "null".equalsIgnoreCase(originalStr) 
                                ? "-" 
                                : originalStr;
                    }

                    // 3. 清除TXT列分裂风险的特殊字符（制表符/换行符，不修改原始内容）
                    cellData = cellData.replace("\t", " ").replace("\r", "").replace("\n", "");
                    rowData.add(cellData);
                }
                // 制表符分隔列，写入一行
                writer.write(String.join("\t", rowData));
                writer.newLine();
            }
        }
    }

    /**
     * 简化版：生成 XLSX 文件（仅导出数据，无复杂样式，空值用"-"）
     */
    private void generateExcelFile(File file, List<HeadVO> heads, List<Map<String, Object>> data) throws Exception {
        // 1. 转换表头（处理空表头，避免显示异常）
        List<List<String>> excelHeads = new ArrayList<>();
        for (HeadVO head : heads) {
            List<String> singleHead = new ArrayList<>();
            // 表头空值时显示“未命名列”，避免Excel表头为空
            singleHead.add(StringUtils.hasText(head.getName()) ? head.getName() : "未命名列");
            excelHeads.add(singleHead);
        }

        // 2. 转换数据（核心：保留原始值，仅空值用"-"）
        List<List<Object>> excelData = new ArrayList<>();
        for (Map<String, Object> map : data) {
            List<Object> rowData = new ArrayList<>();
            for (HeadVO head : heads) {
                String identity = head.getIdentity();
                // 1. 获取原始值（不预设默认值，完全保留数据源状态）
                Object originalValue = map.get(identity);
                Object cellData;

                // 核心逻辑：空值处理（null/空字符串/"null" 统一替换为"-"，其他保留原始值）
                if (originalValue == null) {
                    cellData = "-";
                } else {
                    // 如果是数字字符串，尝试转换为 Double，让 Excel 识别为数值类型
                    cellData = convertToNumberIfNeeded(originalValue.toString());
                    // 清除 Excel 解析异常的特殊字符（换行/制表符，不修改原始业务内容）
                    if (cellData instanceof String) {
                        cellData = ((String) cellData).replace("\r", "").replace("\n", "").replace("\t", " ");
                        // 处理空字符串或"null"（不区分大小写）
                        if (((String) cellData).trim().isEmpty() || "null".equalsIgnoreCase((String) cellData)) {
                            cellData = "-";
                        }
                    }
                }

                rowData.add(cellData);
            }
            excelData.add(rowData);
        }

        // 3. 写入Excel（try-with-resources自动关闭流，避免资源泄漏）
        try (OutputStream out = new FileOutputStream(file)) {
            EasyExcel.write(out)
                    .head(excelHeads)
                    .sheet("测量数据") // Excel工作表名称
                    .doWrite(excelData);
        }
    }
    /**
     * 尝试将字符串转换为数值类型
     * @param value 待转换的字符串
     * @return 如果是有效数字则返回 Double，否则返回原字符串
     */
    private Object convertToNumberIfNeeded(String value) {
        if (value == null || value.trim().isEmpty() || "-".equals(value)) {
            return value;
        }

        try {
            // 尝试去除空格
            String trimmed = value.trim();
            // 尝试转换为 Double
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            // 转换失败，返回原字符串
            return value;
        }
    }
    // ---------------------- 工具方法 ----------------------
    /**
     * 获取文件后缀（Java 8兼容）
     */
    private String getFileSuffix(String fileType) {
        if ("word".equals(fileType)) {
            return "docx";
        } else if ("txt".equals(fileType)) {
            return "txt";
        } else if ("xlsx".equals(fileType)) {
            return "xlsx";
        } else {
            return "docx";
        }
    }

    @Override
    public R<List<InputParam>> queryInputParam(String methodId) {
        if (!StringUtils.hasText(methodId)) {
            //未选择方法
            return R.fail(YamlMessageSource.getMessageStatic("00000008"));
        }
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return R.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        PhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
        List<InputParam> inputParams = parameter.getSampleInputParams();

        return R.ok(inputParams);
    }

    @Override
    public R<List<InputParamMapTableHead>> queryInputParamMapTableHead(String taskId) {

        if (!StringUtils.hasText(taskId)) {
            //未选择任务
            return R.fail(YamlMessageSource.getMessageStatic("0000001B"));
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        List<InputParamMapTableHead> ans = new ArrayList<>();
        if (task.getTableInfo() != null) {
            TaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
            ans = snapshot.getMapRelation();
        }
        return R.ok(ans);

    }

    @Override
    public R<Boolean> saveInputParamMapTableHead(TaskHeadSnapshotDTO snapshotDTO) {

        if (!StringUtils.hasText(snapshotDTO.getTaskId())) {
            //未选择任务
            return R.fail(YamlMessageSource.getMessageStatic("0000001B"));
        }

        TMeasureTask task = tMeasureTaskService.getById(snapshotDTO.getTaskId());
        if (task == null) {
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        TaskHeadSnapshot taskHeadSnapshot = new TaskHeadSnapshot();
        List<HeadVO> taskHeads = new ArrayList<>();
        List<HeadVO> sampleHeads = new ArrayList<>();
        for (HeadVO head : snapshotDTO.getTaskHeads()) {
            if (head.getLocate().equals(TableHeadILocateEnums.sample.getIdentity()) || head.getLocate().equals(TableHeadILocateEnums.system.getIdentity())) {
                sampleHeads.add(head);
            } else {
                taskHeads.add(head);
            }
        }
        BeanUtils.copyProperties(snapshotDTO, taskHeadSnapshot);
        taskHeadSnapshot.setTaskHeads(taskHeads);
        taskHeadSnapshot.setSampleHeads(sampleHeads);

        task.setTableInfo(JSON.toJSONString(taskHeadSnapshot));

        return tMeasureTaskService.updateById(task) ? R.ok(true) : R.fail(YamlMessageSource.getMessageStatic("00300008"));//保存失败
    }

    @Override
    public Result<InputParam> insertMethodParameter(InputParam methodParameter) {
        if(methodParameter.getParamName().length() >= 256){
            //名称格式错误
            return Result.fail(YamlMessageSource.getMessageStatic("00000020"));
        }
        String methodMark = methodParameter.getResultIdentity();
        String regex = "^[A-Z]\\d{1,99}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(methodMark);
        boolean res = matcher.matches();
        if (!res) {
            return Result.fail();
        }
        return Result.ok(methodParameter);
    }

    @Override
    public List<PhotometricCountDataInfo> dataRounding(List<PhotometricCountDataInfo> dataVO, PhotometricParameter parameter) {
        List<RoundingVO> approxParams = parameter.getApproxParams();
        if (approxParams == null || approxParams.isEmpty()) {
            log.info("没有设置修约参数");
            return dataVO;
        }
        List<PreCountResultParam> resultParams = parameter.getResultParams();
        if (resultParams == null || resultParams.isEmpty()) {
            log.info("没有设置待计算结果参数，不做修约处理");
            return dataVO;
        }

        if (dataVO == null || dataVO.isEmpty()) {
            log.info("没有待修约的数据");
            return dataVO;
        }
        Map<String, String> map = new HashMap<>();
        for (PreCountResultParam param:resultParams){
            map.put(param.getResultIdentity(), param.getResultIdentity() + "-" + param.getFormulaText());
        }
        Map<String, List<RoundingParamVO>> roundingInfo = new HashMap<>();
        for (RoundingVO param : approxParams){
            String identity = param.getResultIdentity();
            if (map.containsKey(identity)) {
                String key = map.get(identity);
                roundingInfo.put(key, param.getRoundingParams());
            }
        }
        for (PhotometricCountDataInfo count : dataVO){
            String resultIdentity = count.getResultIdentity() + "-" + count.getFormulaText();
            List<RoundingParamVO> list = roundingInfo.get(resultIdentity);
            if (roundingInfo.containsKey(resultIdentity) && list != null) {
                String s = roundData(count.getCountValue(), list);
                count.setCountValue(s);
            }
        }
        return dataVO;
    }

    @Override
    public R<Boolean> createSample(String taskId, String updateBy, String deviceSerialNumber) {
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        Integer sampleAutoNum = task.getSampleAutoNum();
        if (sampleAutoNum == null) {
            sampleAutoNum = 0;
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        if (taskSpecimen == null) {
            taskSpecimen = new ArrayList<>();
        }
        Result<List<HeadVO>> listResult=querySampleHeads(taskId);
        if (!listResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode())){
            return R.fail(listResult.getMessage());
        }
        List<HeadVO> heads = listResult.getData();
        List<HeadVO> dynamicHeads = new ArrayList<>();
        heads.forEach(head -> {
            if (head.getLocate().equals(TableHeadILocateEnums.sample.getIdentity())){
                dynamicHeads.add(head);
            }
        });

        Boolean isAutoChangePool = false;
        R<FSamplePoolInfoVO> poolInfo = tSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
        if (poolInfo.getSuccess()) {
            FSamplePoolInfoVO poolInfoVO = poolInfo.getData();
            isAutoChangePool = poolInfoVO.getIsAutoChangePool();
        }
        if (taskSpecimen.isEmpty()) {
            TMeasureSample newSample = new TMeasureSample();

            sampleAutoNum = 1;
            newSample.setSampleName("默认样品" + 1);
            newSample.setSampleNumber("default-" + 1);
            newSample.setSampleType(SampleTypeEnum.unKnown.getIdentity());
            if (!dynamicHeads.isEmpty()) {
                newSample.setHeads(JSON.toJSONString(dynamicHeads));
            }
            boolean save=tMeasureSampleService.save(newSample);
            if (!save) {
                //保存失败
                return R.fail(YamlMessageSource.getMessageStatic("00300008"));
            }
            taskSpecimen.add(new TaskSpecimen(newSample.getId(), createType, noTested, newSample.getSampleType(), null));
        }else {
            boolean hasNoTested = false;
            List<String> sampleNumList = new ArrayList<>();
            for (TaskSpecimen specimen : taskSpecimen){
                if (specimen.getDeleteType().equals(deleteType)) {
                    continue;
                }

                if (specimen.getIsTested().equals(noTested)) {
                    hasNoTested = true;
                    break;
                }

                TMeasureSample measureSample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                if (measureSample != null && StringUtils.hasText(measureSample.getSampleNumber())){
                    sampleNumList.add(measureSample.getSampleNumber());
                }
            }
            if (hasNoTested) {
                return R.ok(true);
            }
            TMeasureSample newSample = new TMeasureSample();
            sampleAutoNum = sampleAutoNum + 1;
            while (sampleNumList.contains("default-"+ sampleAutoNum)) {
                sampleAutoNum += 1;
            }

            int number = sampleAutoNum;

            newSample.setSampleNumber("default-"+ number);
            newSample.setSampleName("默认样品" + number);

            sampleNumList.add("default-" + number);
            List<Integer> usefulLocation=tSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
            int all=usefulLocation.size();

            newSample.setSampleType(SampleTypeEnum.unKnown.getIdentity());
            if (!dynamicHeads.isEmpty()) {
                newSample.setHeads(JSON.toJSONString(dynamicHeads));
            }
            boolean save = tMeasureSampleService.save(newSample);
            if (!save) {
                //保存失败
                return R.fail(YamlMessageSource.getMessageStatic("00300008"));
            }

            taskSpecimen.add(new TaskSpecimen(newSample.getId(), createType, noTested, newSample.getSampleType(), null));
            if (isAutoChangePool) {
                for (int i = 1; i < all; i++) {
                    newSample.setId(null);
                    while (sampleNumList.contains("default-"+ sampleAutoNum)) {
                        sampleAutoNum += 1;
                    }

                    int newNum = sampleAutoNum;
                    sampleNumList.add("default-" + newNum);
                    newSample.setSampleNumber("default-" + newNum);
                    newSample.setSampleName("默认样品" + newNum);

                    save = tMeasureSampleService.save(newSample);
                    if (!save) {
                        //保存失败
                        return R.fail(YamlMessageSource.getMessageStatic("00300008"));
                    }
                    taskSpecimen.add(new TaskSpecimen(newSample.getId(), createType, noTested, newSample.getSampleType(), null));
                }
            }
        }
        task.setSampleKey(JSON.toJSONString(taskSpecimen));
        task.setSampleAutoNum(sampleAutoNum);
        boolean update = tMeasureTaskService.updateById(task);
        if (!update) {
            //更新失败
            return R.fail(YamlMessageSource.getMessageStatic("00000048"));
        }
        if (isAutoChangePool) {
            List<Integer> usefulLocation =tSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
            publicUtilService.changeSampleLocation(task.getId(),usefulLocation);

        }
        return R.ok(true);
    }

    /**
     * 测量值质控
     */
    public Map<String, String> checkValueQuantity(PhotometricParameter parameter, PhotometricOriginDataInfo dataInfo, QualityConfEnums qcType) {

        Map<String, String> result = new HashMap<>();
        if (parameter == null || parameter.getQcParams() == null || parameter.getQcParams().isEmpty()) {
            log.info("无质控参数");
            return  result;
        }
        List<QualityConfVO> qcParams = parameter.getQcParams();
        String luminosity = parameter.getLuminosity();
        Map<String, String> data = dataInfo.getData();


        for (QualityConfVO param : qcParams) {
            String key = param.getWaveLength()+ "-"+ luminosity;

            String s = data.get(key);
            if (s == null) {
                continue;
            }
            double v = Double.parseDouble(s);
            if (param.getControlType().equals(qcType.getCode())) {

                if (param.getIsUpperLimitIncluded() != null && param.getIsUpperLimitIncluded()) {
                    if (v >= param.getUpperLimit()) {
                        result.put(key, QualityConfEnums.quality_high.getCode());
                    }
                } else {
                    if (v > param.getUpperLimit()) {
                        result.put(key, QualityConfEnums.quality_high.getCode());
                    }
                }

                if (param.getIsLowerLimitIncluded() != null && param.getIsLowerLimitIncluded()) {
                    if (v <= param.getLowerLimit()) {
                        result.put(key, QualityConfEnums.quality_low.getCode());

                    }

                } else {
                    if (v < param.getLowerLimit()) {
                        result.put(key, QualityConfEnums.quality_low.getCode());

                    }
                }
            }
        }

        return result;
    }


    private String roundData(String value, List<RoundingParamVO> roundingParams) {
        double middle = Double.parseDouble(value);
        String  ans = value;
        for (RoundingParamVO param : roundingParams){
            Double lowerLimit = param.getLowerLimit();

            Double upperLimit = param.getUpperLimit();

            // 检查下限
            boolean lowerCheck = true;
            if (lowerLimit != null) {
                if (param.getIsLowerLimitIncluded() != null && param.getIsLowerLimitIncluded()) {
                    // 包含下限
                    lowerCheck = middle >= lowerLimit;
                } else {
                    // 不包含下限
                    lowerCheck = middle > lowerLimit;
                }
            }

            // 检查上限
            boolean upperCheck = true;
            if (upperLimit != null) {
                if (param.getIsUpperLimitIncluded() != null && param.getIsUpperLimitIncluded()) {
                    // 包含上限
                    upperCheck = middle <= upperLimit;
                } else {
                    // 不包含上限
                    upperCheck = middle < upperLimit;
                }
            }

            if (lowerCheck && upperCheck) {
                String roundingType = param.getRoundingType();

                if (roundingType == null) {
                    return value;
                }

                if (roundingType.equals(RoundingTypeEnums.SIGNIFICANT_FIGURES.getCode())) {
                    ans = DataCountUtil.formatToSignificantDigits(param.getDecimalPlaces(), BigDecimal.valueOf(middle));
                } else {
                    ans = DataCountUtil.addZero(DataCountUtil.formatValue(param.getDecimalPlaces(), middle), param.getDecimalPlaces());
                }
                return ans;
            }
        }
        return value;
    }


}
