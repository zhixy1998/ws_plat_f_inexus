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
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPreCountResultParam;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskSpecimen;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskHeadSnapshot;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.standardService.TReportStandardDetectionService;
import com.persee.workstation.uv_inexus.service.guide.standardService.TReportStandardPretreatmentService;
import com.persee.workstation.uv_inexus.service.guide.parseReport.LaTeXConverter;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.DevOperationInputParam;
import com.persee.workstation.uv_inexus.util.deviceUtil.DevOperationUtilService;
import com.persee.workstation.uv_inexus.util.deviceUtil.OperationResultEntity;
import com.persee.workstation.uv_inexus.util.deviceUtil.OperationTypeEnums;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.util.pythonUtil.RestUtil;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.*;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.*;
import com.persee.workstation.uv_inexus.dto.guide.PhotometricMethodParamCheckDTO;
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
    SysUserMapper userManageDAO;
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
    TSamplePoolConfigService tSamplePoolConfigService;
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
    RestUtil restUtil;

    @Autowired
    LaTeXConverter latexConverter;

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
        if (parameter.getSampleInputParams() != null && parameter.getSampleInputParams().size() > 10) {
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
                if (!(includeUpper ^ includeLower)) {
                    return R.fail(YamlMessageSource.getMessageStatic("00100022"));
                }
            }
        }
        return R.ok(true);
    }


    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, PhotometricParameter photometricParameter) {

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
        if (!StringUtils.hasText(methodDto.getMethodId())) {
            //缺少方法
            return Result.fail(YamlMessageSource.getMessageStatic("00302001"));
        }
        if(methodDto.getMethodName().length() >= 256){
            //方法名格式错误
            return Result.fail(YamlMessageSource.getMessageStatic("00300002"));
        }
        // 根据方法id查询方法
        TMeasureMethod measureMethod = tMeasureMethodService.getById(methodDto.getMethodId());
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

    @Override
    public Result<MethodChangeVO> checkMethodParam(PhotometricMethodParamCheckDTO checkDTO) {
        if (java.util.Objects.isNull(checkDTO.getNewMethodParam()) && !StringUtils.hasText(checkDTO.getNewMethodId())) {
            return Result.fail(YamlMessageSource.getMessageStatic("00103014"));
        }
        if (!StringUtils.hasText(checkDTO.getNewMethodId())) {
            PhotometricParameter newMethodParam = checkDTO.getNewMethodParam();
            R<Boolean> r = checkParam(newMethodParam);
            if (!r.getSuccess()) {
                return Result.fail(r.getMessage());
            }
            PhotometricParameter paramToCheck = null;
            if (checkDTO.getNewMethodParam() != null) {
                paramToCheck = checkDTO.getNewMethodParam();
            } else {
                if (StringUtils.hasText(checkDTO.getNewMethodId())) {
                    TMeasureMethod newMethod = tMeasureMethodService.getById(checkDTO.getNewMethodId());
                    if (newMethod != null && StringUtils.hasText(newMethod.getMeasurementParamInfo())) {
                        paramToCheck = JSON.parseObject(newMethod.getMeasurementParamInfo(), PhotometricParameter.class);
                    }
                } else if (StringUtils.hasText(checkDTO.getCurrentMethodId())) {
                    TMeasureMethod currentMethod = tMeasureMethodService.getById(checkDTO.getCurrentMethodId());
                    if (currentMethod != null && StringUtils.hasText(currentMethod.getMeasurementParamInfo())) {
                        paramToCheck = JSON.parseObject(currentMethod.getMeasurementParamInfo(), PhotometricParameter.class);
                    }
                }
            }
            if (paramToCheck != null && paramToCheck.getResultParams() != null && !paramToCheck.getResultParams().isEmpty()) {
                java.util.List<InputParam> inputParams = paramToCheck.getSampleInputParams();
                java.util.List<PreCountResultParam> resultParams = paramToCheck.getResultParams();
                for (PreCountResultParam resultParam : resultParams) {
                    if (resultParam == null) {
                        continue;
                    }
                    Result<Boolean> checkResult = checkFormula(inputParams, resultParam, checkDTO.getMeasureType());
                    if (checkResult.getCode() != 200 || !java.lang.Boolean.TRUE.equals(checkResult.getData())) {
                        return Result.fail(checkResult.getMessage());
                    }
                }
            }
            double minWaveLength = 190.0;
            double maxWaveLength = 1100.0;
            if (StringUtils.hasText(checkDTO.getDeviceSerialNumber())) {
                R<DeviceMeasureVO> typeInfo = manageService.queryDeviceTypeInfo(checkDTO.getDeviceSerialNumber());
                if (typeInfo.getSuccess()) {
                    DeviceMeasureVO vo = typeInfo.getData();
                    if (vo != null) {
                        minWaveLength = StringUtils.hasText(vo.getMinWavelength()) ? Double.parseDouble(vo.getMinWavelength()) : minWaveLength;
                        maxWaveLength = StringUtils.hasText(vo.getMaxWavelength()) ? Double.parseDouble(vo.getMaxWavelength()) : maxWaveLength;
                    }
                }
            }
            for (String waveStr : newMethodParam.getWaveLength()) {
                if (waveStr == null || waveStr.trim().isEmpty()) {
                    return Result.fail(YamlMessageSource.getMessageStatic("00103006"));
                }
                try {
                    double waveLength = Double.parseDouble(waveStr);
                    if (waveLength < minWaveLength || waveLength > maxWaveLength) {
                        return Result.fail(YamlMessageSource.getMessageStatic("00105004", waveStr, minWaveLength, maxWaveLength));
                    }
                    String[] parts = waveStr.split("\\.");
                    if (parts.length > 1 && parts[1].length() > 1) {
                        return Result.fail(YamlMessageSource.getMessageStatic("00105005", waveStr));
                    }
                } catch (NumberFormatException e) {
                    return Result.fail(YamlMessageSource.getMessageStatic("00105006", waveStr));
                }
            }
        }
        MethodChangeVO vo = new MethodChangeVO();
        vo.setCurrentMethodId(checkDTO.getCurrentMethodId());
        vo.setParamChange(false);
        vo.setClearDataRange(PhotoMetricChangeMethodTypeEnum.noChange.getIdentity());
        vo.setMessage(PhotoMetricChangeMethodTypeEnum.noChange.getMessage());
        if (!StringUtils.hasText(checkDTO.getCurrentMethodId())) {
            log.info("生成新方法");
            vo.setParamChange(true);
            vo.setMessage(PhotoMetricChangeMethodTypeEnum.newMethod.getMessage());
            vo.setClearDataRange(PhotoMetricChangeMethodTypeEnum.newMethod.getIdentity());
            return Result.ok(vo);
        }
        if (!StringUtils.hasText(checkDTO.getCurrentTaskId())) {
            log.info("没有任务存在，将使用该方法生成数据");
            vo.setParamChange(true);
            vo.setClearDataRange(PhotoMetricChangeMethodTypeEnum.noData.getIdentity());
            return Result.ok(vo);
        }
        TMeasureMethod method = tMeasureMethodService.getById(checkDTO.getCurrentMethodId());
        PhotometricParameter oldParam = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
        PhotometricParameter methodParam;
        if (StringUtils.hasText(checkDTO.getNewMethodId())) {
            TMeasureMethod newMethod = tMeasureMethodService.getById(checkDTO.getNewMethodId());
            methodParam = JSON.parseObject(newMethod.getMeasurementParamInfo(), PhotometricParameter.class);
        } else {
            methodParam = checkDTO.getNewMethodParam();
        }
        String dbJson = JSON.toJSONString(oldParam, SerializerFeature.SortField);
        String frontJson = JSON.toJSONString(methodParam, SerializerFeature.SortField);
        if (dbJson.equals(frontJson)) {
            vo.setParamChange(false);
            vo.setClearDataRange(RationChangeMethodTypeEnums.noChange.getIdentity());
            vo.setMessage(RationChangeMethodTypeEnums.noChange.getMessage());
            return Result.ok(vo);
        }
        boolean b = checkMeasureParam(oldParam, methodParam);
        if (!b) {
            log.info("测量参数有改变，要清理所有数据");
            vo.setClearDataRange(PhotoMetricChangeMethodTypeEnum.measure.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(PhotoMetricChangeMethodTypeEnum.measure.getMessage());
            return Result.ok(vo);
        }
        boolean analyze = checkAnalyzeParam(oldParam, methodParam);
        if (!analyze) {
            log.info("分析参数有改变，要清理分析数据");
            vo.setClearDataRange(PhotoMetricChangeMethodTypeEnum.analyze.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(PhotoMetricChangeMethodTypeEnum.analyze.getMessage());
        }
        return Result.ok(vo);
    }

    private boolean checkMeasureParam(PhotometricParameter oldParam, PhotometricParameter newParam) {
        java.util.List<String> oldWave = oldParam.getWaveLength();
        java.util.List<String> newWave = newParam.getWaveLength();
        if (!java.util.Objects.equals(oldWave, newWave)) {
            return false;
        }
        if (!java.util.Objects.equals(oldParam.getLuminosity(), newParam.getLuminosity())) {
            return false;
        }
        if (!java.util.Objects.equals(oldParam.getEnergyLamp(), newParam.getEnergyLamp())) {
            return false;
        }
        if (!java.util.Objects.equals(oldParam.getGain(), newParam.getGain())) {
            return false;
        }
        return java.util.Objects.equals(oldParam.getSpectralBandwidth(), newParam.getSpectralBandwidth()) &&
                java.util.Objects.equals(oldParam.getResponseTime(), newParam.getResponseTime()) &&
                java.util.Objects.equals(oldParam.getChangeLampWaveLength(), newParam.getChangeLampWaveLength()) &&
                java.util.Objects.equals(oldParam.getRepetitionPattern(), newParam.getRepetitionPattern()) &&
                java.util.Objects.equals(oldParam.getRepetitionTime(), newParam.getRepetitionTime()) &&
                java.util.Objects.equals(oldParam.getTimeInterval(), newParam.getTimeInterval());
    }

    private boolean checkAnalyzeParam(PhotometricParameter oldParam, PhotometricParameter newParam) {
        return java.util.Objects.equals(oldParam.getSampleInputParams(), newParam.getSampleInputParams()) &&
                java.util.Objects.equals(oldParam.getQcParams(), newParam.getQcParams()) &&
                java.util.Objects.equals(oldParam.getResultParams(), newParam.getResultParams()) &&
                java.util.Objects.equals(oldParam.getApproxParams(), newParam.getApproxParams());
    }

    private Result<Boolean> checkFormula(java.util.List<InputParam> inputParamList, PreCountResultParam newFormula, String measureType) {
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

        SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
        String id = po.getId();

        LambdaQueryWrapper<TMeasureTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TMeasureTask::getUserId, id)
                .eq(TMeasureTask::getMeasureType, measureType)
                .eq(TMeasureTask::getIsDelete, createType)
                .orderByDesc(TMeasureTask::getCreateTime);

        List<TMeasureTask> taskList = tMeasureTaskService.list(wrapper);
        if (taskList != null && !taskList.isEmpty()) {
            for (TMeasureTask task : taskList) {
                String projectItemKey = task.getProjectItemKey();
                String methodKey = task.getMethodKey();
                TMeasureMethod method = tMeasureMethodService.getById(methodKey);
                if (method == null) {
                    tMeasureTaskService.removeById(task.getId());
                    continue;
                }
                TMeasureProject project = tMeasureProjectService.getById(projectItemKey);
                MeasurementCurrentInfoVO vo = new MeasurementCurrentInfoVO();
                vo.setTaskId(task.getId());
                vo.setTaskName(task.getTaskName());
                vo.setMethodId(method.getId());
                vo.setMethodName(method.getMethodName());
                vo.setProjectId(project.getId());
                vo.setProjectName(project.getProjectName());
                return Result.ok(vo);
            }
        }
        MeasurementCurrentInfoVO vo = new MeasurementCurrentInfoVO();

        LambdaQueryWrapper<TMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureMethod::getMethodCreator, updateBy)
                .eq(TMeasureMethod::getMeasurementType, measureType)
                .orderByDesc(TMeasureMethod::getMethodCreateTime);
        List<TMeasureMethod> list = tMeasureMethodService.list(queryWrapper);
        if (list == null || list.isEmpty()) {
            LambdaQueryWrapper<TMeasureProject> projectWrapper = new LambdaQueryWrapper<>();
            projectWrapper.eq(TMeasureProject::getCreatorKey, updateBy)
                    .orderByDesc(TMeasureProject::getCreateTime);
            List<TMeasureProject> projectList = tMeasureProjectService.list(projectWrapper);
            if (projectList == null || projectList.isEmpty()) {
                return Result.ok(vo);
            }
            queryWrapper.clear();
            queryWrapper.eq(TMeasureMethod::getProjectItemKey, projectList.get(0).getId())
                            .eq(TMeasureMethod::getMeasurementType,measureType)
                                    .last("limit 1");
            TMeasureMethod one = tMeasureMethodService.getOne(queryWrapper);
            vo.setMethodId(one.getId());
            vo.setMethodName(one.getMethodName());
            vo.setProjectName(projectList.get(0).getProjectName());
            vo.setProjectId(projectList.get(0).getId());

        } else {
            TMeasureMethod tMeasureMethod = list.get(0);
            TMeasureProject tMeasureProject = tMeasureProjectService.getById(tMeasureMethod.getProjectItemKey());

            vo.setMethodId(tMeasureMethod.getId());
            vo.setMethodName(tMeasureMethod.getMethodName());
            vo.setProjectId(tMeasureProject.getId());
            vo.setProjectName(tMeasureProject.getProjectName());
        }
        TMeasureTask task=new TMeasureTask();
        task.setUserId(id);

        task.setCreateTime(LocalDateTime.now());
        task.setStartTime(LocalDateTime.now());
        task.setMethodKey(vo.getMethodId());
        task.setMeasureType(measureType);
        task.setProjectItemKey(vo.getProjectId());
        task.setIsSave("0");
        task.setIsDelete("0");
        task.setTaskName("defaultName");
        tMeasureTaskService.save(task);
        vo.setTaskId(task.getId());
        vo.setTaskName(task.getTaskName());
        return Result.ok(vo);

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

        if (dto.getHeads() != null && dto.getHeads().size() > 10) {
            //表格新增列数不能超过10列
            return Result.fail(YamlMessageSource.getMessageStatic("00000013"));
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
    public Result<List<HeadVO>> tableHead(String taskId, String methodId, Boolean allDisplay) {
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

        List<HeadVO> vos = dealHeads(new ArrayList<>(ans), parameter, allDisplay);

        return Result.ok(vos);
    }

    private List<HeadVO> dealHeads(List<HeadVO> heads, PhotometricParameter parameter, Boolean allDisplay) {
        List<String> waveLength = parameter.getWaveLength();
        Map<String, HeadVO> map = heads.stream().collect(Collectors.toMap(HeadVO::getIdentity, head -> head));

        Map<String, List<HeadVO>> headsMap = new HashMap<>();

        Map<String, HeadVO> analyzeMap = new HashMap<>();

        for (HeadVO head : heads) {
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
                HeadVO newHead = new HeadVO(name, identity, TableHeadILocateEnums.measure.getIdentity(), name);
                if (map.containsKey(identity)) {
                    newHead = map.get(identity);
                }
                allMeasureHeads.add(newHead);
            }
        } else if (mode.equals(WaveLengthEnum.abs.getCode()) || mode.equals(WaveLengthEnum.tPercent.getCode())) {
            for (String s : waveLength) {
                String absIdentity = s + "-"+ WaveLengthEnum.abs.getCode();
                String tPercentIdentity = s + "-"+ WaveLengthEnum.tPercent.getCode();
                String absName = s + WaveLengthEnum.abs.getName();
                String tPercentName = s + WaveLengthEnum.tPercent.getName();

                HeadVO absHead = new HeadVO(absName, absIdentity, TableHeadILocateEnums.measure.getIdentity(), absName);
                HeadVO tPercentHead = new HeadVO(tPercentName, tPercentIdentity, TableHeadILocateEnums.measure.getIdentity(), tPercentName);

                if (map.containsKey(absIdentity)) {
                    absHead = map.get(absIdentity);
                }
                allMeasureHeads.add(absHead);

                if (allDisplay) {
                    if (map.containsKey(tPercentIdentity)) {
                        tPercentHead = map.get(tPercentIdentity);
                    }
                    allMeasureHeads.add(tPercentHead);
                }
            }
        }
        List<HeadVO> ans = new ArrayList<>();
        List<HeadVO> systemHeads = headsMap.get(TableHeadILocateEnums.system.getIdentity());
        if (systemHeads != null) ans.addAll(systemHeads);

        List<HeadVO> sampleHeads = headsMap.get(TableHeadILocateEnums.sample.getIdentity());
        if (sampleHeads != null) ans.addAll(sampleHeads);
        ans.addAll(allMeasureHeads);
        List<HeadVO> taskHeads = headsMap.get(TableHeadILocateEnums.data.getIdentity());
        if (taskHeads != null)ans.addAll(taskHeads);

        List<PreCountResultParam> resultParams = parameter.getResultParams();
        List<HeadVO> headAnalyze = new ArrayList<>();
        for (PreCountResultParam results : resultParams) {
            //TODO 方法参数标识待定，后续再改动
            if (!StringUtils.hasText(results.getParam())) {
                results.setParam(results.getResultIdentity() + "-" + results.getFormulaText());
            }
            String name = results.getResultName() + "(" + results.getUnit() + ")";
            HeadVO analyzeHead = new HeadVO(name, results.getParam(), TableHeadILocateEnums.analyze.getIdentity(), name);

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
            HeadVO remark = new HeadVO(SampleTableEnum.REMARK.getName(), SampleTableEnum.REMARK.getIdentity(), TableHeadILocateEnums.other.getIdentity(), SampleTableEnum.REMARK.getName());
            other.add(samplingTimeHead);
            other.add(remark);
        }

        ans.addAll(other);

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

        for (InputParam inputParam : parameter.getSampleInputParams()) {
            boolean b = snapshot.getMapRelation().stream().anyMatch(
                    mapRelation -> inputParam.getResultIdentity().equals(mapRelation.getParamIdentity()));

            Optional<InputParamMapTableHead> mapTableHead = snapshot.getMapRelation().stream().filter(
                    mapRelation -> inputParam.getResultIdentity().equals(mapRelation.getParamIdentity())).findFirst();
            if (!mapTableHead.isPresent()) {
                //"缺少参数映射：" + inputParam.getParamName()
                return R.fail(YamlMessageSource.getMessageStatic("00300005",inputParam.getParamName()));
            }
            identity.append(mapTableHead.get().getParamIdentity()).append(",");
            String s = allData.getString(mapTableHead.get().getHeadIdentity());
            if (!StringUtils.hasText(s)) {
                //缺少数据！
                return R.fail(YamlMessageSource.getMessageStatic("00300006"));
            }
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

        Integer digit = queryDataDTO.getDigit();
        if (digit == null || digit < 0) {
            digit = Constants.DEFAULT_DECIMAL_NUM;
            queryDataDTO.setDigit(digit);
        }

        R<TMeasureTaskAttachment> attachmentResult = tMeasureTaskAttachmentService.queryByTaskId(taskId);
        if (!attachmentResult.getSuccess()) {
            TMeasureTaskAttachment taskAttachment = new TMeasureTaskAttachment();
            taskAttachment.setTaskId(taskId);
            taskAttachment.setDataParam(JSON.toJSONString(queryDataDTO));
            tMeasureTaskAttachmentService.save(taskAttachment);
            attachmentResult = tMeasureTaskAttachmentService.queryByTaskId(taskId);
        }
        TMeasureTaskAttachment attachment = attachmentResult.getData();
        if (!StringUtils.hasText(attachment.getDataParam())) {
            attachment.setDataParam(JSON.toJSONString(queryDataDTO));
            tMeasureTaskAttachmentService.saveOrUpdate(attachment);
        }


        boolean repeat = false;
        PhotometricParameter parameter = null;
        if (StringUtils.hasText(task.getMethodKey())) {
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                parameter= JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
                if (StringUtils.hasText(parameter.getRepetitionPattern()) && parameter.getRepetitionPattern().equals("单池重复")) {
                    repeat = true;
                }
            }
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        for (TaskSpecimen specimen : taskSpecimen) {
            if (!allDisplay && specimen.getDeleteType().equals("1")) {
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

            //样品信息
            map.put(SampleTableEnum.SAMPLE_ID.getIdentity(), sample.getId());
            map.put(SampleTableEnum.SAMPLE_NAME.getIdentity(), sample.getSampleName());
            map.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber());
            map.put(SampleTableEnum.SAMPLE_TYPE.getIdentity(), SampleTypeEnum.getName(sample.getSampleType()));
            map.put(SampleTableEnum.SAMPLE_LOCATION.getIdentity(), "样品池"+ sample.getSampleLocation());


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
                                String formattedValue = DataCountUtil.formatValueWithBigDecimal(digit, entry.getValue());
                                formattedData.put(entry.getKey(), formattedValue);
                            }
                            map.putAll(formattedData);
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

                        PhotometricOriginDataInfo dataInfo = reCountData(wlDataList, digit);
                        for (PhotometricOriginDataInfo wlData : wlDataList) {

                            if (wlData.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                                wlDataList.remove(wlData);
                                BeanUtils.copyProperties(dataInfo, wlData);
                                wlDataList.add(dataInfo);
                                map.putAll(wlData.getData());
                                Map<String, Map<String, String>> ch = extracted(option, wlData, sample);
                                if (!ch.isEmpty()) {
                                    for (Map.Entry<String, Map<String, String>> entry : ch.entrySet()) {
                                        String key = entry.getKey();
                                        Map<String, String> value = entry.getValue();

                                        if (childrenList.containsKey(key)) {
                                            Map<String, String> existingMap = childrenList.get(key);
                                            existingMap.putAll(value);
                                        } else {
                                            childrenList.put(key, new HashMap<>(value));
                                        }
                                    }
                                }
                            } else {
                                Map<String, String> originData = wlData.getData();
                                Map<String, String> formattedData = new HashMap<>();
                                for (Map.Entry<String, String> entry : originData.entrySet()) {
                                    String formattedValue = DataCountUtil.formatValueWithBigDecimal(digit, entry.getValue());
                                    formattedData.put(entry.getKey(), formattedValue);
                                }

                                Map<String, String> children = new HashMap<>(formattedData);
                                String[] split = wlData.getIdentity().split("-");
                                String pre = StringUtils.hasText(sample.getSampleNumber()) ? sample.getSampleNumber() + "-" : "";
                                children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + split[split.length - 1]);
                                children.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), wlData.getMeasureTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                                childrenList.computeIfAbsent(wlData.getIdentity(), k -> new HashMap<>()).putAll(children);
                            }
                        }
                        map.put("samplingTime", data.getSamplingTime());

                        childrenList.forEach((key, value) -> middle.add(value));
                        map.put("children", middle);
                        Map<String, String> countData = getCountData(task, sample, parameter, digit, wlDataList);
                        map.putAll(countData);
                    }
                    map.put("remark", data.getRemark());
                }
            }
            ans.add(map);
        }

        return Result.ok(ans);
    }

    public Map<String,String> getCountData(TMeasureTask task, TMeasureSample sample, PhotometricParameter parameter, Integer digit, List<PhotometricOriginDataInfo> wlDataList) {

        Map<String, String> map = new HashMap<>();

        R<List<PhotometricCountDataInfo>> formula = formula(wlDataList, sample, task, parameter, digit);
        if (formula.getSuccess()) {
            List<PhotometricCountDataInfo> formulaData = formula.getData();
            for (PhotometricCountDataInfo count : formulaData) {
                String s = DataCountUtil.formatValueWithBigDecimal(digit, count.getCountValue());
                map.put(count.getResultIdentity() + "-" + count.getFormulaText(), s);
            }
        }


        LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, task.getId())
                .eq(TMeasureAnalysisData::getSampleKey, sample.getId())
                .orderByDesc(TMeasureAnalysisData::getAnalysisName).last("limit 1");

        TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (one != null) {
            List<PhotometricCountDataInfo> countList = JSON.parseArray(one.getAnalysisResult(), PhotometricCountDataInfo.class);

            if (map.isEmpty()) {
                for (PhotometricCountDataInfo count : countList) {

                    String s = DataCountUtil.formatValueWithBigDecimal(digit, count.getCountValue());
                    map.put(count.getResultIdentity() + "-" + count.getFormulaText(), s);

                }
            }

            if (StringUtils.hasText(one.getAnalysisParam())) {
                Map<String, String> jsonObject = JSON.parseObject(one.getAnalysisParam(), new TypeReference<Map<String, String>>(){});
                map.putAll(jsonObject);
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

    private PhotometricOriginDataInfo reCountData(List<PhotometricOriginDataInfo> wlDataList, Integer digit) {

        Map<String, List<Double>> data = new HashMap<>();
        Map<String, RepeatMeasureData> otherData = new HashMap<>();
        Map<String, String> ans = new HashMap<>();
        PhotometricOriginDataInfo result = new PhotometricOriginDataInfo();

        for (PhotometricOriginDataInfo wlData : wlDataList) {
            if (wlData.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                BeanUtils.copyProperties(wlData, result);
                continue;
            }

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
            repeatMeasureData.setSd(sd == null ? "未检出" :String.valueOf(sd));
            repeatMeasureData.setRsd(rsd == null ? "未检出" :String.valueOf(rsd));
            otherData.put(entry.getKey(), repeatMeasureData);
            ans.put(entry.getKey(), String.valueOf(mean));
        }
        result.setData(ans);
        result.setOtherData(otherData);
        return result;
    }

    private Map<String, Map<String, String>> extracted(List<String> option, PhotometricOriginDataInfo wlData,  TMeasureSample sample) {

        Map<String, String> originData = wlData.getData();
        Map<String, Map<String, String>> ans = new HashMap<>();
        String mapName = wlData.getWavelength() + "-" + wlData.getMode();

        RepeatMeasureData absData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode());
        RepeatMeasureData tPercentData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode());


        if (wlData.getMode().equals(WaveLengthEnum.abs.getCode()) || wlData.getMode().equals(WaveLengthEnum.tPercent.getCode())) {

            if (!option.isEmpty()) {
                for (String s : option) {
                    Map<String, String> children = new HashMap<>();
                    if (DataIdentityEnums.avg.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.avg.getName());

                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), originData.get(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode()));
                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), originData.get(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode()));
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.avg.getIdentity(), children);
                    }
                    if (DataIdentityEnums.sd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.sd.getName());

                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), absData.getSd());
                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tPercentData.getSd());
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.sd.getIdentity(), children);
                    }
                    if (DataIdentityEnums.rsd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getName());
                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), absData.getRsd());
                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tPercentData.getRsd());
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.rsd.getIdentity(), children);
                    }
                }
            }
        } else {
            if (!option.isEmpty()) {
                for (String s : option) {
                    Map<String, String> children = new HashMap<>();
                    if (DataIdentityEnums.avg.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.avg.getName());
                        children.put(mapName, originData.get(mapName));
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.avg.getIdentity(), children);
                    }
                    if (DataIdentityEnums.sd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.sd.getName());
                        RepeatMeasureData measureData = wlData.getOtherData().get(mapName);
                        children.put(mapName, measureData.getSd());
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.sd.getIdentity(), children);
                    }
                    if (DataIdentityEnums.rsd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getName());
                        RepeatMeasureData measureData = wlData.getOtherData().get(mapName);
                        children.put(mapName, measureData.getRsd());
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.rsd.getIdentity(), children);
                    }
                }
            }
        }

        return ans;
    }

    @Override
    public R<String> savePhotometricMeasureData(PhotometricSampleAndDataVO sampleAndDataVO) {

        String taskId = sampleAndDataVO.getTaskId();
        String sampleId = sampleAndDataVO.getSampleId();
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

        if (!StringUtils.hasText(vo.getRemark())) {
            //缺少备注
            return Result.fail(YamlMessageSource.getMessageStatic("00302004"));
        }

        String taskId = vo.getTaskId();
        String sampleId = vo.getSampleId();
        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getSampleKey, sampleId)
                .orderByDesc(TMeasureData::getSerialNumber);
        List<TMeasureData> list = tMeasureDataService.list(queryWrapper);
        TMeasureData data = new TMeasureData();
        if (list != null && !list.isEmpty()) {
            data = list.get(0);
            data.setRemark(vo.getRemark());
        } else {
            data.setTaskId(taskId);
            data.setSampleKey(sampleId);
            data.setSerialNumber(1);
            data.setRemark(vo.getRemark());
        }
        tMeasureDataService.saveOrUpdate(data);

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

        if (photometricParameter.getGain() != null) {
            param.setGain(photometricParameter.getGain());
        }
        if (StringUtils.hasText(photometricParameter.getEnergyLamp())) {
            param.setLampPos(Integer.valueOf(photometricParameter.getEnergyLamp()));
        }
        param.setSlit1(Double.valueOf(photometricParameter.getSpectralBandwidth()));
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
//        measurementCurrentInfoVO.setProjectId(task.getProjectItemKey());
        TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
//        measurementCurrentInfoVO.setProjectName(project.getProjectName());

        return Result.ok(measurementCurrentInfoVO);
    }

    @Override
    public R<File> downloadFileWithChildren(String taskId, Boolean allDisplay, String fileType, Integer digit, List<String> option) {
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
        List<HeadVO> vos = tableHead(taskId, methodId, allDisplay).getData();
        heads.addAll(vos);
        heads.add(0, new HeadVO("序号", "serialNumber"));

        QueryDataDTO queryDataDTO = new QueryDataDTO();
        queryDataDTO.setOption(option != null ? option : new ArrayList<>());
        queryDataDTO.setTaskId(taskId);
        queryDataDTO.setAllDisplay(allDisplay);
        queryDataDTO.setDigit(digit);
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
        List<HeadVO> vos = tableHead(taskId, methodId, allDisplay).getData();
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
                String cellData;

                // 2. 空值处理：null/空字符串/"null" 统一替换为"-"
                if (originalValue == null) {
                    cellData = "-";
                } else {
                    // 原始值转字符串（不做枚举转换、前缀拼接等额外处理）
                    cellData = originalValue.toString();
                    // 清除Excel解析异常的特殊字符（换行/制表符，不修改原始内容）
                    cellData = cellData.replace("\r", "").replace("\n", "").replace("\t", " ");
                    // 处理空字符串或"null"（不区分大小写）
                    if (cellData.trim().isEmpty() || "null".equalsIgnoreCase(cellData)) {
                        cellData = "-";
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
        return Collections.emptyList();
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
        R<TSamplePoolInfoVO> poolInfo = tSamplePoolConfigService.queryCalibrationSampleInfo(deviceSerialNumber);
        if (poolInfo.getSuccess()) {
            TSamplePoolInfoVO poolInfoVO = poolInfo.getData();
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
