/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.concise.common.FMethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.SaveDataDto;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FRationAddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FTaskHeadSnapshotDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.NewRationMethodParamCheckDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.enums.T600ComIdEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.*;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.FormulaUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingParam;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingReceiveInfo;
import com.persee.workstation.uv_inexus.util.pythonUtil.RestUtil;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FMeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FMethodChangeCheckVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FSampleVO;
import com.persee.workstation.uv_inexus.vo.concise.ration.*;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("fRationService")
public class RationServiceConciseImpl implements RationConciseService {

    @Autowired
    SysUserMapper userManageDAO;
    @Autowired
    FMeasureTaskMapper fMeasureTaskMapper;
    @Autowired
    FMeasureProjectService fMeasureProjectService;
    @Autowired
    FMeasureSampleService fMeasureSampleService;
    @Autowired
    FMeasureTaskService fMeasureTaskService;
    @Autowired
    FMeasureMethodService fMeasureMethodService;

    @Autowired
    FMeasureDataService fMeasureDataService;

    @Autowired
    FMeasureAnalysisDataService fMeasureAnalysisDataService;

    @Autowired
    DeviceInfoService deviceInfoService;

    @Autowired
    FMeasureTaskAttachmentService fMeasureTaskAttachmentService;

    @Autowired
    DevMessageUtil devMessageUtil;

    @Autowired
    FormulaUtil formulaUtil;

    @Autowired
    RestUtil restUtil;

    @Autowired
    FSamplePoolConfigService fSamplePoolConfigService;

    @Autowired
    ConcisePublicUtilService concisePublicUtilService;

    @Autowired
    DeviceManageService manageService;


    @Value("1")
    private String deleteType;

    @Value("0")
    private String createType;

    @Value("1")
    String noSave;

    @Value("0")
    String saved;

    @Value("0")
    private String stand;

    @Value("1")
    private String unknown;

    @Value("0")
    String noTested;

    @Value("1")
    String tested;

    @Value("11")
    String defaultProjectId;

    @Value("default")
    String defaultMethodProjectName;

    @Value("文件数据")
    String newTaskName;

    @Value("默认方法")
    String defaultMethodName;

    @Override
    public Result updateMethodParameter(MethodParamsDto methodDto) {
        if (!StringUtils.hasText(methodDto.getMethodId())){
            //请先创建方法或者选择方法
            return Result.fail( YamlMessageSource.getMessageStatic("0010000B"));

        }

        // 根据方法id查询当前方法
        FMeasureMethod method = fMeasureMethodService.getById(methodDto.getMethodId());
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        FRationParameter parameter = new FRationParameter();
        BeanUtils.copyProperties(methodDto, parameter);
        parameter.setEnergyLampName(EnergyLampEnums.getName(parameter.getEnergyLamp()));
        parameter.setCurveTypeName(RationCurveParamEnums.getName(parameter.getCurveType()));
        parameter.setCoefficientModeName(RationCurveParamEnums.getName(parameter.getCoefficientMode()));
        parameter.setEquationTypeName(RationCurveParamEnums.getName(parameter.getEquationType()));
        parameter.setDifferentialModeName(RationCurveParamEnums.getName(parameter.getDifferentialMode()));
        if (parameter.getZeroMethod() != null && !parameter.getZeroMethod().isEmpty()) {
            StringBuilder zeroName = new StringBuilder();
            for (String s : parameter.getZeroMethod()) {
                zeroName.append(RationCurveParamEnums.getName(s)).append(",");
            }
            zeroName.deleteCharAt(zeroName.length() - 1);
            parameter.setZeroMethodName(zeroName.toString());
        }
        /*
         *TODO 待计算结果，暂时加标识，后续需要修改
         */
        for (FPreCountResultParam resultParam : parameter.getResultParams()) {
            resultParam.setParam(resultParam.getResultIdentity() + "-" + resultParam.getFormulaText());
        }

        String param = JSON.toJSONString(parameter);
        // 更新方法参数
        method.setMeasurementParamInfo(param);
        // 更新方法
        boolean update = fMeasureMethodService.updateById(method);
        return Result.ok(update);
    }

    @Override
    public Result createMethodPort(FMethodParamRequestDto methodDto, String updateBy, FRationParameter rationParameter) {
        if (rationParameter.getWaveLength() == null || rationParameter.getWaveLength().isEmpty()) {
            //请填写波长参数
            return Result.fail(YamlMessageSource.getMessageStatic("00000022"));
        }

        // 创建测量方法表
        FMeasureMethod fMeasureMethod = new FMeasureMethod();
        fMeasureMethod.setProjectItemKey(methodDto.getProjectId());
        fMeasureMethod.setMethodName(methodDto.getMethodName());
        fMeasureMethod.setMeasurementType(methodDto.getMethodType());
        fMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(rationParameter));
        fMeasureMethod.setMethodCreator(updateBy);
        fMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        boolean save = fMeasureMethodService.save(fMeasureMethod);
        if (save){
            //创建成功
            return Result.ok(YamlMessageSource.getMessageStatic("0000000F"));
        }else {
            //创建失败
            return Result.fail(YamlMessageSource.getMessageStatic("00000010"));
        }
    }

    @Override
    public Result<FMeasurementCurrentInfoVO> newDraft(SaveDataDto dto) {

        String methodId = dto.getMethodId();
        String taskId = dto.getTaskId();
        String updateBy = dto.getUpdateBy();
        Boolean isSave = dto.getIsSave();
        if (!StringUtils.hasText(methodId)) {
            //请选择方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000002"));
        }
        FMeasureTask oldTask = null;
        if (StringUtils.hasText(taskId) ) {
            oldTask = fMeasureTaskService.getById(taskId);
            if (isSave) {
                saveData(taskId);
            }
        }


        SysUserPO sysUserPO = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
        String userId = sysUserPO.getId();

        FMeasureTask task = new FMeasureTask();
        task.setTaskName(newTaskName + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        task.setIsDelete(createType);
        task.setMeasureType(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription());
        task.setMethodKey(methodId);
        task.setUserId(userId);
        task.setProjectItemKey(defaultProjectId);
        task.setCreateTime(LocalDateTime.now());
        task.setEndSaveTime(LocalDateTime.now());

        if (oldTask != null) {

            if (StringUtils.hasText(oldTask.getTableInfo())) {
                task.setTableInfo(oldTask.getTableInfo());
            }

            if (StringUtils.hasText(oldTask.getZeroParam())){
                task.setZeroParam(oldTask.getZeroParam());
            }
        }
//        else {
//            List<FHeadVO> standHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), SampleTypeEnum.kind_stand.getIdentity());
//            List<FHeadVO> unknownHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), SampleTypeEnum.kind_unknown.getIdentity());
//
//            FRationTaskHeadSnapShot rationTaskHeadSnapShot = new FRationTaskHeadSnapShot();
//
//            FTaskHeadSnapshot standSnapshot = new FTaskHeadSnapshot();
//            standSnapshot.setSampleHeads(standHead);
//            rationTaskHeadSnapShot.setStandHead(standSnapshot);
//
//            FTaskHeadSnapshot unKnowSnapshot = new FTaskHeadSnapshot();
//            unKnowSnapshot.setSampleHeads(unknownHead);
//            rationTaskHeadSnapShot.setUnknownHead(unKnowSnapshot);
//
//            task.setTableInfo(JSON.toJSONString(rationTaskHeadSnapShot));
//        }

        boolean save = fMeasureTaskService.save(task);
        FMeasurementCurrentInfoVO vo = new FMeasurementCurrentInfoVO();
        vo.setMethodId(task.getMethodKey());

        if (save) {
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getTaskName());
            return Result.ok(vo);
        } else {
            //新建失败
            return Result.fail(YamlMessageSource.getMessageStatic("00100001"));
        }
    }

    @Override
    public Result<FMethodChangeCheckVO> checkMethodParam(NewRationMethodParamCheckDTO checkDTO) {

        if (Objects.isNull(checkDTO.getNewMethodParam()) && !StringUtils.hasText(checkDTO.getNewMethodId())) {
            //没有可应用的方法参数
            return Result.fail(YamlMessageSource.getMessageStatic("00103014"));
        }

        if (!StringUtils.hasText(checkDTO.getNewMethodId())) {

            FRationParameter methodParam = checkDTO.getNewMethodParam();

            R<Boolean> r = checkParam(methodParam);
            if (!r.getSuccess()) {
                return Result.fail(r.getMessage());
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

            List<String> waveLengths = methodParam.getWaveLength();

            for (int i = 0; i < waveLengths.size(); i++) {
                String waveStr = waveLengths.get(i);
                if (isBlank(waveStr)) {
                    //"第" + (i + 1) + "个波长值不能为空字符串"
                    return Result.fail(YamlMessageSource.getMessageStatic("00102005",(i + 1)));
                }
                try {
                    double waveVal = Double.parseDouble(waveStr);
                    if (waveVal < minWaveLength || waveVal > maxWaveLength) {
                        //"第" + (i + 1) + "个波长值超出范围（190-900），当前值：" + waveVal
                        return Result.fail(YamlMessageSource.getMessageStatic("00102006",(i + 1),waveVal, minWaveLength, maxWaveLength));
                    }
                    // 校验小数点后最多1位
                    String[] parts = waveStr.split("\\.");
                    if (parts.length > 1 && parts[1].length() > 1) {
                        //"波长值" + waveStr + "小数点后最多保留1位"
                        return Result.fail(YamlMessageSource.getMessageStatic("00105005",waveStr));
                    }
                } catch (NumberFormatException e) {
                    return Result.fail(YamlMessageSource.getMessageStatic("00102007",(i + 1) ,waveStr));
                }
            }

        }

        FMethodChangeCheckVO vo = new FMethodChangeCheckVO();
        vo.setCurrentMethodId(checkDTO.getCurrentMethodId());
        vo.setParamChange(false);
        vo.setClearDataRange(PhotoMetricChangeMethodTypeEnum.noChange.getIdentity());
        vo.setMessage(PhotoMetricChangeMethodTypeEnum.noChange.getMessage());

        if (!StringUtils.hasText(checkDTO.getCurrentMethodId())) {
            log.info("生成新方法");
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.newMethod.getMessage());
            vo.setClearDataRange(RationChangeMethodTypeEnums.newMethod.getIdentity());
            return Result.ok(vo);
        }

        FMeasureMethod currentMethod = fMeasureMethodService.getById(checkDTO.getCurrentMethodId());
        if (StringUtils.hasText(currentMethod.getProjectItemKey()) && currentMethod.getProjectItemKey().equals(defaultMethodProjectName)) {
            log.info("生成新方法");
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.newMethod.getMessage());
            vo.setClearDataRange(RationChangeMethodTypeEnums.newMethod.getIdentity());
            return Result.ok(vo);
        }

        if (!StringUtils.hasText(checkDTO.getCurrentTaskId())) {
            log.info("没有任务存在，将使用该方法生成数据");
            vo.setParamChange(true);
            vo.setClearDataRange(RationChangeMethodTypeEnums.noData.getIdentity());
            return Result.ok(vo);
        }

        FMeasureMethod method = fMeasureMethodService.getById(checkDTO.getCurrentMethodId());
        FRationParameter oldParam = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);

        FRationParameter methodParam = new FRationParameter();
        if (StringUtils.hasText(checkDTO.getNewMethodId())) {
            FMeasureMethod newMethod = fMeasureMethodService.getById(checkDTO.getNewMethodId());
            methodParam = JSON.parseObject(newMethod.getMeasurementParamInfo(), FRationParameter.class);
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
        //检查测量数据
        boolean b = checkMeasureParam(oldParam, methodParam);
        if (!b) {
            log.info("测量参数有改变");
            vo.setClearDataRange(RationChangeMethodTypeEnums.measure.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.measure.getMessage());
            return Result.ok(vo);
        }

        boolean curveParam = checkCurveParam(oldParam, methodParam);
        if (!curveParam) {
            log.info("曲线参数有改变");
            vo.setClearDataRange(RationChangeMethodTypeEnums.curve.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.curve.getMessage());
        }

        //检查分析参数
        boolean analyze = checkAnalyzeParam(oldParam, methodParam);
        if (!analyze) {
            log.info("分析参数有改变");
            vo.setClearDataRange(RationChangeMethodTypeEnums.analyze.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.analyze.getMessage());
        }
        return Result.ok(vo);
    }

    //检查参数是否改变，true--未改变，false--改变了
    private boolean checkMeasureParam(FRationParameter oldParam, FRationParameter newParam) {

        List<String> oldWave = oldParam.getWaveLength();
        List<String> newWave = newParam.getWaveLength();
        if (!Objects.equals(oldWave, newWave)) {
            return false;
        }

        return oldParam.getSpectralBandwidth().equals(newParam.getSpectralBandwidth()) &&
                oldParam.getResponseTime().equals(newParam.getResponseTime()) &&
                oldParam.getChangeLampWaveLength().equals(newParam.getChangeLampWaveLength()) &&
                oldParam.getTimeInterval().equals(newParam.getTimeInterval()) && 
                oldParam.getRepeatMode().equals(newParam.getRepeatMode()) &&
                oldParam.getRepeatCount().equals(newParam.getRepeatCount());
    }

    //检查参数是否改变，true--未改变，false--改变了
    private boolean checkAnalyzeParam(FRationParameter oldParam, FRationParameter newParam) {

        return Objects.equals(oldParam.getSampleInputParams(),newParam.getSampleInputParams()) &&
                Objects.equals(oldParam.getQcParams(), newParam.getQcParams()) &&
                Objects.equals(oldParam.getCurveCoefficient(), newParam.getCurveCoefficient()) &&
                Objects.equals(oldParam.getUnit(), newParam.getUnit()) &&
                Objects.equals(oldParam.getResultParams(), newParam.getResultParams()) &&
                Objects.equals(oldParam.getApproxParams(), newParam.getApproxParams());

    }

    /*
    * 检查曲线参数是否改变，true--未改变，false--改变了
     */
    private boolean checkCurveParam(FRationParameter oldParam, FRationParameter newParam) {
        List<String> oldLengthParam = oldParam.getWaveLengthParams();
        List<String> newLengthParam = newParam.getWaveLengthParams();
        if (!Objects.equals(oldLengthParam, newLengthParam)) {
            return false;
        }

        List<String> oldZero = oldParam.getZeroMethod();
        List<String> newZero = newParam.getZeroMethod();
        if (!Objects.equals(oldZero, newZero)) {
            return false;
        }
        if (!oldParam.getCoefficientMode().equals(newParam.getCoefficientMode())) {
            return false;
        }

        List<String> oldCoefficient = oldParam.getCoefficientParams();
        List<String> newCoefficient = newParam.getCoefficientParams();
        if (!Objects.equals(oldCoefficient, newCoefficient)) {
            return false;
        }


        return oldParam.getCurveType().equals(newParam.getCurveType()) &&
                oldParam.getEquationType().equals(newParam.getEquationType());

    }

    private R<Boolean> checkParam(FRationParameter parameter) {
        // 1. 根对象非空校验
        if (parameter == null) {
            //定量测量参数对象不能为空
            return R.fail(YamlMessageSource.getMessageStatic("00102003"));
        }

        // 2. 波长相关参数校验
        // 2.1 波长值校验（1-3个，数值范围190-900）
        List<String> waveLengths = parameter.getWaveLength();
        if (waveLengths == null || waveLengths.isEmpty()) {
            //波长值不能为空
            return R.fail(YamlMessageSource.getMessageStatic("00105003"));
        }
        if (waveLengths.size() > 3) {
            //"波长值最多支持3个，当前数量：" + waveLengths.size()
            return R.fail(YamlMessageSource.getMessageStatic("00102004", waveLengths.size()));
        }


        if (StringUtils.hasText(parameter.getChangeLampWaveLength())) {
            double v = Double.parseDouble(parameter.getChangeLampWaveLength());
            if (v < 330 || v > 390) {
                //"换灯波长要在（330-390）之间，当前值：" + v
                return R.fail(YamlMessageSource.getMessageStatic("0010202A",v));
            }

            if (waveLengths.contains(parameter.getChangeLampWaveLength())) {
                //换灯波长不能和测量波长一样。
                return R.fail(YamlMessageSource.getMessageStatic("0010202B"));
            }
        }

        // 2.2 波长参数校验（与波长值数量一致）
        List<String> waveLengthParams = parameter.getWaveLengthParams();
        if (waveLengthParams == null) {
            //波长参数不能为空
            return R.fail(YamlMessageSource.getMessageStatic("00102008"));
        }
        if (waveLengthParams.size() != waveLengths.size()) {
            //"波长参数数量需与波长值一致（当前参数数：" + waveLengthParams.size() + "，波长数：" + waveLengths.size() + "）"
            return R.fail(YamlMessageSource.getMessageStatic("00102009",waveLengthParams.size(), waveLengths.size()));
        }
        for (int i = 0; i < waveLengthParams.size(); i++) {
            if (isBlank(waveLengthParams.get(i))) {
                return R.fail(YamlMessageSource.getMessageStatic("0010200A" , (i + 1)));
            }
        }

        // 2.3 微分方式校验（枚举值）
//        String differentialMode = parameter.getDifferentialMode();
//        if (isBlank(differentialMode)) {
//            return R.fail("微分方式不能为空");
//        }
//        List<String> validDiffModes = Arrays.asList(
//                "single", "1st-der", "2nd-der", "3rd-der", "4th-der",
//                "peak", "valley", "max", "min"
//        );
//        if (!validDiffModes.contains(differentialMode)) {
//            return R.fail("微分方式不合法（可选值：" + String.join("、", validDiffModes) + "），当前值：" + differentialMode);
//        }

        // 3. 曲线与系数相关校验
        // 3.1 曲线类型校验
        if (isBlank(parameter.getCurveType())) {
            //曲线类型不能为空
            return R.fail(YamlMessageSource.getMessageStatic("0010200B"));
        }

        // 3.2 方程类型校验（枚举值：qtCFA/qtAFC）
        String equationType = parameter.getEquationType();
        if (isBlank(equationType)) {
            //方程类型不能为空
            return R.fail(YamlMessageSource.getMessageStatic("0010200C"));
        }
//        if (!Arrays.asList("qtCFA", "qtAFC").contains(equationType)) {
//            return R.fail("方程类型不合法（可选值：qtCFA、qtAFC），当前值：" + equationType);
//        }

        // 3.3 系数方式校验（枚举值：0/1）
        String coefficientMode = parameter.getCoefficientMode();
        if (isBlank(coefficientMode)) {
            //系数方式不能为空
            return R.fail(YamlMessageSource.getMessageStatic("0010200D"));
        }
        if (RationCurveParamEnums.getName(coefficientMode) == null) {
            //系数方式不合法（可选值：直接系数法、标准曲线法）
            return R.fail(YamlMessageSource.getMessageStatic("0010200E"));
        }
        if (RationCurveParamEnums.direct_coefficient.getIdentity().equals(coefficientMode)) {
            // 3.4 系数参数校验（1-10个，数字且最多3位小数）
            List<String> coefficientParams = parameter.getCoefficientParams();
            if (coefficientParams == null || coefficientParams.isEmpty()) {
                //系数参数不能为空
                return R.fail(YamlMessageSource.getMessageStatic("0010200F"));
            }
            if (coefficientParams.size() > 10) {
                //"系数参数最多支持10个，当前数量：" + coefficientParams.size()
                return R.fail(YamlMessageSource.getMessageStatic("00102010",coefficientParams.size()));
            }

            int curveType = Integer.parseInt(parameter.getCurveType());
            if (coefficientParams.size() < curveType + 1) {
                //"系数参数数量不足，当前数量：" + coefficientParams.size() + "，所需数量：" + (curveType + 1)
                return R.fail(YamlMessageSource.getMessageStatic("00102011",coefficientParams.size(),(curveType + 1)));
            }

            for (int i = 0; i < coefficientParams.size(); i++) {
                String coeffStr = coefficientParams.get(i);
                if (isBlank(coeffStr)) {
                    //"第" + (i + 1) + "个系数参数不能为空字符串"
                    return R.fail(YamlMessageSource.getMessageStatic("00102012",(i + 1)));
                }
                try {
                    Double.parseDouble(coeffStr);
                    if (coeffStr.contains(".") && coeffStr.split("\\.")[1].length() > 10) {
                        //"第" + (i + 1) + "个系数参数小数位数不能超过10位，当前值：" + coeffStr
                        return R.fail(YamlMessageSource.getMessageStatic("00102013",(i + 1),coeffStr));
                    }
                } catch (NumberFormatException e) {
                    //"第" + (i + 1) + "个系数参数格式错误（需为数字），当前值：" + coeffStr
                    return R.fail(YamlMessageSource.getMessageStatic("00102014",(i + 1),coeffStr));
                }
            }
        }

        // 3.5 零点方式校验
        List<String> zeroMethod = parameter.getZeroMethod();
//        if (zeroMethod == null || zeroMethod.isEmpty()) {
//            return R.fail("零点方式不能为空");
//        }
        if (zeroMethod != null && !zeroMethod.isEmpty()) {
            for (String zm : zeroMethod) {
                if (RationCurveParamEnums.getName(zm) == null) {
                    //零点方式不合法（可选值：过零点、插入零点）
                    return R.fail(YamlMessageSource.getMessageStatic("00102015"));
                }
            }
        }


        // 4. 重复测量相关校验
        // 4.1 重复模式校验（枚举值：rmNone/rmCR）
        String repeatMode = parameter.getRepeatMode();
        if (isBlank(repeatMode)) {
            //重复模式不能为空
            return R.fail(YamlMessageSource.getMessageStatic("00103007"));
        }

        if (RationCurveParamEnums.getName(repeatMode) == null) {
            //重复模式不合法
            return R.fail(YamlMessageSource.getMessageStatic("00102016" ));
        }

        // 4.2 单池重复（rmCR）时的关联校验
        if ("单池重复".equals(repeatMode)) {
            // 重复次数：2-10的整数
            String repeatCountStr = parameter.getRepeatCount();
            if (isBlank(repeatCountStr)) {
                //单池重复模式下，重复次数不能为空
                return R.fail(YamlMessageSource.getMessageStatic("00102017"));
            }
            try {
                int repeatCount = Integer.parseInt(repeatCountStr);
                if (repeatCount < 2 || repeatCount > 10) {
                    //重复次数需在2-10范围内，当前值：repeatCount
                    return R.fail(YamlMessageSource.getMessageStatic("00102018" ,repeatCount));
                }
            } catch (NumberFormatException e) {
                //"重复次数格式错误（需为整数），当前值：" + repeatCountStr
                return R.fail(YamlMessageSource.getMessageStatic("00102019",repeatCountStr));
            }

            // 时间间隔：0-60秒，保留1位小数
            String timeIntervalStr = parameter.getTimeInterval();
            if (isBlank(timeIntervalStr)) {
                //单池重复模式下，时间间隔不能为空
                return R.fail(YamlMessageSource.getMessageStatic("0010201A"));
            }
            try {
                double timeInterval = Double.parseDouble(timeIntervalStr);
                if (timeInterval < 0 || timeInterval > 60) {
                    //"时间间隔需在0-60秒范围内，当前值：" + timeInterval
                    return R.fail(YamlMessageSource.getMessageStatic("0010201B",timeInterval));
                }
                if (timeIntervalStr.contains(".") && timeIntervalStr.split("\\.")[1].length() > 1) {
                    //"时间间隔小数位数不能超过1位，当前值：" + timeIntervalStr
                    return R.fail(YamlMessageSource.getMessageStatic("0010201C",timeIntervalStr));
                }
            } catch (NumberFormatException e) {
                //"时间间隔格式错误（需为数字），当前值：" + timeIntervalStr
                return R.fail(YamlMessageSource.getMessageStatic("0010201D",timeIntervalStr));
            }
        }


        // 6. 复杂结构参数校验
        // 6.1 样品输入参数校验（最多10个）
        List<FInputParam> sampleInputParams = parameter.getSampleInputParams();
        if (sampleInputParams != null && !sampleInputParams.isEmpty()) {
            if (sampleInputParams.size() > 10) {
                //"样品输入参数最多支持10个，当前数量：" + sampleInputParams.size()
                return R.fail(YamlMessageSource.getMessageStatic("0010201E",sampleInputParams.size()));
            }
            for (int i = 0; i < sampleInputParams.size(); i++) {
                FInputParam inputParam = sampleInputParams.get(i);
                if (inputParam == null || isBlank(inputParam.getParamName())) {
                    //"第" + (i + 1) + "个样品输入参数标题不能为空"
                    return R.fail(YamlMessageSource.getMessageStatic("0010201F",(i+1)));
                }
            }
        }

        // 6.2 待计算结果参数校验（最多5个）
        List<FPreCountResultParam> resultParams = parameter.getResultParams();
        if (resultParams != null && !resultParams.isEmpty()) {
            if (resultParams.size() > 5) {
                return R.fail(YamlMessageSource.getMessageStatic("00102020",resultParams.size()));
            }
            for (int i = 0; i < resultParams.size(); i++) {
                FPreCountResultParam resultParam = resultParams.get(i);
                if (resultParam == null || isBlank(resultParam.getResultName()) || isBlank(resultParam.getFormulaText())) {
                    //"第" + (i + 1) + "个待计算结果参数标题或公式不能为空"
                    return R.fail(YamlMessageSource.getMessageStatic("00102021",(i + 1)));
                }
            }
        }

        // 6.3 修约设置校验（最多10个）
        List<FRoundingVO> approxParams = parameter.getApproxParams();
        if (approxParams != null && !approxParams.isEmpty()) {

            if (approxParams.size() > 10) {
                //"最多支持修约10个变量" + approxParams.size()
                return R.fail(YamlMessageSource.getMessageStatic("00103010",approxParams.size()));
            }
            for (FRoundingVO approxParam : approxParams) {

                if (approxParam.getRoundingParams() == null || approxParam.getRoundingParams().isEmpty()) {
                    //"修约设置" + approxParam.getResultIdentity() + "的修约条件不能为空"
                    return R.fail(YamlMessageSource.getMessageStatic("00103011",approxParam.getResultIdentity()));
                }
                if (approxParam.getRoundingParams().size() > 10) {
                    //"修约设置" + approxParam.getResultIdentity() + "的修约条件数量不能超过10个"
                    return R.fail(YamlMessageSource.getMessageStatic("00103012",approxParam.getResultIdentity()));
                }

                R<Boolean> r = concisePublicUtilService.checkApproximateRange(approxParam);
                if (!r.getSuccess()) {
                    return r;
                }
            }
        }


        // 6.4 质控设置校验（最多10个，类型合法）
        List<FQualityConfVO> qcParams = parameter.getQcParams();
        if (qcParams != null && !qcParams.isEmpty() && qcParams.size() > 10) {
            //"质控设置最多支持10个规则，当前数量：" + qcParams.size()
            return R.fail(YamlMessageSource.getMessageStatic("00102022",qcParams.size()));
        }

        // 7. 继承参数校验（方法名称等长度限制）
        if (parameter.getEquationTypeName() != null && parameter.getEquationTypeName().length() > 256) {
            //方法名称长度不能超过256字符
            return R.fail(YamlMessageSource.getMessageStatic("00102023"));
        }

        return R.ok(true);
    }


    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }



    @Override
    public Result<FMeasurementCurrentInfoVO> applyMethod(NewRationMethodParamCheckDTO methodDTO) {

        if (Objects.isNull(methodDTO.getNewMethodParam()) && !StringUtils.hasText(methodDTO.getNewMethodId())) {
            //没有可应用的方法或参数
            return Result.fail(YamlMessageSource.getMessageStatic("00103015"));
        }
        if (!StringUtils.hasText(methodDTO.getMeasureType())) {
            methodDTO.setMeasureType(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription());
        }
        String methodId = methodDTO.getNewMethodId();

        FMeasurementCurrentInfoVO ans = new FMeasurementCurrentInfoVO();

        SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, methodDTO.getUpdateBy()));
        String userId = po.getId();
        ans.setMethodName(methodDTO.getMethodName());

        if (!StringUtils.hasText(methodId)) {
            FMeasureMethod method = new FMeasureMethod();
            method.setMeasurementParamInfo(JSON.toJSONString(methodDTO.getNewMethodParam()));

            method.setMethodName(defaultMethodName + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            method.setMeasurementType(methodDTO.getMeasureType());
            method.setProjectItemKey(defaultProjectId);
            method.setMethodCreator(methodDTO.getUpdateBy());
            method.setMethodCreateTime(LocalDateTime.now());
            method.setMethodUseTime(LocalDateTime.now());
            boolean save = fMeasureMethodService.save(method);

            if (!save) {
                //应用失败
                return Result.fail(YamlMessageSource.getMessageStatic("00100002"));
            }
            methodId = method.getId();
            ans.setMethodName(method.getMethodName());
        }
        ans.setMethodId(methodId);

        FittingReceiveInfo curveInfo = createFittingInfo(methodId);
        String fittingInfo = null;
        if (curveInfo != null) {
            fittingInfo = JSONObject.toJSONString(curveInfo);
        }

        FMeasureTask task = new FMeasureTask();
        if (!StringUtils.hasText(methodDTO.getCurrentTaskId())) {
            //任务不存在，创建任务和方法
            task.setTaskName(newTaskName + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            task.setMethodKey(methodId);
            task.setCreateTime(LocalDateTime.now());
            task.setEndSaveTime(LocalDateTime.now());
            task.setUserId(userId);
            task.setProjectItemKey(defaultProjectId);
            task.setIsDelete(createType);
            task.setMeasureType(methodDTO.getMeasureType());

            List<FHeadVO> standHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), SampleTypeEnum.kind_stand.getIdentity());
            List<FHeadVO> unknownHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), SampleTypeEnum.kind_unknown.getIdentity());

            FRationTaskHeadSnapShot rationTaskHeadSnapShot = new FRationTaskHeadSnapShot();

            FTaskHeadSnapshot standSnapshot = new FTaskHeadSnapshot();
            standSnapshot.setSampleHeads(standHead);
            rationTaskHeadSnapShot.setStandHead(standSnapshot);

            FTaskHeadSnapshot unKnowSnapshot = new FTaskHeadSnapshot();
            unKnowSnapshot.setSampleHeads(unknownHead);
            rationTaskHeadSnapShot.setStandHead(unKnowSnapshot);

            task.setTableInfo(JSON.toJSONString(rationTaskHeadSnapShot));
            task.setCreateTime(LocalDateTime.now());
            task.setEndSaveTime(LocalDateTime.now());
            task.setFittingParam(fittingInfo);
            fMeasureTaskService.save(task);

            ans.setTaskId(task.getId());
            ans.setTaskName(task.getTaskName());
            setParamToDevice(methodId, methodDTO.getDeviceSerialNumber());
            return Result.ok(ans);

        } else {
            task = fMeasureTaskService.getById(methodDTO.getCurrentTaskId());

            if (task == null) {
                task = new FMeasureTask();
                task.setTaskName(newTaskName + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
                task.setMethodKey(methodId);
                task.setCreateTime(LocalDateTime.now());
                task.setEndSaveTime(LocalDateTime.now());
                task.setUserId(userId);
                task.setProjectItemKey(defaultProjectId);
                task.setIsDelete(createType);
                task.setMeasureType(methodDTO.getMeasureType());

                List<FHeadVO> standHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), SampleTypeEnum.kind_stand.getIdentity());
                List<FHeadVO> unknownHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), SampleTypeEnum.kind_unknown.getIdentity());

                FRationTaskHeadSnapShot rationTaskHeadSnapShot = new FRationTaskHeadSnapShot();

                FTaskHeadSnapshot standSnapshot = new FTaskHeadSnapshot();
                standSnapshot.setSampleHeads(standHead);
                rationTaskHeadSnapShot.setStandHead(standSnapshot);

                FTaskHeadSnapshot unKnowSnapshot = new FTaskHeadSnapshot();
                unKnowSnapshot.setSampleHeads(unknownHead);
                rationTaskHeadSnapShot.setStandHead(unKnowSnapshot);

                task.setTableInfo(JSON.toJSONString(rationTaskHeadSnapShot));
                task.setFittingParam(fittingInfo);
                fMeasureTaskService.save(task);

                ans.setTaskId(task.getId());
                ans.setTaskName(task.getTaskName());
                setParamToDevice(methodId, methodDTO.getDeviceSerialNumber());
                return Result.ok(ans);
            }

            if (!StringUtils.hasText(task.getSampleKey())) {
                task.setMethodKey(methodId);

                task.setFittingParam(fittingInfo);

                fMeasureTaskService.updateById(task);

                ans.setTaskId(task.getId());
                ans.setTaskName(task.getTaskName());
                setParamToDevice(methodId, methodDTO.getDeviceSerialNumber());
                return Result.ok(ans);

            }

            if (methodDTO.getIsSaveDraft() == null || !methodDTO.getIsSaveDraft()) {

                if (methodDTO.getClearDataRange().equals(PhotoMetricChangeMethodTypeEnum.measure.getIdentity())) {
                    LambdaQueryWrapper<FMeasureData> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(FMeasureData::getTaskId, task.getId())
                            .eq(FMeasureData::getIsSave, noSave);
                    fMeasureDataService.remove(wrapper);

                    LambdaQueryWrapper<FMeasureAnalysisData> analysisWrapper = new LambdaQueryWrapper<>();
                    analysisWrapper.eq(FMeasureAnalysisData::getTaskId, task.getId())
                            .eq(FMeasureAnalysisData::getIsSave, noSave);
                    fMeasureAnalysisDataService.remove(analysisWrapper);

                    FMeasureTask newTask = new FMeasureTask();
                    newTask.setProjectItemKey(defaultProjectId);
                    newTask.setUserId(userId);
                    newTask.setTaskName(newTaskName + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));


                    if (StringUtils.hasText(task.getTableInfo())) {
                        newTask.setTableInfo(task.getTableInfo());
                    }

                    newTask.setId(null);
                    newTask.setMethodKey(methodId);
                    newTask.setDeviceSerialNumber(null);
                    newTask.setIsDelete(createType);
                    newTask.setCreateTime(LocalDateTime.now());
                    newTask.setEndSaveTime(LocalDateTime.now());
                    newTask.setMeasureType(methodDTO.getMeasureType());
                    fMeasureTaskService.save(newTask);
                    ans.setTaskId(newTask.getId());
                    ans.setTaskName(newTask.getTaskName());
                    setParamToDevice(methodId, methodDTO.getDeviceSerialNumber());
                    return Result.ok(ans);

                } else if (methodDTO.getClearDataRange().equals(RationChangeMethodTypeEnums.curve.getIdentity())) {
                    int digit = Constants.DEFAULT_DECIMAL_NUM;
                    R<FMeasureTaskAttachment> attachmentR = fMeasureTaskAttachmentService.queryByTaskId(task.getId());
                    if (attachmentR.getSuccess()) {
                        FMeasureTaskAttachment attachment = attachmentR.getData();
                        if (StringUtils.hasText(attachment.getDataParam())) {
                            FQueryDataDTO queryDataDTO = JSON.parseObject(attachment.getDataParam(), FQueryDataDTO.class);
                            if (queryDataDTO.getDigit() != null) {
                                digit = queryDataDTO.getDigit();
                            }
                        }
                    }
                    task.setMethodKey(methodId);
                    if (StringUtils.hasText(fittingInfo)){
                        task.setFittingParam(fittingInfo);
                        fMeasureTaskService.updateById(task);
                        changeUnknowSampleDigit(task.getId(), digit);

                    } else {
                        task.setFittingParam(fittingInfo);
                        fMeasureTaskService.updateById(task);
                        changeStandSampleDataDigit(task.getId(), digit);
                        changeUnknowSampleDigit(task.getId(), digit);
                    }

                    ans.setTaskId(task.getId());
                    ans.setTaskName(task.getTaskName());
                    setParamToDevice(methodId, methodDTO.getDeviceSerialNumber());
                    return Result.ok(ans);

                } else if (methodDTO.getClearDataRange().equals(RationChangeMethodTypeEnums.analyze.getIdentity())) {
                    task.setMethodKey(methodId);
                    fMeasureTaskService.updateById(task);

                    int digit = Constants.DEFAULT_DECIMAL_NUM;
                    R<FMeasureTaskAttachment> attachmentR = fMeasureTaskAttachmentService.queryByTaskId(task.getId());
                    if (attachmentR.getSuccess()) {
                        FMeasureTaskAttachment attachment = attachmentR.getData();
                        if (StringUtils.hasText(attachment.getDataParam())) {
                            FQueryDataDTO queryDataDTO = JSON.parseObject(attachment.getDataParam(), FQueryDataDTO.class);
                            if (queryDataDTO.getDigit() != null) {
                                digit = queryDataDTO.getDigit();
                            }
                        }
                    }
                    changeStandSampleDataDigit(task.getId(), digit);
                    changeUnknowSampleDigit(task.getId(), digit);
                    //TODO 公式重算

                    ans.setTaskId(task.getId());
                    ans.setTaskName(task.getTaskName());
                    setParamToDevice(methodId, methodDTO.getDeviceSerialNumber());
                    return Result.ok(ans);
                } else {
                    task.setMethodKey(methodId);
                    fMeasureTaskService.updateById(task);
                    ans.setTaskId(task.getId());
                    ans.setTaskName(task.getTaskName());
                    setParamToDevice(methodId, methodDTO.getDeviceSerialNumber());
                    return Result.ok(ans);
                }
            } else {
                //保存数据

                saveData(task.getId());

                FMeasureTask newTask = new FMeasureTask();
                newTask.setProjectItemKey(defaultProjectId);
                newTask.setUserId(userId);
                newTask.setTaskName(newTaskName + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
                if (StringUtils.hasText(task.getTableInfo())) {
                    newTask.setTableInfo(task.getTableInfo());
                }

                newTask.setCreateTime(LocalDateTime.now());
                newTask.setEndSaveTime(LocalDateTime.now());
                newTask.setId(null);
                newTask.setMeasureType(methodDTO.getMeasureType());
                newTask.setMethodKey(methodId);
                newTask.setDeviceSerialNumber(null);
                newTask.setIsDelete(createType);
                newTask.setFittingParam(fittingInfo);
                fMeasureTaskService.save(newTask);

                ans.setTaskId(newTask.getId());
                ans.setTaskName(newTask.getTaskName());
                setParamToDevice(methodId, methodDTO.getDeviceSerialNumber());
                return Result.ok(ans);

            }
        }
    }

    private void setParamToDevice(String methodId, String deviceSerialNumber) {

        if (!StringUtils.hasText(deviceSerialNumber)) {
            log.info("设备未连接,无需下发参数");
            return;
        }

        FMeasureMethod fMeasureMethod = fMeasureMethodService.getById(methodId);
        if (fMeasureMethod == null) {
            log.warn("找不到对应的测量方法: methodId={}", methodId);
            return;
        }

        FRationParameter parameter = JSON.parseObject(fMeasureMethod.getMeasurementParamInfo(), FRationParameter.class);
        if (parameter == null) {
            log.warn("解析测量参数失败: methodId={}, json={}", methodId, fMeasureMethod.getMeasurementParamInfo());
            return;
        }
        String ip = deviceInfoService.getIp(deviceSerialNumber);
        if (StringUtils.hasText(ip)) {
            List<Command> list = new ArrayList<>();
            list.add(new Command(T600ComIdEnums.SetPhotoMode.getComId(), new Object[]{WaveLengthEnum.abs.getCode()}));

            String changeLampWaveLength = parameter.getChangeLampWaveLength();
            Double v1 = formulaUtil.calculateStep(Double.parseDouble(changeLampWaveLength), deviceSerialNumber);
            list.add(new Command(T600ComIdEnums.SetChangeLampWl.getComId(), new Object[]{v1}));

            String responseTime = StringUtils.hasText(parameter.getResponseTime()) ? parameter.getResponseTime() : "0.5";
            list.add(new Command(T600ComIdEnums.SetResponseTime.getComId(), new Object[]{responseTime}));

            String waveLength = "660";
            if (parameter.getWaveLength() != null && !parameter.getWaveLength().isEmpty()){
                waveLength = parameter.getWaveLength().get(parameter.getWaveLength().size() - 1);
            }

            Double step = formulaUtil.calculateStep(Double.parseDouble(waveLength), deviceSerialNumber);

            list.add(new Command(T600ComIdEnums.SetWl.getComId(), new Object[]{step}));

            try {
                deviceInfoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_OTHER);
                devMessageUtil.photometricAndRationMeasurement(list, ip);
            } catch (Exception e) {
                log.info("参数设置异常");
            } finally {
                deviceInfoService.releaseDevice(deviceSerialNumber);
            }
        }

    }

    private void saveData(String taskId) {
        LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureData::getTaskId, taskId)
                .eq(FMeasureData::getIsSave, noSave);
        List<FMeasureData> list = fMeasureDataService.list(queryWrapper);
        // 批量更新优化
        if (!list.isEmpty()) {
            list.forEach(fMeasureData -> fMeasureData.setIsSave(saved));
            fMeasureDataService.updateBatchById(list);
        }

        LambdaQueryWrapper<FMeasureAnalysisData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                .eq(FMeasureAnalysisData::getIsSave, noSave);
        List<FMeasureAnalysisData> anaList = fMeasureAnalysisDataService.list(wrapper);
        if (!anaList.isEmpty()) {
            anaList.forEach(fMeasureAnalysisData -> fMeasureAnalysisData.setIsSave(saved));
            fMeasureAnalysisDataService.updateBatchById(anaList);
        }
    }

    private FittingReceiveInfo createFittingInfo(String methodId) {
        FittingReceiveInfo fittingReceiveInfo = new FittingReceiveInfo();

        FMeasureMethod fMeasureMethod = fMeasureMethodService.getById(methodId);
        if (StringUtils.hasText(fMeasureMethod.getMeasurementParamInfo())) {
            FRationParameter parameter = JSON.parseObject(fMeasureMethod.getMeasurementParamInfo(), FRationParameter.class);
            if (parameter.getCoefficientMode().equals(RationCurveParamEnums.direct_coefficient.getIdentity())) {
                int degree = Integer.parseInt(parameter.getCurveType());
                fittingReceiveInfo.setDegree(degree);

                String variable = parameter.getEquationType().equals(RationCurveParamEnums.typeAbs.getIdentity())? "Abs":"C";
                String Y = parameter.getCurveType().equals(RationCurveParamEnums.typeAbs.getIdentity())? "C":"Abs";
                StringBuilder formula = new StringBuilder();
                formula.append(Y).append(" = ");
                List<String> coefficientList = parameter.getCoefficientParams();
                List<Double> paramList = new ArrayList<>();
                while (coefficientList.size() < degree + 1) {
                    coefficientList.add("0");
                }
                for (int i = 0; i <= degree; i++) {

                    // 系数索引：从高次到低次（第一个系数是最高次项）
                    int coeffIndex = degree - i;
                    double coeffValue = 0.0;
                    paramList.add(Double.valueOf(coefficientList.get(i)));
                    // 解析系数值
                    if (coeffIndex < coefficientList.size()) {
                        try {
                            coeffValue = Double.parseDouble(coefficientList.get(coeffIndex));

                        } catch (NumberFormatException ignored) {
                            // 保持默认值0.0
                        }
                    }
                    // 设置系数字段（根据degree动态设置）
                    switch (degree - i) {
                        case 0: fittingReceiveInfo.setK0(coeffValue); break;
                        case 1: fittingReceiveInfo.setK1(coeffValue); break;
                        case 2: fittingReceiveInfo.setK2(coeffValue); break;
                        case 3: fittingReceiveInfo.setK3(coeffValue); break;
                        case 4: fittingReceiveInfo.setK4(coeffValue); break;
                    }
                    if(!coefficientList.get(degree - i).equals("0")){
                        double cuff = Double.parseDouble(coefficientList.get(degree - i));
                        if (i==0){
                            if (cuff < 0) {
                                formula.append(" - ");
                            }
                        }
                        if (i > 0) {
                            if (cuff < 0) {
                                formula.append(" - ");
                            } else {
                                formula.append(" + ");
                            }
                        }
                        System.out.println(formula);
                        System.out.println(cuff);
                        // 添加系数绝对值，如果是负数则取绝对值
                        formula.append(Math.abs(cuff));
                        if (degree - i > 0) {
                            formula.append("*").append(variable);
                            if (degree - i > 1) {
                                formula.append("^").append(degree - i);
                            }
                        }
                    }
                }
                fittingReceiveInfo.setEquation(formula.toString());
                fittingReceiveInfo.setCoefficients(paramList);
                return fittingReceiveInfo;

            }

        }
        return null;
    }


    /**
     * 计算拟合信息的X轴范围
     *
     * @param taskId             任务ID
     * @param parameter          参数对象
     * @return FittingRangeResult or null
     */
    private FittingRangeResult calculateFittingInfoRange(String taskId, FRationParameter parameter) {
        return calculateFittingInfoRange(taskId, parameter, true, true, null);
    }

    private FittingRangeResult calculateFittingInfoRange(String taskId, FRationParameter parameter, boolean includeUnknown, boolean includeStand, FittingReceiveInfo fitting) {
        if (parameter == null) {
            return null;
        }

        if (StringUtils.hasText(taskId)) {
            FMeasureTask task = fMeasureTaskService.getById(taskId);
            if (task != null && StringUtils.hasText(task.getSampleKey())) {
                List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
                if (taskSpecimen != null) {
                    Double minX = null;
                    Double maxX = null;
                    for (FTaskSpecimen specimen : taskSpecimen) {
                        if (specimen.getDeleteType().equals(deleteType)) {
                            continue;
                        }

                        FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());
                        if (sample == null) {
                            continue;
                        }
                        String kind = sample.getSampleKind();
                        if (SampleTypeEnum.kind_unknown.getIdentity().equals(kind) && !includeUnknown) {
                            continue;
                        }
                        if (SampleTypeEnum.kind_stand.getIdentity().equals(kind) && !includeStand) {
                            continue;
                        }

                        String abs = null;
                        String sumAbs = null;
                        String countValue = null;

                        LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                        queryWrapper.eq(FMeasureData::getTaskId, taskId)
                                .eq(FMeasureData::getSampleKey, sample.getId())
                                .orderByDesc(FMeasureData::getSerialNumber)
                                .last("limit 1");
                        FMeasureData data = fMeasureDataService.getOne(queryWrapper);
                        if (data == null) {
                            continue;
                        }

                        LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                        analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                                .eq(FMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                                .eq(FMeasureAnalysisData::getMeasureDataId, data.getId())
                                .orderByDesc(FMeasureAnalysisData::getDataVersion)
                                .last("limit 1");
                        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                        if (analysisData == null) {
                            analysisData = new FMeasureAnalysisData();
                        }

                        if (StringUtils.hasText(analysisData.getAnalysisResult())) {
                            List<FRationCountDataInfo> countDataInfoList = JSON.parseArray(analysisData.getAnalysisResult(), FRationCountDataInfo.class);
                            for (FRationCountDataInfo dataInfo : countDataInfoList) {
                                if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                                    abs = dataInfo.getColValue();
                                }
                                if (dataInfo.getIdentity().equals(SampleTableEnum.SUM_Abs.getIdentity())) {
                                    sumAbs = dataInfo.getColValue();
                                }
                                if (dataInfo.getIdentity().equals(SampleTableEnum.FIT_CONCENTRATION.getIdentity())) {
                                    countValue = dataInfo.getColValue();
                                }
                            }
                        }

                        if (SampleTypeEnum.kind_stand.getIdentity().equals(kind) && !StringUtils.hasText(countValue)) {
                            countValue = sample.getNormalConcentration();
                        }

                        String measureValue;
                        if (StringUtils.hasText(sumAbs)) {
                            measureValue = sumAbs;
                        } else {
                            measureValue = abs;
                        }

                        Double xValue = null;
                        try {
                            if (parameter.getEquationType().equals(RationCurveParamEnums.typeAbs.getIdentity())) {
                                if (measureValue != null) {
                                    xValue = Double.valueOf(measureValue);
                                }
                            } else {
                                String conc = countValue;
                                if (!StringUtils.hasText(conc) && fitting != null && StringUtils.hasText(measureValue)) {
                                    Double mv = null;
                                    try {
                                        mv = Double.valueOf(measureValue);
                                    } catch (Exception ignored) {}
                                    if (mv != null) {
                                        String calc = valueCalculate(fitting, mv, RationCurveParamEnums.typeCurve.getIdentity());
                                        conc = calc;
                                    }
                                }
                                if (StringUtils.hasText(conc)) {
                                    xValue = Double.valueOf(conc);
                                }
                            }
                        } catch (NumberFormatException ignored) {
                        }

                        if (xValue != null) {
                            if (minX == null || xValue < minX) {
                                minX = xValue;
                            }
                            if (maxX == null || xValue > maxX) {
                                maxX = xValue;
                            }
                        }
                    }

                    if (minX != null && maxX != null) {
                        return new FittingRangeResult(minX, maxX);
                    }
                }
            }
        }
        return null;
    }

    private Map<String, List<JSONObject>> collectSamplePoints(String taskId, FRationParameter parameter, Integer digit, FittingReceiveInfo fitting, boolean includeUnknown, boolean includeStand) {
        Map<String, List<JSONObject>> ans = new HashMap<>();
        List<JSONObject> unknownPoints = new ArrayList<>();
        List<JSONObject> standPoints = new ArrayList<>();

        if (!StringUtils.hasText(taskId)) {
            ans.put("unknown", unknownPoints);
            ans.put("stand", standPoints);
            return ans;
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null || !StringUtils.hasText(task.getSampleKey())) {
            ans.put("unknown", unknownPoints);
            ans.put("stand", standPoints);
            return ans;
        }

        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        if (taskSpecimen == null) {
            ans.put("unknown", unknownPoints);
            ans.put("stand", standPoints);
            return ans;
        }

        for (FTaskSpecimen specimen : taskSpecimen) {
            if (specimen.getDeleteType().equals(deleteType)) {
                continue;
            }
            if (specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {
                continue;
            }

            FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());
            if (sample == null) {
                continue;
            }
            String kind = sample.getSampleKind();
            if (SampleTypeEnum.kind_unknown.getIdentity().equals(kind) && !includeUnknown) {
                continue;
            }
            if (SampleTypeEnum.kind_stand.getIdentity().equals(kind) && !includeStand) {
                continue;
            }

            String abs = null;
            String sumAbs = null;
            String countValue = null;
            String fitData = null;
            String measureValue;

            LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FMeasureData::getTaskId, taskId)
                    .eq(FMeasureData::getSampleKey, sample.getId())
                    .orderByDesc(FMeasureData::getSerialNumber)
                    .last("limit 1");
            FMeasureData data = fMeasureDataService.getOne(queryWrapper);
            if (data == null) {
                continue;
            }

            LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
            analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                    .eq(FMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                    .eq(FMeasureAnalysisData::getMeasureDataId, data.getId())
                    .orderByDesc(FMeasureAnalysisData::getDataVersion)
                    .last("limit 1");
            FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
            if (analysisData == null) {
                analysisData = new FMeasureAnalysisData();
            }

            if (StringUtils.hasText(analysisData.getAnalysisResult())) {
                List<FRationCountDataInfo> countDataInfoList = JSON.parseArray(analysisData.getAnalysisResult(), FRationCountDataInfo.class);
                for (FRationCountDataInfo dataInfo : countDataInfoList) {
                    if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                        abs = DataCountUtil.addZero(Double.valueOf(dataInfo.getColValue()), digit);
                    }
                    if (dataInfo.getIdentity().equals(SampleTableEnum.SUM_Abs.getIdentity())) {
                        sumAbs = DataCountUtil.addZero(Double.valueOf(dataInfo.getColValue()), digit);
                    }
                    if (dataInfo.getIdentity().equals(SampleTableEnum.FIT_CONCENTRATION.getIdentity())) {
                        countValue = dataInfo.getColValue() == null ? null : dataInfo.getColValue();
                        fitData = dataInfo.getOriginalValue() == null ? null : dataInfo.getOriginalValue();
                    }
                }
            }

            if (SampleTypeEnum.kind_stand.getIdentity().equals(kind)) {
                countValue = sample.getNormalConcentration();
            }

            if (StringUtils.hasText(sumAbs)) {
                measureValue = sumAbs;
            } else {
                measureValue = abs;
            }

            JSONObject point = new JSONObject();

            if (parameter.getEquationType().equals(RationCurveParamEnums.typeAbs.getIdentity())) {
                if (measureValue != null) {
                    String yVal = countValue;
                    if (!StringUtils.hasText(yVal) && fitting != null) {
                        Double xVal = null;
                        try {
                            xVal = Double.valueOf(measureValue);
                        } catch (Exception ignored) {}
                        if (xVal != null) {
                            String calc = valueCalculate(fitting, xVal, RationCurveParamEnums.typeAbs.getIdentity());
                            yVal = calc;
                        }
                    }
                    point.put("x", measureValue);
                    point.put("y", yVal);
                    if (fitData != null) {
                        point.put("xValue", measureValue);
                        point.put("yValue", yVal);
                        point.put("y", fitData);
                    }
                    if (SampleTypeEnum.kind_stand.getIdentity().equals(kind)) {
                        standPoints.add(point);
                    } else {
                        unknownPoints.add(point);
                    }
                }
            } else {
                if (measureValue != null || countValue != null) {
                    String xValStr = countValue;
                    if (!StringUtils.hasText(xValStr) && fitting != null && StringUtils.hasText(measureValue)) {
                        Double m;
                        try {
                            m = Double.valueOf(measureValue);
                        } catch (Exception e) {
                            m = null;
                        }
                        if (m != null) {
                            String calc = valueCalculate(fitting, m, RationCurveParamEnums.typeCurve.getIdentity());
                            xValStr = calc;
                        }
                    }
                    point.put("x", xValStr);
                    point.put("y", measureValue);
                    if (fitData != null) {
                        point.put("xValue", xValStr);
                        point.put("yValue", measureValue);
                        point.put("x", fitData);
                    }
                    if (SampleTypeEnum.kind_stand.getIdentity().equals(kind)) {
                        standPoints.add(point);
                    } else {
                        unknownPoints.add(point);
                    }
                }
            }
        }

        ans.put("unknown", unknownPoints);
        ans.put("stand", standPoints);
        return ans;
    }

    @Override
    public Result<FMeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType) {

        SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
        String id = po.getId();

        LambdaQueryWrapper<FMeasureTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FMeasureTask::getUserId, id)
                .eq(FMeasureTask::getMeasureType, measureType)
                .eq(FMeasureTask::getIsDelete, createType)
                .orderByDesc(FMeasureTask::getCreateTime);
        List<FMeasureTask> taskList = fMeasureTaskService.list(wrapper);
        if (taskList != null && !taskList.isEmpty()){
            FMeasureTask task = taskList.get(0);
            String methodKey = task.getMethodKey();
            FMeasureMethod method = fMeasureMethodService.getById(methodKey);
            FMeasurementCurrentInfoVO vo = new FMeasurementCurrentInfoVO();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getTaskName());
            vo.setMethodId(method.getId());
            vo.setMethodName(method.getMethodName());
            try {
                if (StringUtils.hasText(task.getZeroParam())) {
                    List<ChangeParam> params = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
                    if (params != null && !params.isEmpty()) {
                        params.forEach(param -> DetectionConstants.zeroResult.putIfAbsent(Double.valueOf(param.getWl()), param));
                    }
                }
            } catch (Exception e) {
                log.error("解析任务里的校零参数异常: {}", e.getMessage());
            }




            return Result.ok(vo);
        }

        FMeasurementCurrentInfoVO vo = new FMeasurementCurrentInfoVO();

        LambdaQueryWrapper<FMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureMethod::getMethodCreator, updateBy)
                .eq(FMeasureMethod::getMeasurementType, measureType)
                .orderByDesc(FMeasureMethod::getMethodCreateTime);
        List<FMeasureMethod> list = fMeasureMethodService.list(queryWrapper);
        if (list == null|| list.isEmpty()){
            queryWrapper.clear();
            queryWrapper.eq(FMeasureMethod::getProjectItemKey, "default")
                    .eq(FMeasureMethod::getMeasurementType, measureType)
                    .last("limit 1");
            FMeasureMethod one = fMeasureMethodService.getOne(queryWrapper);
            vo.setMethodId(one.getId());
            vo.setMethodName(one.getMethodName());


        } else {
            FMeasureMethod fMeasureMethod = list.get(0);

            vo.setMethodId(fMeasureMethod.getId());
            vo.setMethodName(fMeasureMethod.getMethodName());
        }
        FMeasureTask fMeasureTask = new FMeasureTask();


        fMeasureTask.setCreateTime(LocalDateTime.now());
        fMeasureTask.setMethodKey(vo.getMethodId());
        fMeasureTask.setMeasureType(measureType);
        fMeasureTask.setEndSaveTime(LocalDateTime.now());
        fMeasureTask.setIsSave(noSave);
        fMeasureTask.setIsDelete(createType);

        fMeasureTask.setUserId(id);
        fMeasureTask.setTaskName(vo.getMethodName()+newTaskName);

        boolean save = fMeasureTaskService.save(fMeasureTask);
        vo.setTaskId(fMeasureTask.getId());
        vo.setTaskName(fMeasureTask.getTaskName());
        return Result.ok(vo);
    }


    @Override
    public Result<List<JSONObject>> querySampleList(String taskId, String sampleKind) {
        List<JSONObject> sampleVOList = new ArrayList<>();
        if (!StringUtils.hasText(taskId)) {
            return Result.ok(sampleVOList);
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        if (StringUtils.hasText(task.getSampleKey())) {
            List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
            for (FTaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(deleteType)) {
                    continue;
                }
                FMeasureSample measureSample = fMeasureSampleService.getById(specimen.getSpecimenKey());
                if (!StringUtils.hasText(measureSample.getSampleKind()) || !measureSample.getSampleKind().equals(sampleKind)) {
                    continue;
                }
                FSampleVO vo = new FSampleVO();
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
    public Result<List<FHeadVO>> querySampleHeads(String taskId, String sampleKind) {

        if (!StringUtils.hasText(sampleKind)) {
            log.info("缺少样品种类");
            //缺少样品种类
            return Result.fail(YamlMessageSource.getMessageStatic("00000023"));
        }

        if (!StringUtils.hasText(taskId)) {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), sampleKind);
            return Result.ok(preHead);
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        if (!StringUtils.hasText(task.getTableInfo())) {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), sampleKind);
            return Result.ok(preHead);
        } else {
            FRationTaskHeadSnapShot snapShot = JSON.parseObject(task.getTableInfo(), FRationTaskHeadSnapShot.class);
            if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity())) {
                FTaskHeadSnapshot standHead = snapShot.getStandHead();
                if (standHead != null && standHead.getSampleHeads()!=null && !standHead.getSampleHeads().isEmpty()) {
                    return Result.ok(standHead.getSampleHeads());
                } else {
                    List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), sampleKind);
                    return Result.ok(preHead);
                }
            } else if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                FTaskHeadSnapshot unknownHead = snapShot.getUnknownHead();
                if (unknownHead != null && unknownHead.getSampleHeads()!=null && !unknownHead.getSampleHeads().isEmpty()) {
                    return Result.ok(unknownHead.getSampleHeads());
                } else {
                    List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), sampleKind);
                    return Result.ok(preHead);
                }
            } else {
                //样品种类出错
                return Result.fail(YamlMessageSource.getMessageStatic("00000024"));
            }
        }

    }


    @Override
//    @DS("concise")
//    @Transactional
    public Result addOrDeleteSample(FRationAddOrDeleteSampleDTO dto) {

        if (!StringUtils.hasText(dto.getMethodId())) {
            //没有方法！
            return Result.fail(YamlMessageSource.getMessageStatic("00000025"));
        }

        FMeasureMethod method = fMeasureMethodService.getById(dto.getMethodId());
        if (method == null) {
            //没有方法！
            return Result.fail(YamlMessageSource.getMessageStatic("00000025"));
        }

        if (dto.getUnknowHeads() == null || dto.getUnknowHeads().isEmpty()) {
            //未知样品表头为空
            return Result.fail(YamlMessageSource.getMessageStatic("00000027"));
        }

        if (dto.getUnknowHeads().size() > 10) {
            //未知样品表格新增列数不能超过10列
            return Result.fail(YamlMessageSource.getMessageStatic("00000029"));
        }

        FRationParameter rationParameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);
        boolean standHas = false;
        if (StringUtils.hasText(rationParameter.getCoefficientMode()) && rationParameter.getCoefficientMode().equals(RationCurveParamEnums.standard_curve.getIdentity())) {
            standHas = true;
            if (dto.getStandHeads() == null || dto.getStandHeads().isEmpty()) {
                //标准样品表头为空
                return Result.fail(YamlMessageSource.getMessageStatic("00000026"));
            }

            if (dto.getStandHeads().size() > 10) {
                //标准样品表格新增列数不能超过10列
                return Result.fail(YamlMessageSource.getMessageStatic("00000028"));
            }
            /// 校验是否有重复的样品编号
            // 取出所有样品编号
            List<String> sampleNos = dto.getNewStandSamples().stream()
                    .map(m -> JSON.parseObject(JSON.toJSONString(m), FSampleVO.class).getSampleNumber()) // 取出 sampleNo
                    .filter(Objects::nonNull)    // 过滤掉空值
                    .collect(Collectors.toList());
            // 查重：找到重复的样品编号
            Set<String> duplicates = sampleNos.stream()
                    .filter(no -> Collections.frequency(sampleNos, no) > 1)
                    .collect(Collectors.toSet());
            // 判断如果isTested为true，
            if (!duplicates.isEmpty()) {
                //标准样品编号重复
                return Result.fail(YamlMessageSource.getMessageStatic("00102001"));
            }
        }

        List<String> unknownSampleNos = dto.getNewUnknowSamples().stream()
                .map(m -> JSON.parseObject(JSON.toJSONString(m), FSampleVO.class).getSampleNumber()) // 取出 sampleNo
                .filter(Objects::nonNull)    // 过滤掉空值
                .collect(Collectors.toList());
        // 查重：找到重复的样品编号
        Set<String> isDuplicates = unknownSampleNos.stream()
                .filter(no -> Collections.frequency(unknownSampleNos, no) > 1)
                .collect(Collectors.toSet());
        // 判断如果isTested为true，
        if (!isDuplicates.isEmpty()) {
            //未知样品编号重复
            return Result.fail(YamlMessageSource.getMessageStatic("00102002"));
        }

        if (!StringUtils.hasText(dto.getTaskId())) {
            //创建任务
            FMeasureTask newTask = new FMeasureTask();

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
            newTask.setEndSaveTime(LocalDateTime.now());
            boolean save = false;
            for (int i = 0; i < 3; i++) {
                if (fMeasureTaskService.save(newTask)) {
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

        FMeasureTask task = fMeasureTaskService.getById(dto.getTaskId());
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        List<FTaskSpecimen> taskSpecimen = new ArrayList<>();


        if (StringUtils.hasText(task.getSampleKey())) {
            taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        }

        if (taskSpecimen == null) {

            taskSpecimen = new ArrayList<>();
        }
        Map<String, FTaskSpecimen> oldMap = new HashMap<>();

        for (FTaskSpecimen specimen : taskSpecimen) {
            if (dto.getDelSampleIds().contains(specimen.getSpecimenKey())) {
                specimen.setDeleteType(deleteType);
                LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(FMeasureData::getTaskId, task.getId()).orderByDesc(FMeasureData::getSerialNumber).last("limit 1");
                FMeasureData one = fMeasureDataService.getOne(queryWrapper);
                if (one != null) {
                    one.setIsDelete(deleteType);
                    one.setIsSave(noSave);
                    fMeasureDataService.updateById(one);
                }
            }
            oldMap.putIfAbsent(specimen.getSpecimenKey(), specimen);
        }

        FRationTaskHeadSnapShot taskHeadSnapShot = new FRationTaskHeadSnapShot();
        if (StringUtils.hasText(task.getTableInfo())) {
            taskHeadSnapShot = JSON.parseObject(task.getTableInfo(), FRationTaskHeadSnapShot.class);
        }

        /*
        处理表头
         */
        if (standHas) {
            if (dto.getStandHeads() != null && !dto.getStandHeads().isEmpty()) {
                FTaskHeadSnapshot standHead = new FTaskHeadSnapshot();
                if (taskHeadSnapShot.getStandHead() != null) {
                    BeanUtils.copyProperties(taskHeadSnapShot.getStandHead(), standHead);
                }
                standHead.setTaskId(task.getId());
                standHead.setSampleHeads(dto.getStandHeads());
                taskHeadSnapShot.setStandHead(standHead);
            }
        }

        if (dto.getUnknowHeads() != null && !dto.getUnknowHeads().isEmpty()) {
            FTaskHeadSnapshot unknownHead = new FTaskHeadSnapshot();
            if (taskHeadSnapShot.getUnknownHead() != null) {
                BeanUtils.copyProperties(taskHeadSnapShot.getUnknownHead(), unknownHead);
            }
            unknownHead.setTaskId(task.getId());
            unknownHead.setSampleHeads(dto.getUnknowHeads());
            taskHeadSnapShot.setUnknownHead(unknownHead);
        }
        task.setTableInfo(JSON.toJSONString(taskHeadSnapShot));
        fMeasureTaskService.updateById(task);
        task = fMeasureTaskService.getById(task.getId());
        Integer digit = fMeasureTaskAttachmentService.queryLastDigit(task.getId());
        digit = digit == null ? Constants.DEFAULT_DECIMAL_NUM : digit;

        if (standHas) {
            for (Map<String, String> map : dto.getNewStandSamples()) {

                String jsonString = JSON.toJSONString(map);
                FSampleVO sample = JSON.parseObject(jsonString, FSampleVO.class);

                FMeasureSample newSample = new FMeasureSample();
                BeanUtils.copyProperties(sample, newSample);
                newSample.setId(sample.getSampleId());
                newSample.setSampleKind(stand);
                if (dto.getStandHeads() != null && !dto.getStandHeads().isEmpty()) {
                    JSONObject newColData = new JSONObject();

                    for (FHeadVO head : dto.getStandHeads()) {
                        if (head.getLocate().equals(TableHeadILocateEnums.system.getIdentity())) {
                            continue;
                        }
                        String s = map.get(head.getIdentity());
                        newColData.put(head.getIdentity(), s);
                    }

                    newSample.setHeads(JSON.toJSONString(dto.getStandHeads()));
                    newSample.setNewColData(JSON.toJSONString(newColData));
                }

                if (StringUtils.hasText(newSample.getId())) {
                    fMeasureSampleService.updateById(newSample);
                    FTaskSpecimen specimen = oldMap.get(newSample.getId());
                    specimen.setSampleKind(stand);
                    specimen.setSampleType(newSample.getSampleType());
                    oldMap.put(newSample.getId(), specimen);

                } else {
                    FTaskSpecimen specimen = new FTaskSpecimen();
                    specimen.setDeleteType(createType);
                    specimen.setIsTested(noTested);
                    specimen.setSampleType(newSample.getSampleType());
                    specimen.setSampleKind(stand);
                    log.info("新增的标准样品：{}", newSample);
                    fMeasureSampleService.save(newSample);
                    specimen.setSpecimenKey(newSample.getId());
                    oldMap.put(newSample.getId(), specimen);
                    taskSpecimen.add(specimen);
                }
            }
        }

        for (Map<String, String> map : dto.getNewUnknowSamples()) {

            String jsonString = JSON.toJSONString(map);
            FSampleVO sample = JSON.parseObject(jsonString, FSampleVO.class);

            FMeasureSample newSample = new FMeasureSample();
            BeanUtils.copyProperties(sample, newSample);
            newSample.setId(sample.getSampleId());
            newSample.setSampleKind(unknown);

            if (dto.getUnknowHeads() != null && !dto.getUnknowHeads().isEmpty()) {
                JSONObject newColData = new JSONObject();

                for (FHeadVO head : dto.getUnknowHeads()) {
                    if (head.getLocate().equals(TableHeadILocateEnums.system.getIdentity())) {
                        continue;
                    }
                    String s = map.get(head.getIdentity());
                    newColData.put(head.getIdentity(), s);
                }

                newSample.setHeads(JSON.toJSONString(dto.getUnknowHeads()));
                newSample.setNewColData(JSON.toJSONString(newColData));
            }

            if (StringUtils.hasText(newSample.getId())) {
                fMeasureSampleService.updateById(newSample);
                FTaskSpecimen specimen = oldMap.get(newSample.getId());
                specimen.setSampleKind(unknown);
                specimen.setSampleType(newSample.getSampleType());
                oldMap.put(newSample.getId(), specimen);
            } else {
                FTaskSpecimen specimen = new FTaskSpecimen();
                specimen.setDeleteType(createType);
                specimen.setIsTested(noTested);
                specimen.setSampleKind(unknown);
                specimen.setSampleType(newSample.getSampleType());
                log.info("新增的未知样品：{}", newSample);
                fMeasureSampleService.save(newSample);
                specimen.setSpecimenKey(newSample.getId());
                oldMap.put(newSample.getId(), specimen);
                taskSpecimen.add(specimen);
            }
        }

        for (FTaskSpecimen specimen : taskSpecimen) {
            specimen = oldMap.get(specimen.getSpecimenKey()) == null ? specimen : oldMap.get(specimen.getSpecimenKey());
        }

        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);
        boolean b = fMeasureTaskService.updateById(task);

        if (!b) {
            //任务保存样品信息出错
            return Result.fail(YamlMessageSource.getMessageStatic("00100003"));
        }
        //重新分析
        if (standHas) {
            changeStandSampleDataDigit(task.getId(), digit);
        }
        changeUnknowSampleDigit(task.getId(), digit);

        if (StringUtils.hasText(dto.getDeviceSerialNumber())) {
            R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(dto.getDeviceSerialNumber());
            if (!poolInfo.getSuccess()) {
                return Result.ok();
            }
            FSamplePoolInfoVO poolInfoVO = poolInfo.getData();
            Boolean isAutoChangePool = poolInfoVO.getIsAutoChangePool();
            if (isAutoChangePool) {
                List<Integer> usefulLocation = fSamplePoolConfigService.getUsefulLocation(dto.getDeviceSerialNumber());
                concisePublicUtilService.changeSampleLocation(task.getId(), usefulLocation);
            }
        }

        //公式重算

        return Result.ok();
    }

    @Override
    public Result<Boolean> deleteSample(String taskId, String sampleId) {

        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(sampleId)) {
            //请选择需要删除的任务和样品
            return Result.fail(YamlMessageSource.getMessageStatic("00000015"));
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            //任务无样品
            return Result.fail(YamlMessageSource.getMessageStatic("00000016"));
        }
        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        for (FTaskSpecimen specimen : taskSpecimen) {
            if (sampleId.equals(specimen.getSpecimenKey())) {
                specimen.setDeleteType(deleteType);
                LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(FMeasureData::getTaskId, task.getId())
                        .orderByDesc(FMeasureData::getSerialNumber).last("limit 1");
                FMeasureData one = fMeasureDataService.getOne(queryWrapper);
                if (one != null) {
                    one.setIsDelete(deleteType);
                    one.setIsSave(noSave);
                    fMeasureDataService.updateById(one);
                }
            }
        }

        task.setSampleKey(JSON.toJSONString(taskSpecimen));
        boolean b = fMeasureTaskService.updateById(task);
        if (!b) {
            //删除失败
            return Result.fail(YamlMessageSource.getMessageStatic("00100008"));
        }

        FMeasureSample measureSample = fMeasureSampleService.getById(sampleId);

        Integer i = fMeasureTaskAttachmentService.queryLastDigit(taskId);
        Integer digit = i == null ? Constants.DEFAULT_DECIMAL_NUM : i;

        if (measureSample.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
            changeStandSampleDataDigit(taskId, digit);
            changeUnknowSampleDigit(taskId, digit);
        } else {
            changeUnknowSampleDigit(taskId, digit);
        }
        return Result.ok(true);
    }

    @Override
    public Result revokeDel(String taskId, String sampleId) {
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        String sampleKey = task.getSampleKey();

        FRationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), FRationTaskHeadSnapShot.class);

        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(sampleKey, FTaskSpecimen.class);
        FMeasureSample newSample = fMeasureSampleService.getById(sampleId);
        Boolean b = revokeDelSampleNumRepeat(taskSpecimen, newSample);
        if (!b) {
            //要恢复的样品编号重复，请处理重复编号后再恢复！
            return Result.fail(YamlMessageSource.getMessageStatic("00100004"));
        }


        for (FTaskSpecimen specimen : taskSpecimen) {
            if (specimen.getSpecimenKey().equals(sampleId)) {
                specimen.setDeleteType(createType);

                if (shot != null && specimen.getSampleKind() != null) {
                    if (specimen.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {

                        if (shot.getUnknownHead() != null && shot.getUnknownHead().getSampleHeads() != null) {
                            newSample.setHeads(JSON.toJSONString(shot.getUnknownHead().getSampleHeads()));
                        }
                    } else if (specimen.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                        if (shot.getStandHead() != null && shot.getStandHead().getSampleHeads() != null) {
                            newSample.setHeads(JSON.toJSONString(shot.getStandHead().getSampleHeads()));
                        }
                    }
                }

                fMeasureSampleService.updateById(newSample);

                LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(FMeasureData::getTaskId, task.getId())
                        .orderByDesc(FMeasureData::getSerialNumber).last("limit 1");
                FMeasureData one = fMeasureDataService.getOne(queryWrapper);
                if (one != null) {
                    one.setIsDelete(createType);
                    one.setIsSave(saved);
                    fMeasureDataService.updateById(one);
                }
            }
        }
        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);

        fMeasureTaskService.updateById(task);

        FMeasureSample measureSample = fMeasureSampleService.getById(sampleId);

        Integer i = fMeasureTaskAttachmentService.queryLastDigit(taskId);
        Integer digit = i == null ? Constants.DEFAULT_DECIMAL_NUM : i;

        if (measureSample.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
            changeStandSampleDataDigit(taskId, digit);
            changeUnknowSampleDigit(taskId, digit);
        } else {
            changeUnknowSampleDigit(taskId, digit);
        }


        return Result.ok();
    }

    private Boolean revokeDelSampleNumRepeat(List<FTaskSpecimen> taskSpecimen, FMeasureSample sample) {

        String sampleKind = sample.getSampleKind();
        List<String> sampleNumList = new ArrayList<>();

        for (FTaskSpecimen specimen : taskSpecimen) {
            if (!specimen.getSampleKind().equals(sampleKind)) {
                continue;
            }
            if (specimen.getDeleteType().equals(deleteType) || specimen.getSpecimenKey().equals(sample.getId())) {
                continue;
            }
            FMeasureSample measureSample = fMeasureSampleService.getById(specimen.getSpecimenKey());
            if (measureSample != null && StringUtils.hasText(measureSample.getSampleNumber())){
                sampleNumList.add(measureSample.getSampleNumber());
            }

        }
        if (sampleNumList.contains(sample.getSampleNumber())) {
            return false;
        }
        return true;

    }

    @Override
    public Result<FRationMethodParam> queryMethodParam(String methodId) {
        if (!StringUtils.hasText(methodId)) {
            //请先创建方法或者选择方法
            return Result.fail(YamlMessageSource.getMessageStatic("0010000B"));
        }

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }

        if (!StringUtils.hasText(method.getMeasurementParamInfo())) {
            //未查询到该方法参数
            return Result.fail(YamlMessageSource.getMessageStatic("00000017"));
        }
        FRationParameter rationParameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);

        FRationMethodParam param = new FRationMethodParam();
        BeanUtils.copyProperties(rationParameter, param);
        param.setMethodId(method.getId());
        param.setMethodName(method.getMethodName());
        param.setMeasurementType(method.getMeasurementType());
        param.setCoefficientModeName(RationCurveParamEnums.getName(param.getCoefficientMode()));
        return Result.ok(param);
    }

    @Override
    public Result<Boolean> saveButton(String taskId, String updateBy) {

        if (!StringUtils.hasText(taskId)) {
            //未创建任务，无法保存
            return Result.fail(YamlMessageSource.getMessageStatic("0000001F"));
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
        String id = po.getId();
        task.setUserId(id);
        task.setEndSaveTime(LocalDateTime.now());
        fMeasureTaskService.updateById(task);
        saveData(taskId);
        return Result.ok(true);
    }

    //标准样品表头
    @Override
    public Result<List<FHeadVO>> getStandSampleTableHead(String taskId, String methodId, Boolean allDisplay, String deviceSerialNumber) {

        if (!StringUtils.hasText(taskId)) {
            //请选择任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000006"));
        }
        if (!StringUtils.hasText(methodId)){
            //请选择方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000002"));
        }
        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        if (!StringUtils.hasText(deviceSerialNumber)) {
            List<Integer> usefulLocation = fSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
        }
        LinkedHashSet<FHeadVO> ans = new LinkedHashSet<>();

        FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);

        //根据任务中记录的样品id判断是否存在空白样品
        FMeasureTask task = fMeasureTaskService.getById(taskId);

        if (task == null) {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_stand.getIdentity());
            ans.addAll(preHead);
            return Result.ok(new ArrayList<>(ans));
        }

        if (StringUtils.hasText(task.getTableInfo())) {
            FRationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), FRationTaskHeadSnapShot.class);
            if (shot != null && shot.getStandHead() != null) {
                if (shot.getStandHead().getTaskHeads() != null) {
                    ans.addAll(shot.getStandHead().getTaskHeads());
                }
                if (shot.getStandHead().getSampleHeads() != null) {
                    ans.addAll(shot.getStandHead().getSampleHeads());
                }
            }
        } else {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_stand.getIdentity());
            ans.addAll(preHead);
        }

        boolean empty = false;
        if (StringUtils.hasText(task.getSampleKey())) {
            List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
            for (FTaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(deleteType) || !specimen.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                    continue;
                }
                FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());

                if (StringUtils.hasText(sample.getSampleType())) {
                    if (sample.getSampleType().equals(SampleTypeEnum.empty.getIdentity())){
                        empty = true;
                    }
                }
                if (StringUtils.hasText(sample.getHeads())){
                    List<FHeadVO> heads = JSON.parseArray(sample.getHeads(), FHeadVO.class);
                    if (heads != null && !heads.isEmpty()) {
                        ans.addAll(heads);
                    }
                }
            }
        }
        List<FHeadVO> result = dealHeads(new ArrayList<>(ans), parameter, SampleTypeEnum.kind_stand.getIdentity(), allDisplay, empty, deviceSerialNumber);

        System.out.println("111111");
        return Result.ok(result);

    }

    private List<FHeadVO> dealHeads(List<FHeadVO> heads, FRationParameter parameter, String sampleKind, Boolean allDisplay, Boolean empty, String deviceSerialNumber) {
        List<String> waveLength = parameter.getWaveLength();
        Map<String, FHeadVO> map = heads.stream().collect(Collectors.toMap(FHeadVO::getIdentity, head -> head));

        Map<String, List<FHeadVO>> headsMap = new HashMap<>();

        Map<String, FHeadVO> analyzeMap = new HashMap<>();

        for (FHeadVO head : heads) {
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

        List<FHeadVO> allMeasureHeads = new ArrayList<>();

        for (String s : waveLength) {
            String absIdentity = s + "-"+ WaveLengthEnum.abs.getCode();
            String tPercentIdentity = s + "-"+ WaveLengthEnum.tPercent.getCode();
            String absName = WaveLengthEnum.abs.getName() +  "[" + s + "nm]";
            String tPercentName = WaveLengthEnum.tPercent.getName() +  "[" + s + "nm]";

            FHeadVO absHead = new FHeadVO(absName, absIdentity, TableHeadILocateEnums.measure.getIdentity(), absName);
            FHeadVO tPercentHead = new FHeadVO(tPercentName, tPercentIdentity, TableHeadILocateEnums.measure.getIdentity(), tPercentName);

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

        String sumAbsIdentity = SampleTableEnum.SUM_Abs.getIdentity();
        String absIdentity = SampleTableEnum.Abs.getIdentity();
        FHeadVO newHead = new FHeadVO(SampleTableEnum.Abs.getName(), absIdentity, TableHeadILocateEnums.measure.getIdentity(), SampleTableEnum.Abs.getName());
        FHeadVO sumAbsHead = new FHeadVO(SampleTableEnum.SUM_Abs.getName(), sumAbsIdentity, TableHeadILocateEnums.measure.getIdentity(), SampleTableEnum.SUM_Abs.getName());

        if (map.containsKey(absIdentity)) {
            newHead = map.get(absIdentity);
        }
        allMeasureHeads.add(newHead);

        if (empty) {
            if (map.containsKey(sumAbsIdentity)) {
                sumAbsHead = map.get(sumAbsIdentity);
            }
            allMeasureHeads.add(sumAbsHead);
        }
        List<FHeadVO> ans = new ArrayList<>();
        List<FHeadVO> systemHeads = headsMap.get(TableHeadILocateEnums.system.getIdentity());
        if (systemHeads != null) ans.addAll(systemHeads);
        for (FHeadVO an : ans) {
            if (an.getIdentity().equals(SampleTableEnum.SAMPLE_LOCATION.getIdentity())) {
                ans.remove(an);
                break;
            }

            if (an.getIdentity().equals(SampleTableEnum.NORMAL_CONCENTRATION.getIdentity()) && StringUtils.hasText(parameter.getUnit())) {
                an.setName(an.getName() + "(" + parameter.getUnit() + ")");
                break;
            }
        }

        if (StringUtils.hasText(deviceSerialNumber)) {

            R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
            if (poolInfo.getSuccess()) {
                FSamplePoolInfoVO data = poolInfo.getData();
                if (data.getIsAutoChangePool() ) {
                    String sampleLocationName = SampleTableEnum.SAMPLE_LOCATION.getName();
                    String sampleLocationIdentity = SampleTableEnum.SAMPLE_LOCATION.getIdentity();
                    FHeadVO sampleLocation = new FHeadVO(sampleLocationName, sampleLocationIdentity, TableHeadILocateEnums.location.getIdentity(), sampleLocationName);
                    ans.add(sampleLocation);
                }
            }
        }

        List<FHeadVO> sampleHeads = headsMap.get(TableHeadILocateEnums.sample.getIdentity());
        if (sampleHeads != null) ans.addAll(sampleHeads);

        ans.addAll(allMeasureHeads);

        List<FHeadVO> taskHeads = headsMap.get(TableHeadILocateEnums.data.getIdentity());
        if (taskHeads != null)ans.addAll(taskHeads);

        if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
            String fitConcentrationName = SampleTableEnum.FIT_CONCENTRATION.getName();
            String fitConcentrationIdentity = SampleTableEnum.FIT_CONCENTRATION.getIdentity();

            if (StringUtils.hasText(parameter.getUnit())) {
                fitConcentrationName = fitConcentrationName + "(" + parameter.getUnit() + ")";
            }

            FHeadVO fitHead = new FHeadVO(fitConcentrationName, SampleTableEnum.FIT_CONCENTRATION.getIdentity(), TableHeadILocateEnums.analyze.getIdentity(), fitConcentrationName);
            if (analyzeMap.containsKey(fitConcentrationIdentity)) {

                FHeadVO fHeadVO = analyzeMap.get(fitConcentrationIdentity);
                fitHead.setPrintTitle(fHeadVO.getPrintTitle());
            }
            ans.add(fitHead);

            List<FPreCountResultParam> resultParams = parameter.getResultParams();
            List<FHeadVO> headAnalyze = new ArrayList<>();
            if (resultParams == null) {
                resultParams = new ArrayList<>();
            }
            for (FPreCountResultParam results : resultParams) {
                //TODO 方法参数标识待定，后续再改动
                if (!StringUtils.hasText(results.getParam())) {
                    results.setParam(results.getResultIdentity() + "-" + results.getFormulaText());
                }
                String name = results.getResultName() + "(" + results.getUnit() + ")";

                FHeadVO analyzeHead = new FHeadVO(name, results.getParam(), TableHeadILocateEnums.analyze.getIdentity(), name);

                if (analyzeMap.containsKey(results.getParam())) {
                    analyzeHead = analyzeMap.get(results.getParam());
                }
                headAnalyze.add(analyzeHead);
            }
            ans.addAll(headAnalyze);
        }
        List<FHeadVO> other = headsMap.get(TableHeadILocateEnums.other.getIdentity());
        if (other == null || other.isEmpty()) {
            other = new ArrayList<>();
            FHeadVO samplingTimeHead = new FHeadVO(SampleTableEnum.SAMPLING_TIME.getName(), SampleTableEnum.SAMPLING_TIME.getIdentity(), TableHeadILocateEnums.other.getIdentity(), SampleTableEnum.SAMPLING_TIME.getName());
            other.add(samplingTimeHead);
        }
        ans.addAll(other);
        List<FHeadVO> remarks = headsMap.get(TableHeadILocateEnums.remark.getIdentity());
        if (remarks == null || remarks.isEmpty()) {
            remarks = new ArrayList<>();
            FHeadVO remark = new FHeadVO(SampleTableEnum.REMARK.getName(), SampleTableEnum.REMARK.getIdentity(), TableHeadILocateEnums.remark.getIdentity(), SampleTableEnum.REMARK.getName());
            remarks.add(remark);
        }
        ans.addAll(remarks);

        return ans;
    }


    //未知样品表头
    @Override
    public Result<List<FHeadVO>> getUnknownSampleTableHead(String taskId, String methodId, Boolean allDisplay, String deviceSerialNumber) {

        if (!StringUtils.hasText(taskId) ) {
            //请选择任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000006"));
        }
        if (!StringUtils.hasText(methodId)){
            //请选择方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000002"));
        }

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }

        LinkedHashSet<FHeadVO> ans = new LinkedHashSet<>();
        //根据任务中记录的样品id判断是否存在空白样品

        FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);

        FMeasureTask task = fMeasureTaskService.getById(taskId);

        if (task == null) {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_unknown.getIdentity());
            ans.addAll(preHead);
            return Result.ok(new ArrayList<>(ans));
        }

        if (StringUtils.hasText(task.getTableInfo())) {
            FRationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), FRationTaskHeadSnapShot.class);
            if (shot != null && shot.getUnknownHead() != null) {

                if (shot.getUnknownHead().getTaskHeads() != null) {
                    ans.addAll(shot.getUnknownHead().getTaskHeads());
                }
                if (shot.getUnknownHead().getSampleHeads() != null) {
                    ans.addAll(shot.getUnknownHead().getSampleHeads());
                }
            }
        }else {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_unknown.getIdentity());
            ans.addAll(preHead);
        }

        boolean empty = false;
        if (StringUtils.hasText(task.getSampleKey())) {
            List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
            for (FTaskSpecimen specimen : taskSpecimen) {
                FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());

                if(sample == null || !SampleTypeEnum.kind_unknown.getIdentity().equals(sample.getSampleKind())) {
                    log.info("跳过该样品");
                    continue;
                }
                if (StringUtils.hasText(sample.getSampleType())) {
                    if (sample.getSampleType().equals(SampleTypeEnum.empty.getIdentity())){
                        empty = true;
                    }
                }
                if (StringUtils.hasText(sample.getHeads())){
                    List<FHeadVO> heads = JSON.parseArray(sample.getHeads(), FHeadVO.class);
                    if (heads != null && !heads.isEmpty()) {
                        ans.addAll(heads);
                    }
                }
            }
        }
        List<FHeadVO> result = dealHeads(new ArrayList<>(ans), parameter, SampleTypeEnum.kind_unknown.getIdentity(), allDisplay, empty, deviceSerialNumber);
        return Result.ok(result);
    }

    @Override
    public R<Boolean> checkAllSampleTest(String taskId) {

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            return R.ok(true);
        }
        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        if (taskSpecimen == null || taskSpecimen.isEmpty()) {
            return R.ok(true);
        }
        boolean allTested = true;
        for (FTaskSpecimen specimen : taskSpecimen) {
            if(specimen.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                if (specimen.getIsTested().equals(noTested) && specimen.getDeleteType().equals(createType)) {
                    allTested = false;
                    break;
                }
            }
        }

        return R.ok(allTested);
    }

    @Override
    public R<Boolean> createSample(String taskId, String sampleKind, String normalConcentration, String updateBy, String deviceSerialNumber) {
        FMeasureTask task = fMeasureTaskService.getById(taskId);

        if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity()) && normalConcentration == null) {
            return R.fail(YamlMessageSource.getMessageStatic("00102024"));
        }

        if (task == null) {
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        if (taskSpecimen == null) {
            taskSpecimen = new ArrayList<>();
        }
        Result<List<FHeadVO>> listResult = querySampleHeads(taskId, sampleKind);
        if (!listResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode())){
            return R.fail(listResult.getMessage());
        }
        Integer sampleAutoNum = task.getSampleAutoNum();
        if (sampleAutoNum == null) {
            sampleAutoNum = 0;
        }

        List<FHeadVO> heads = listResult.getData();
        //动态表头
        List<FHeadVO> dynamicHeads = new ArrayList<>();
        heads.forEach(head -> {
            if (head.getLocate().equals(TableHeadILocateEnums.sample.getIdentity())){
                dynamicHeads.add(head);
            }
        });
        boolean isAutoChangePool = false;

        R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
        if (poolInfo.getSuccess()) {
            FSamplePoolInfoVO poolInfoVO = poolInfo.getData();
            isAutoChangePool = poolInfoVO.getIsAutoChangePool();

        }

        if (taskSpecimen.isEmpty()) {
            FMeasureSample newSample = new FMeasureSample();

            sampleAutoNum = 1;
            newSample.setSampleName("默认样品" + 1);
            newSample.setSampleNumber("default-" + 1);

            if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                newSample.setSampleType(SampleTypeEnum.unKnown.getIdentity());
            } else {
                newSample.setSampleType(SampleTypeEnum.stand.getIdentity());
                newSample.setNormalConcentration(String.valueOf(normalConcentration));
            }

            newSample.setSampleKind(sampleKind);
            if (!dynamicHeads.isEmpty()) {
                newSample.setHeads(JSON.toJSONString(dynamicHeads));
            }
            boolean save = fMeasureSampleService.save(newSample);
            if (!save) {
                //保存失败
                return R.fail(YamlMessageSource.getMessageStatic("00300008"));
            }

            taskSpecimen.add(new FTaskSpecimen(newSample.getId(), createType, noTested, newSample.getSampleType(), sampleKind));

        } else {
            boolean hasNoTested = false;
            List<String> sampleNumList = new ArrayList<>();

            double cou = 0;
            double last = 0;

            for (FTaskSpecimen specimen : taskSpecimen) {
                if (!specimen.getSampleKind().equals(sampleKind) || specimen.getDeleteType().equals(deleteType)) {
                    continue;
                }
                if (specimen.getIsTested().equals(noTested)) {
                    hasNoTested = true;
                    break;
                }
                FMeasureSample measureSample = fMeasureSampleService.getById(specimen.getSpecimenKey());
                if (measureSample != null && StringUtils.hasText(measureSample.getSampleName())){
                    sampleNumList.add(measureSample.getSampleNumber());
                    if (StringUtils.hasText(measureSample.getNormalConcentration())) {
                        cou =  Double.parseDouble(measureSample.getNormalConcentration()) - last;
                        last = Double.parseDouble(measureSample.getNormalConcentration());
                    }
                }
            }
            if (hasNoTested) {
                return R.ok(true);
            }

            FMeasureSample newSample = new FMeasureSample();
            if (cou == 0) {
                cou = 1;
            }

            sampleAutoNum = 1;
            while (sampleNumList.contains("default-"+ sampleAutoNum)) {
                sampleAutoNum += 1;
            }

            int num = sampleAutoNum;

            sampleNumList.add("default-" + num);
            newSample.setSampleNumber("default-" + num);
            newSample.setSampleName("默认样品" + num);
            List<Integer> usefulLocation = fSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
            int all = usefulLocation.size();

            if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                newSample.setSampleType(SampleTypeEnum.unKnown.getIdentity());
            } else {
                newSample.setSampleType(SampleTypeEnum.stand.getIdentity());
                newSample.setNormalConcentration(String.valueOf(normalConcentration));
            }
            newSample.setSampleKind(sampleKind);
            if (!dynamicHeads.isEmpty()) {
                newSample.setHeads(JSON.toJSONString(dynamicHeads));
            }
            boolean save = fMeasureSampleService.save(newSample);
            if (!save) {
                //保存失败
                return R.fail(YamlMessageSource.getMessageStatic("00300008"));
            }
            taskSpecimen.add(new FTaskSpecimen(newSample.getId(), createType, noTested, newSample.getSampleType(), sampleKind));


            if (isAutoChangePool) {
                for (int i = 1; i < all; i++) {
                    newSample.setId(null);
                    sampleAutoNum ++;

                    while (sampleNumList.contains("default-"+ sampleAutoNum)) {
                        sampleAutoNum += 1;
                    }

                    int newNum = sampleAutoNum;
                    sampleNumList.add("default-" + newNum);
                    newSample.setSampleNumber("default-" + newNum);
                    newSample.setSampleName("默认样品" + newNum);

                    if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity())) {
                        normalConcentration = String.valueOf(Double.parseDouble(normalConcentration) + cou);

                        newSample.setNormalConcentration(normalConcentration);
                    }
                    save = fMeasureSampleService.save(newSample);
                    if (!save) {
                        //保存失败
                        return R.fail(YamlMessageSource.getMessageStatic("00300008"));
                    }
                    taskSpecimen.add(new FTaskSpecimen(newSample.getId(), createType, noTested, newSample.getSampleType(), sampleKind));
                }
            }
        }
        task.setSampleAutoNum(sampleAutoNum);
        task.setSampleKey(JSON.toJSONString(taskSpecimen));
        boolean update = fMeasureTaskService.updateById(task);
        if (!update) {
            //更新失败
            return R.fail(YamlMessageSource.getMessageStatic("00000048"));
        }

        if (isAutoChangePool) {
            List<Integer> usefulLocation = fSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
            concisePublicUtilService.changeSampleLocation(task.getId(), usefulLocation);
        }

        return R.ok(true);
    }

    @Override
    public R<String> saveRationSampleMeasureData(FRationDataVO dataInfo) {

        String sampleId = dataInfo.getSampleId();
        String taskId = dataInfo.getTaskId();

        LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureData::getTaskId, taskId)
                .eq(FMeasureData::getSampleKey, sampleId)
                .orderByDesc(FMeasureData::getSerialNumber)
                .last("limit 1");
        FMeasureData data = fMeasureDataService.getOne(queryWrapper);

        FMeasureData fMeasureData = new FMeasureData();
        fMeasureData.setTaskId(taskId);
        fMeasureData.setSampleKey(sampleId);
        fMeasureData.setInstrumentData(JSON.toJSONString(dataInfo.getMachineData()));
        fMeasureData.setOriginalCalculationData(JSON.toJSONString(dataInfo.getWavelengthData()));
        fMeasureData.setMeasurementStartTime(dataInfo.getStartTime());
        fMeasureData.setMeasurementEndTime(dataInfo.getEndTime());
        fMeasureData.setSamplingTime(dataInfo.getSampleTime());

        if (data != null) {
            fMeasureData.setSerialNumber(data.getSerialNumber() + 1);
        } else {
            fMeasureData.setSerialNumber(0);
        }
        fMeasureData.setIsDelete(createType);
        fMeasureData.setIsSave(noSave);
        fMeasureDataService.save(fMeasureData);

        LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                .eq(FMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(FMeasureAnalysisData::getDataVersion)
                .last("limit 1");
        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (analysisData != null && analysisData.getMeasureDataId() == null) {

            analysisData.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            analysisData.setMeasureDataId(fMeasureData.getId());
            analysisData.setIsDelete(createType);
            fMeasureAnalysisDataService.updateById(analysisData);

        } else {
            analysisData = new FMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setMeasureDataId(fMeasureData.getId());
            analysisData.setDataVersion(0);
            analysisData.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            analysisData.setIsDelete(createType);
            analysisData.setIsSave(noSave);
            fMeasureAnalysisDataService.save(analysisData);
        }


        return R.ok(fMeasureData.getId());

    }

    @Override
    public R<FRationSaveCountDataInfo> saveSampleMeasureData(FRationDataVO dataInfo) {
        String sampleId = dataInfo.getSampleId();
        String taskId = dataInfo.getTaskId();

        LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureData::getTaskId, taskId)
                .eq(FMeasureData::getSampleKey, sampleId)
                .orderByDesc(FMeasureData::getSerialNumber)
                .last("limit 1");
        FMeasureData data = fMeasureDataService.getOne(queryWrapper);

        FMeasureData fMeasureData = new FMeasureData();
        fMeasureData.setTaskId(taskId);
        fMeasureData.setSampleKey(sampleId);
        fMeasureData.setInstrumentData(JSON.toJSONString(dataInfo.getMachineData()));
        fMeasureData.setOriginalCalculationData(JSON.toJSONString(dataInfo.getWavelengthData()));
        fMeasureData.setMeasurementStartTime(dataInfo.getStartTime());
        fMeasureData.setMeasurementEndTime(dataInfo.getEndTime());
        fMeasureData.setSamplingTime(dataInfo.getSampleTime());

        if (data != null) {
            fMeasureData.setSerialNumber(data.getSerialNumber() + 1);
        } else {
            fMeasureData.setSerialNumber(0);
        }
        fMeasureData.setIsDelete(createType);
        fMeasureData.setIsSave(noSave);
        fMeasureDataService.save(fMeasureData);

        LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                .eq(FMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(FMeasureAnalysisData::getDataVersion)
                .last("limit 1");
        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (analysisData != null && analysisData.getMeasureDataId() == null) {

            analysisData.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            analysisData.setMeasureDataId(fMeasureData.getId());
            analysisData.setIsDelete(createType);
            analysisData.setIsSave(noSave);
            fMeasureAnalysisDataService.updateById(analysisData);

        } else {
            analysisData = new FMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setMeasureDataId(fMeasureData.getId());
            analysisData.setDataVersion(0);
            analysisData.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            analysisData.setIsDelete(createType);
            analysisData.setIsSave(noSave);
            fMeasureAnalysisDataService.save(analysisData);
        }

        FRationSaveCountDataInfo countDataInfo = new FRationSaveCountDataInfo();
        countDataInfo.setTaskId(taskId);
        countDataInfo.setSampleId(sampleId);
        countDataInfo.setMeasureDataId(fMeasureData.getId());
        countDataInfo.setAnalyzeDataId(analysisData.getId());
        countDataInfo.setCountData(dataInfo.getCountData());

        return R.ok(countDataInfo);
    }

    @Override
    public R<Boolean> saveAnalyzeData(FRationSaveCountDataInfo countData) {
        String taskId = countData.getTaskId();
        String sampleId = countData.getSampleId();
        String measureDataId = countData.getMeasureDataId();
        FMeasureAnalysisData analysisData;
        if (StringUtils.hasText(countData.getAnalyzeDataId())) {
            analysisData = fMeasureAnalysisDataService.getById(countData.getAnalyzeDataId());
        } else {
            LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
            analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                    .eq(FMeasureAnalysisData::getSampleKey, sampleId)
                    .eq(FMeasureAnalysisData::getMeasureDataId, measureDataId)
                    .orderByDesc(FMeasureAnalysisData::getDataVersion)
                    .last("limit 1");
            analysisData = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        }

        if (analysisData == null) {
            analysisData = new FMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setMeasureDataId(measureDataId);
            analysisData.setDataVersion(0);
            analysisData.setIsDelete(createType);
            analysisData.setAnalysisResult(JSON.toJSONString(countData.getCountData()));
            analysisData.setIsSave(noSave);
            fMeasureAnalysisDataService.save(analysisData);
        } else {
            if (!analysisData.getMeasureDataId().equals(measureDataId)) {
                analysisData.setMeasureDataId(measureDataId);
            }
            analysisData.setAnalysisResult(JSON.toJSONString(countData.getCountData()));
            analysisData.setIsSave(noSave);
            fMeasureAnalysisDataService.updateById(analysisData);
        }

        return R.ok(true);

    }

    @Override
    public void saveEmptySampleData(List<FRationSaveCountDataInfo> emptySampleList) {
        for (FRationSaveCountDataInfo emptyData : emptySampleList) {
            LambdaQueryWrapper<FMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FMeasureAnalysisData::getTaskId, emptyData.getTaskId())
                    .eq(FMeasureAnalysisData::getSampleKey, emptyData.getSampleId())
                    .eq(FMeasureAnalysisData::getMeasureDataId, emptyData.getMeasureDataId())
                    .orderByDesc(FMeasureAnalysisData::getDataVersion)
                    .last("limit 1");
            FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(queryWrapper);
            if (analysisData != null) {
                analysisData.setAnalysisResult(JSON.toJSONString(emptyData.getCountData()));
                analysisData.setIsSave(noSave);
                fMeasureAnalysisDataService.updateById(analysisData);
            } else {
                analysisData = new FMeasureAnalysisData();
                analysisData.setTaskId(emptyData.getTaskId());
                analysisData.setSampleKey(emptyData.getSampleId());
                analysisData.setMeasureDataId(emptyData.getMeasureDataId());
                analysisData.setDataVersion(0);
                analysisData.setAnalysisResult(JSON.toJSONString(emptyData.getCountData()));
                analysisData.setIsDelete(createType);
                analysisData.setIsSave(noSave);
                fMeasureAnalysisDataService.save(analysisData);
            }
        }
    }

    @Override
    public R<FRationCountDataInfo> countAbsValue(List<FRationWavelengthDataInfo> originData, FRationParameter parameter, Integer  digit) {

        List<String> waveLength = parameter.getWaveLength();
        List<String> lengthParams = parameter.getWaveLengthParams();
        double ans = 0;

        for (int i = 0; i < waveLength.size(); i++) {
            double value = Double.MIN_VALUE;
            for (FRationWavelengthDataInfo dataInfo : originData) {
                if (waveLength.get(i).equals(dataInfo.getWavelength()) && dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                    value = DataCountUtil.formatValue(digit, Double.parseDouble(dataInfo.getAbsValue()));
                }
            }
            if (value == Double.MIN_VALUE) {
                //未找到对应波长数据
                return R.fail(YamlMessageSource.getMessageStatic("0030300E"));
            }
            if (i < lengthParams.size()) {
                value = value * Double.parseDouble(lengthParams.get(i));
            } else {
                value = value * 1;
            }
            ans += value;
        }
        FRationCountDataInfo countDataInfo = new FRationCountDataInfo();
        countDataInfo.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, ans), digit));
        countDataInfo.setIdentity(SampleTableEnum.Abs.getIdentity());
        countDataInfo.setHeadName(SampleTableEnum.Abs.getName());

        return R.ok(countDataInfo);
    }

    @Override
    public R<FRationCountDataInfo> countAbsValue(List<FRationWavelengthDataInfo> originData, FRationParameter parameter) {

        List<String> waveLength = parameter.getWaveLength();
        List<String> lengthParams = parameter.getWaveLengthParams();
        double ans = 0;

        for (int i = 0; i < waveLength.size(); i++) {
            double value = Double.MIN_VALUE;
            for (FRationWavelengthDataInfo dataInfo : originData) {
                if (waveLength.get(i).equals(dataInfo.getWavelength())) {
                    value = Double.parseDouble(dataInfo.getAbsValue());
                }
            }
            if (value == Double.MIN_VALUE) {
                //未找到对应波长数据
                return R.fail(YamlMessageSource.getMessageStatic("0030300E"));
            }
            if (i < lengthParams.size()) {
                value = value * Double.parseDouble(lengthParams.get(i));
            } else {
                value = value * 1;
            }
            ans += value;
        }
        FRationCountDataInfo countDataInfo = new FRationCountDataInfo();
        countDataInfo.setColValue(String.valueOf(ans));
        countDataInfo.setIdentity(SampleTableEnum.Abs.getIdentity());
        countDataInfo.setHeadName(SampleTableEnum.Abs.getName());

        return R.ok(countDataInfo);
    }

    @Override
    public Result<List<Map<String, Object>>> queryStandSampleData(String taskId, Boolean allDisplay) {
        if (!StringUtils.hasText(taskId) || allDisplay == null) {
            //未选择任务
            return Result.fail(YamlMessageSource.getMessageStatic("0000001B"));
        }
        List<Map<String, Object>> ans = new ArrayList<>();
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        boolean repeat = false;
        if (StringUtils.hasText(task.getMethodKey())) {
            FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);
                if (StringUtils.hasText(parameter.getRepeatMode()) && parameter.getRepeatMode().equals("单池重复")) {
                    repeat = true;
                }
            }
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("未选择样品");
            return Result.ok(ans);
        }
        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);

        for (FTaskSpecimen specimen : taskSpecimen) {
            if (!allDisplay && specimen.getDeleteType().equals("1")) {
                continue;
            }
            FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());
            if (sample == null || !sample.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                log.info("不是标准样品，跳过");
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            //样品信息
            map.put(SampleTableEnum.SAMPLE_ID.getIdentity(), sample.getId());
            map.put(SampleTableEnum.SAMPLE_NAME.getIdentity(), sample.getSampleName());
            map.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber());
            map.put(SampleTableEnum.SAMPLE_TYPE.getIdentity(), SampleTypeEnum.getName(sample.getSampleType()));
            map.put(SampleTableEnum.SAMPLE_KIND.getIdentity(), sample.getSampleKind());
            map.put(SampleTableEnum.NORMAL_CONCENTRATION.getIdentity(), sample.getNormalConcentration());
            map.put(SampleTableEnum.DILUTION_RATIO.getIdentity(), sample.getDilutionRatio());
            map.put(SampleTableEnum.AMOUNT.getIdentity(), sample.getSampleVolume());

            if (StringUtils.hasText(sample.getNewColData())){
                Map<String, String> object = JSONObject.parseObject(sample.getNewColData(), new TypeReference<Map<String, String>>() {});

                map.putAll(object);
            }
            //数据信息,repeat：false-不重复，true-重复
            if (!repeat) {
                LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureData::getTaskId, taskId)
                        .eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .orderByDesc(FMeasureData::getSerialNumber).last("limit 1");
                FMeasureData data = fMeasureDataService.getOne(queryWrapper);
                if (data != null) {
                    map.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.REMARK.getIdentity(), data.getRemark());
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        List<FRationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), FRationWavelengthDataInfo.class);
                        for (FRationWavelengthDataInfo dataInfo : list) {
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                        }

                    }

                    LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                    analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                            .eq(FMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                            .eq(FMeasureAnalysisData::getAnalysisName, data.getSerialNumber());

                    FMeasureAnalysisData one = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                    if (one != null && one.getAnalysisResult() != null) {
                        List<FRationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), FRationCountDataInfo.class);
                        for (FRationCountDataInfo countData : countDataList) {
                            map.put(countData.getIdentity(), String.valueOf(countData.getColValue()));
                        }
                    }

                }
            } else {
                //重复模式，获取数据方式待定----暂时先以不重复做回显
                LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureData::getTaskId, taskId)
                        .eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .orderByDesc(FMeasureData::getSerialNumber).last("limit 1");
                FMeasureData data = fMeasureDataService.getOne(queryWrapper);
                if (data != null) {
                    map.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.REMARK.getIdentity(), data.getRemark());
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        Map<String, Map<String, String>> childrenList = new HashMap<>();
                        List<Map<String, String>> middle = new ArrayList<>();
                        List<FRationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), FRationWavelengthDataInfo.class);
                        for (FRationWavelengthDataInfo dataInfo : list) {
                            Map<String, String> children = new HashMap<>();

                            if (dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                                map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                                map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                            } else {
                                children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                                children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                                childrenList.computeIfAbsent(dataInfo.getIdentity(), k -> new HashMap<>()).putAll(children);
                            }
                        }
                        childrenList.forEach((key, value) -> middle.add(value));
                        map.put("children", middle);
                    }

                    LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                    analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                            .eq(FMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                            .eq(FMeasureAnalysisData::getAnalysisName, data.getSerialNumber());

                    FMeasureAnalysisData one = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                    if (one != null && one.getAnalysisResult() != null) {
                        List<FRationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), FRationCountDataInfo.class);
                        for (FRationCountDataInfo countData : countDataList) {
                            map.put(countData.getIdentity(), String.valueOf(countData.getColValue()));
                        }
                    }

                }
            }
            ans.add(map);
        }
        return Result.ok(ans);
    }

    @Override
    public Result<FRationSampleDataVO> querySampleData(FQueryDataDTO queryDataDTO) {

        String taskId = queryDataDTO.getTaskId();
        Boolean allDisplay = queryDataDTO.getAllDisplay();
        Integer digit = queryDataDTO.getDigit();
        List<String> option = queryDataDTO.getOption();

        if (!StringUtils.hasText(taskId)) {
            //未选择任务
            return Result.fail(YamlMessageSource.getMessageStatic("0000001B"));
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            log.info("未查询到任务信息");
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        if (digit == null) {
            digit = Constants.DEFAULT_DECIMAL_NUM;
            queryDataDTO.setDigit(digit);
        }

        if(queryDataDTO.getAllDisplay() == null) {
            allDisplay = false;
            queryDataDTO.setAllDisplay(allDisplay);
        }
        FMeasureTaskAttachment attachment;
        boolean recount = false;

        R<FMeasureTaskAttachment> r = fMeasureTaskAttachmentService.queryByTaskId(taskId);
        if (!r.getSuccess()) {
            log.info("未查询到任务附件信息");
            attachment = new FMeasureTaskAttachment();
            attachment.setTaskId(taskId);
            attachment.setDataParam(JSON.toJSONString(queryDataDTO));
            fMeasureTaskAttachmentService.save(attachment);
            recount = true;
        } else {
            attachment = r.getData();

            if (!StringUtils.hasText(attachment.getDataParam())) {
                attachment.setDataParam(JSON.toJSONString(queryDataDTO));
                fMeasureTaskAttachmentService.saveOrUpdate(attachment);
                recount = true;
            } else {
                FQueryDataDTO dataDTO = JSON.parseObject(attachment.getDataParam(), FQueryDataDTO.class);
                if (dataDTO.getDigit() == null || !dataDTO.getDigit().equals(digit)) {
                    recount = true;
                }
                attachment.setDataParam(JSON.toJSONString(queryDataDTO));
                fMeasureTaskAttachmentService.saveOrUpdate(attachment);
            }
        }

        if (recount) {
            changeStandSampleDataDigit(taskId, digit);
            changeUnknowSampleDigit(taskId, digit);
        }

        List<Integer> usefulLocation = new ArrayList<>();
        if (StringUtils.hasText(queryDataDTO.getDeviceSerialNumber())) {
            usefulLocation = fSamplePoolConfigService.getUsefulLocation(queryDataDTO.getDeviceSerialNumber());
        }
        /*
        查询数据
        */
        FRationSampleDataVO dataVO = new FRationSampleDataVO();

        List<Map<String, Object>> standSampleData = new ArrayList<>();
        List<Map<String, Object>> unknowSampleData = new ArrayList<>();
        boolean repeat = false;
        if (StringUtils.hasText(task.getMethodKey())) {
            FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);
                if (StringUtils.hasText(parameter.getRepeatMode()) && parameter.getRepeatMode().equals("单池重复")) {
                    repeat = true;
                }
            }
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("没有样品");
            dataVO.setStandSampleData(standSampleData);
            dataVO.setUnknowSampleData(unknowSampleData);
            return Result.ok(dataVO);
        }
        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);

        for (FTaskSpecimen specimen : taskSpecimen) {
            if (!allDisplay && specimen.getDeleteType().equals("1")) {
                continue;
            }
            if (specimen.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());
                Map<String, Object> singleSampleData = getSingleSampleData(task, sample, repeat, option, digit);
                singleSampleData.put("deleteType", specimen.getDeleteType());

                if (StringUtils.hasText(queryDataDTO.getDeviceSerialNumber())) {

                    if (specimen.getIsTested().equals(noTested) && !usefulLocation.isEmpty()) {
                        R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(queryDataDTO.getDeviceSerialNumber());
                        if (poolInfo.getSuccess()) {
                            FSamplePoolInfoVO data = poolInfo.getData();
                            if (data.getIsAutoChangePool()) {
                                singleSampleData.put(SampleTableEnum.SAMPLE_LOCATION.getIdentity(), "样品池"+sample.getSampleLocation());
                            }
                        }
                    }
                }
                standSampleData.add(singleSampleData);
            } else if (specimen.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {

                FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());

                Map<String, Object> sampleData = getSingleSampleData(task, sample, repeat, option, digit);
                sampleData.put("deleteType", specimen.getDeleteType());
                if (StringUtils.hasText(queryDataDTO.getDeviceSerialNumber())) {

                    if (specimen.getIsTested().equals(noTested) && !usefulLocation.isEmpty()) {
                        R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(queryDataDTO.getDeviceSerialNumber());
                        if (poolInfo.getSuccess()) {
                            FSamplePoolInfoVO data = poolInfo.getData();
                            if (data.getIsAutoChangePool()) {
                                sampleData.put(SampleTableEnum.SAMPLE_LOCATION.getIdentity(), "样品池"+sample.getSampleLocation());
                            }
                        }
                    }
                }

                unknowSampleData.add(sampleData);

            } else {
                log.info("类型出错");
            }
        }
        dataVO.setUnknowSampleData(unknowSampleData);
        dataVO.setStandSampleData(standSampleData);
        return Result.ok(dataVO);

    }

    private Map<String, Map<String, String>> extracted(List<String> option, FRationWavelengthDataInfo wlData, FMeasureSample sample) {

        Map<String, Map<String, String>> ans = new HashMap<>();

        FRepeatMeasureData absData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode());
        FRepeatMeasureData tPercentData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode());
        String pre = StringUtils.hasText(sample.getSampleNumber()) ? sample.getSampleNumber() + "-" : "";

        if (option != null && !option.isEmpty()) {
            for (String s : option) {
                Map<String, String> children = new HashMap<>();
                if (DataIdentityEnums.avg.getIdentity().equals(s)) {
                    children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + DataIdentityEnums.avg.getName());

                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), wlData.getAbsValue());
                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(),wlData.getTPercentValue());
                    ans.putIfAbsent(pre + DataIdentityEnums.avg.getIdentity(), children);
                }
                if (DataIdentityEnums.sd.getIdentity().equals(s)) {
                    children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + DataIdentityEnums.sd.getName());

                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), absData.getSd());
                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tPercentData.getSd());
                    ans.putIfAbsent(pre + DataIdentityEnums.sd.getIdentity(), children);
                }
                if (DataIdentityEnums.rsd.getIdentity().equals(s)) {
                    children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + DataIdentityEnums.rsd.getName());
                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), absData.getRsd());
                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tPercentData.getRsd());
                    ans.putIfAbsent(pre + DataIdentityEnums.rsd.getIdentity(), children);
                }
            }
        }
        return ans;
    }

    public Map<String, Object> getSingleSampleData(FMeasureTask task, FMeasureSample sample, Boolean repeat, List<String> option, Integer digit) {

        String sampleId = sample.getId();
        String taskId = task.getId();

        Map<String, Object> map = new HashMap<>();
        //样品信息
        map.put(SampleTableEnum.SAMPLE_ID.getIdentity(), sample.getId());
        map.put(SampleTableEnum.SAMPLE_NAME.getIdentity(), sample.getSampleName());
        map.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber());
        map.put(SampleTableEnum.SAMPLE_TYPE.getIdentity(), SampleTypeEnum.getName(sample.getSampleType()));
        map.put(SampleTableEnum.SAMPLE_KIND.getIdentity(), sample.getSampleKind());

        if (sample.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
            map.put(SampleTableEnum.NORMAL_CONCENTRATION.getIdentity(), sample.getNormalConcentration());
        }

        if (StringUtils.hasText(sample.getNewColData())) {
            Map<String, String> object = JSONObject.parseObject(sample.getNewColData(), new TypeReference<Map<String, String>>() {
            });

            map.putAll(object);
        }

        //数据信息,repeat：false-不重复，true-重复
        LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureData::getTaskId, taskId)
                .eq(FMeasureData::getSampleKey, sampleId)
                .orderByDesc(FMeasureData::getSerialNumber).last("limit 1");
        FMeasureData data = fMeasureDataService.getOne(queryWrapper);
        if (!repeat) {
            if (data != null) {

                if (StringUtils.hasText(data.getOriginalCalculationData())) {
                    List<FRationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), FRationWavelengthDataInfo.class);
                    for (FRationWavelengthDataInfo dataInfo : list) {
//                        String s = DataCountUtil.formatValueWithBigDecimal(digit, dataInfo.getAbsValue());
                        Double v = DataCountUtil.formatValue(digit, Double.valueOf(dataInfo.getAbsValue()));
                        String s = DataCountUtil.addZero(v, digit);

                        map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), s);

                        Double t = DataCountUtil.formatValue(digit, Double.valueOf(dataInfo.getTPercentValue()));
                        String tP = DataCountUtil.addZero(t, digit);

                        map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tP);
                    }
                }

                map.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), data.getSamplingTime());
                map.put(SampleTableEnum.REMARK.getIdentity(), data.getRemark());

                LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                        .eq(FMeasureAnalysisData::getSampleKey, sampleId)
                        .eq(FMeasureAnalysisData::getMeasureDataId, data.getId())
                        .orderByDesc(FMeasureAnalysisData::getDataVersion).last("limit 1");

                FMeasureAnalysisData one = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                if (one != null && one.getAnalysisResult() != null) {
                    List<FRationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), FRationCountDataInfo.class);
                    for (FRationCountDataInfo countData : countDataList) {
                        if (countData.getColValue() != null) {
                            map.put(countData.getIdentity(), countData.getColValue());
                        }

                    }
                }

            }

        } else {
            //重复模式，获取数据方式待定----暂时先以不重复做回显
            if (data != null) {
                map.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), data.getSamplingTime());
                map.put(SampleTableEnum.REMARK.getIdentity(), data.getRemark());
                if (StringUtils.hasText(data.getOriginalCalculationData())) {
                    Map<String, Map<String, String>> childrenList = new TreeMap<>((key1, key2) -> {
                        // 分割键，格式如 "default-1-1"
                        String[] parts1 = key1.split("-");
                        String[] parts2 = key2.split("-");

                        // 按照各部分进行比较
                        for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {

                            // 如果都是数字，则按数值比较
                            if (isNumeric(parts1[i]) && isNumeric(parts2[i])) {
                                int num1 = Integer.parseInt(parts1[i]);
                                int num2 = Integer.parseInt(parts2[i]);
                                int compare = Integer.compare(num1, num2);
                                if (compare != 0) {
                                    return compare;
                                }
                            } else {
                                // 否则按字符串比较
                                int compare = parts1[i].compareTo(parts2[i]);
                                if (compare != 0) {
                                    return compare;
                                }
                            }
                        }

                        // 如果前缀都相同，长度短的排在前面
                        return Integer.compare(parts1.length, parts2.length);
                    });
                    Map<String, Map<String, String>> resultChildrenList = new TreeMap<>();
                    List<Map<String, String>> middle = new ArrayList<>();
                    List<FRationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), FRationWavelengthDataInfo.class);
                    for (FRationWavelengthDataInfo dataInfo : list) {
                        Map<String, String> children = new HashMap<>();

                        Double v = DataCountUtil.formatValue(digit, Double.valueOf(dataInfo.getAbsValue()));
                        String s = DataCountUtil.addZero(v, digit);
                        if (dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {

                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), s);

                            Double t = DataCountUtil.formatValue(digit, Double.valueOf(dataInfo.getTPercentValue()));
                            String tP = DataCountUtil.addZero(t, digit);

                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tP);

//                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), DataCountUtil.formatValueWithBigDecimal(digit,dataInfo.getAbsValue()));
//                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), DataCountUtil.formatValueWithBigDecimal(digit, dataInfo.getTPercentValue()));

                            Map<String, Map<String, String>> ch = extracted(option, dataInfo, sample);
                            if (!ch.isEmpty()) {
                                for (Map.Entry<String, Map<String, String>> entry : ch.entrySet()) {
                                    String key = entry.getKey();
                                    Map<String, String> value = entry.getValue();

                                    if (resultChildrenList.containsKey(key)) {
                                        // 如果键已存在，将新值中的键值对合并到现有Map中
                                        Map<String, String> existingMap = resultChildrenList.get(key);
                                        // 将value中的所有键值对放入existingMap中（相同键会覆盖，新键会添加）
                                        existingMap.putAll(value);
                                    } else {
                                        // 如果键不存在，直接添加（创建副本以避免引用问题）
                                        resultChildrenList.put(key, new HashMap<>(value));
                                    }
                                }
                            }

                        } else {

                            children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), s);

                            Double t = DataCountUtil.formatValue(digit, Double.valueOf(dataInfo.getTPercentValue()));
                            String tP = DataCountUtil.addZero(t, digit);

                            children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tP);

//                            children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), DataCountUtil.formatValueWithBigDecimal(digit,dataInfo.getAbsValue()));
//                            children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), DataCountUtil.formatValueWithBigDecimal(digit,dataInfo.getTPercentValue()));
                            String[] split = dataInfo.getIdentity().split("-");
                            String pre = StringUtils.hasText(sample.getSampleNumber()) ? sample.getSampleNumber() + "-" : "";
                            children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + split[split.length - 1]);
                            String time = dataInfo.getMeasureTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            children.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), time);
                            childrenList.computeIfAbsent(dataInfo.getIdentity(), k -> new HashMap<>()).putAll(children);
                        }
                    }
                    childrenList.forEach((key, value) -> middle.add(value));
                    resultChildrenList.forEach((key, value) -> middle.add(value));
                    map.put("children", middle);
                }
            }
        }
        LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                .eq(FMeasureAnalysisData::getSampleKey, sampleId);
        if (data != null) {
            analysisDataQueryWrapper.eq(FMeasureAnalysisData::getMeasureDataId, data.getId());
        }
        analysisDataQueryWrapper.orderByDesc(FMeasureAnalysisData::getDataVersion).last("limit 1");

        FMeasureAnalysisData one = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (one != null && one.getAnalysisResult() != null) {
            List<FRationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), FRationCountDataInfo.class);
            for (FRationCountDataInfo countData : countDataList) {
                if (countData.getColValue() !=null) {
                    map.put(countData.getIdentity(), countData.getColValue());
                }
            }
        }
        if (one != null && StringUtils.hasText(one.getAnalysisParam())) {
            Map<String, String> jsonObject;
            jsonObject = JSON.parseObject(one.getAnalysisParam(), new TypeReference<Map<String, String>>(){});

            map.putAll(jsonObject);
        }
        return map;
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.matches("-?\\d+");
    }


    @Override
    public Result<List<Map<String, Object>>> queryUnknowSampleData(String taskId, Boolean allDisplay) {
        if (!StringUtils.hasText(taskId) || allDisplay == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        List<Map<String, Object>> ans = new ArrayList<>();
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        boolean repeat = false;
        if (StringUtils.hasText(task.getMethodKey())) {
            FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);
                if (StringUtils.hasText(parameter.getRepeatMode()) && parameter.getRepeatMode().equals("单池重复")) {
                    repeat = true;
                }
            }
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("未选择样品");
            return Result.ok(ans);
        }
        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);

        for (FTaskSpecimen specimen : taskSpecimen) {
            if (!allDisplay && specimen.getDeleteType().equals("1")) {
                continue;
            }
            FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());
            if (sample == null || !sample.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                log.info("不是未知样品，跳过");
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            //样品信息
            map.put(SampleTableEnum.SAMPLE_ID.getIdentity(), sample.getId());
            map.put(SampleTableEnum.SAMPLE_NAME.getIdentity(), sample.getSampleName());
            map.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber());
            map.put(SampleTableEnum.DILUTION_RATIO.getIdentity(), sample.getDilutionRatio());
            map.put(SampleTableEnum.AMOUNT.getIdentity(), sample.getSampleVolume());
            map.put(SampleTableEnum.SAMPLE_TYPE.getIdentity(), SampleTypeEnum.getName(sample.getSampleType()));
            map.put(SampleTableEnum.SAMPLE_KIND.getIdentity(), sample.getSampleKind());
            map.put(SampleTableEnum.NORMAL_CONCENTRATION.getIdentity(), sample.getNormalConcentration());
            map.put(SampleTableEnum.SAMPLE_LOCATION.getIdentity(), "样品池"+sample.getSampleLocation());

            if (StringUtils.hasText(sample.getNewColData())){
                Map<String, String> object = JSONObject.parseObject(sample.getNewColData(), new TypeReference<Map<String, String>>() {});

                map.putAll(object);
            }

            //数据信息,repeat：false-不重复，true-重复
            if (!repeat) {
                LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureData::getTaskId, taskId)
                        .eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .orderByDesc(FMeasureData::getSerialNumber).last("limit 1");
                FMeasureData data = fMeasureDataService.getOne(queryWrapper);
                if (data != null) {

                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        List<FRationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), FRationWavelengthDataInfo.class);
                        for (FRationWavelengthDataInfo dataInfo : list) {
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                        }
                    }

                    map.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.REMARK.getIdentity(), data.getRemark());

                    LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                    analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                            .eq(FMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                            .eq(FMeasureAnalysisData::getAnalysisName, data.getId() + "-" + data.getSerialNumber());

                    FMeasureAnalysisData one = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                    if (one != null && one.getAnalysisResult() != null) {
                        List<FRationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), FRationCountDataInfo.class);
                        for (FRationCountDataInfo countData : countDataList) {
                            map.put(countData.getIdentity(), String.valueOf(countData.getColValue()));
                        }
                    }

                }
            } else {
                //重复模式，获取数据方式待定----暂时先以不重复做回显
                LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureData::getTaskId, taskId)
                        .eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .orderByDesc(FMeasureData::getSerialNumber).last("limit 1");
                FMeasureData data = fMeasureDataService.getOne(queryWrapper);
                if (data != null) {
                    map.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.REMARK.getIdentity(), data.getRemark());
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        Map<String, Map<String, String>> childrenList = new HashMap<>();
                        List<Map<String, String>> middle = new ArrayList<>();
                        List<FRationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), FRationWavelengthDataInfo.class);
                        for (FRationWavelengthDataInfo dataInfo : list) {
                            Map<String, String> children = new HashMap<>();

                            if (dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                                map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                                map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                            } else {
                                children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                                children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                                childrenList.computeIfAbsent(dataInfo.getIdentity(), k -> new HashMap<>()).putAll(children);
                            }
                        }
                        childrenList.forEach((key, value) -> middle.add(value));
                        map.put("children", middle);
                    }

                    LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                    analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                            .eq(FMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                            .eq(FMeasureAnalysisData::getAnalysisName, data.getId() + "-" + data.getSerialNumber());

                    FMeasureAnalysisData one = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                    if (one != null && one.getAnalysisResult() != null) {
                        List<FRationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), FRationCountDataInfo.class);
                        for (FRationCountDataInfo countData : countDataList) {
                            map.put(countData.getIdentity(), String.valueOf(countData.getColValue()));
                        }
                    }

                }
            }
            ans.add(map);
        }
        return Result.ok(ans);
    }

    @Override
    public R<FittingReceiveInfo> fittingCurve(List<FRationCountDataInfo> countData, Boolean empty, String methodId, List<String> inputParam) {
        
        List<Double> data = new ArrayList<>();
        FittingParam param = new FittingParam();

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);
        FittingReceiveInfo pointsFitting = null;

        if (!StringUtils.hasText(parameter.getCurveType())){
            //请选择曲线类型
            return R.fail(YamlMessageSource.getMessageStatic("00102025"));
        }
        int minPointNum = Integer.MAX_VALUE;

        switch (parameter.getCurveType()) {
            case "1":
                minPointNum = 2;
                break;
            case "2":
                minPointNum = 3;
                break;

            case "3":
                minPointNum = 4;
                break;

            case "4":
                minPointNum = 5;
                break;
        }

        if (parameter.getZeroMethod() != null && parameter.getZeroMethod().contains(RationCurveParamEnums.insert_zero.getIdentity())) {
            minPointNum = minPointNum - 1;
        }

        if (inputParam.size() < minPointNum) {
            log.info("数据点数不足，不进行曲线拟合");
            //数据点数不足，不进行曲线拟合
            return R.fail(YamlMessageSource.getMessageStatic("00102026"));
        }


        if (empty) {
            for (FRationCountDataInfo dataInfo : countData) {
                if (dataInfo.getIdentity().equals(SampleTableEnum.SUM_Abs.getIdentity())) {
                    data.add(Double.parseDouble(dataInfo.getColValue()));

                }
            }
        } else {
            for (FRationCountDataInfo dataInfo : countData) {
                if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                    data.add(Double.parseDouble(dataInfo.getColValue()));
                }
            }
        }

        List<String> zeroMethod = parameter.getZeroMethod();

        param.setForce_zero(0L);

        if (zeroMethod != null && !zeroMethod.isEmpty()) {
            for (String zero: zeroMethod) {
                if (zero.equals(RationCurveParamEnums.insert_zero.getIdentity())) {
                    data.add(0.00);
                    inputParam.add("0.0");
                }

                if (zero.equals(RationCurveParamEnums.force_zero.getIdentity())) {
                    param.setForce_zero(1L);
                }
            }
        }

        param.setDegree(Long.parseLong(parameter.getCurveType()));

        if (RationCurveParamEnums.typeAbs.getIdentity().equals(parameter.getEquationType())) {
            param.setX_data(data.stream().mapToDouble(Double::doubleValue).toArray());
            param.setY_data(inputParam.stream().mapToDouble(Double::parseDouble).toArray());
        } else {
            param.setY_data(data.stream().mapToDouble(Double::doubleValue).toArray());
            param.setX_data(inputParam.stream().mapToDouble(Double::parseDouble).toArray());
        }
        String s = restUtil.dataFit(param);
        FittingReceiveInfo info = JSON.parseObject(s, FittingReceiveInfo.class);
        if (info == null) {
            log.info("数据拟合失败");
            return R.fail();
        }

        if (!ResultCodeEnum.SUCCESS.getCode().equals(info.getCode())) {
            log.info("数据拟合失败");
            return R.fail(info.getMessage());
        }

        String equation = info.getEquation();
        if (equation == null) {
            log.info("数据拟合失败");
            //曲线拟合失败
            return R.fail(YamlMessageSource.getMessageStatic("0030300F"));
        }

        if (RationCurveParamEnums.typeAbs.getIdentity().equals(parameter.getEquationType())) {
            info.setEquation(equation.replace("x", "Abs").replace("y", "C"));
        } else {
            info.setEquation(equation.replace("x", "C").replace("y", "Abs"));
        }
        Arrays.stream(param.getX_data()).min().ifPresent(info::setMinX);
        Arrays.stream(param.getX_data()).max().ifPresent(info::setMaxX);

        return R.ok(info);
    }


    @Override
    public Result<FRationFitCurveVO> queryCurveInfo(String taskId, String methodId){

        if (!StringUtils.hasText(taskId)) {
            log.info("缺少任务id");
            //未选择任务
            return Result.fail(YamlMessageSource.getMessageStatic("0000001B"));
        }
        if (!StringUtils.hasText(methodId)) {
            log.info("缺少方法id");
            //未选择方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000008"));
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            log.info("任务不存在");
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        int digit = Constants.DEFAULT_DECIMAL_NUM;

        R<FMeasureTaskAttachment> attachmentR = fMeasureTaskAttachmentService.queryByTaskId(taskId);
        if (attachmentR.getSuccess()) {
            FMeasureTaskAttachment data = attachmentR.getData();
            if (data != null && data.getDataParam() != null) {
                FQueryDataDTO dataDTO = JSON.parseObject(data.getDataParam(), FQueryDataDTO.class);
                if (dataDTO!=null && dataDTO.getDigit() != null) {
                    digit = dataDTO.getDigit();
                }
            }
        }

        FRationFitCurveVO resultVo = new FRationFitCurveVO();

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);
        FittingReceiveInfo pointsFitting = null;
        if (StringUtils.hasText(parameter.getCoefficientMode()) && parameter.getCoefficientMode().equals(RationCurveParamEnums.direct_coefficient.getIdentity())) {

            FittingReceiveInfo fittingInfo = createFittingInfo(methodId);
            if (fittingInfo != null) {
                List<JSONObject> unknownPoints = new ArrayList<>();
                Map<String, List<JSONObject>> pointsMap = collectSamplePoints(taskId, parameter, digit, fittingInfo, true, false);
                if (pointsMap != null && pointsMap.containsKey("unknown")) {
                    unknownPoints = pointsMap.get("unknown");
                }

                FittingRangeResult range = calculateFittingInfoRange(taskId, parameter, true, false, fittingInfo);
                if (range != null) {
                    fittingInfo.setMinX(range.getMinX());
                    fittingInfo.setMaxX(range.getMaxX());
                }

                resultVo = curveData(fittingInfo);
                resultVo.setUnknowPoints(unknownPoints);
                resultVo.setTaskId(taskId);
                resultVo.setEquationType(RationCurveParamEnums.getName(parameter.getEquationType()));
                resultVo.setCurveType(RationCurveParamEnums.getName(parameter.getCurveType()));
                return Result.ok(resultVo);
            }

        }

        if (StringUtils.hasText(task.getFittingParam()) && !task.getFittingParam().equals("{}") && !task.getFittingParam().equals("null")) {
            FittingReceiveInfo fittingParam = JSONObject.parseObject(task.getFittingParam(), FittingReceiveInfo.class);
            pointsFitting = fittingParam;
            FittingRangeResult range = calculateFittingInfoRange(taskId, parameter, true, true, fittingParam);
            if (range != null) {
                fittingParam.setMinX(range.getMinX());
                fittingParam.setMaxX(range.getMaxX());
            }
            resultVo = curveData(fittingParam);
            if (StringUtils.hasText(fittingParam.getR())) {
                Double r = null;
                try  {
                     r = Double.valueOf(fittingParam.getR());
                } catch (Exception ignored) {
                }
                if (r != null) {
                    if (!r.isNaN() && !r.isInfinite()) {
                        resultVo.setR(DataCountUtil.addZero(r, digit));
                    } else {
                        resultVo.setR(String.valueOf(r));
                    }

                }
            }
            if (StringUtils.hasText(fittingParam.getR2())) {

                Double r2 = null;
                try  {
                    r2 = Double.valueOf(fittingParam.getR2());
                } catch (Exception ignored) {
                }
                if (r2 != null) {
                    if (!r2.isNaN() && !r2.isInfinite()) {
                        resultVo.setR2(DataCountUtil.addZero(r2, digit));
                    } else {
                        resultVo.setR2(String.valueOf(r2));
                    }
                }
            }

         }

        if (StringUtils.hasText(task.getSampleKey())) {
            Map<String, List<JSONObject>> pointsMap = collectSamplePoints(taskId, parameter, digit, pointsFitting, true, true);
            if (pointsMap != null) {
                List<JSONObject> unknownPoints = pointsMap.getOrDefault("unknown", new ArrayList<>());
                List<JSONObject> standPoints = pointsMap.getOrDefault("stand", new ArrayList<>());
                resultVo.setUnknowPoints(unknownPoints);
                resultVo.setStandPoints(standPoints);
            }
        }

        resultVo.setTaskId(taskId);
        resultVo.setEquationType(RationCurveParamEnums.getName(parameter.getEquationType()));
        resultVo.setCurveType(RationCurveParamEnums.getName(parameter.getCurveType()));

        return Result.ok(resultVo);

    }

    @Override
    public FRationFitCurveVO curveData(FittingReceiveInfo info) {
        List<Double> list = info.getCoefficients().stream()
                .map(d -> BigDecimal.valueOf(d)
                        .setScale(3, RoundingMode.HALF_UP)
                        .doubleValue())
                .collect(Collectors.toList());

        List<JSONObject> dataList = new ArrayList<>();

        double k0 = info.getK0() == null ? 0 : info.getK0();
        double k1 = info.getK1() == null ? 0 : info.getK1();
        double k2 = info.getK2() == null ? 0 : info.getK2();
        double k3 = info.getK3() == null ? 0 : info.getK3();
        double k4 = info.getK4() == null ? 0 : info.getK4();

        double step = 0.01;
        int dataDigit = 8;
        //采用3位小数来计算
        double minX = info.getMinX() == null ? -1 : info.getMinX();
        double maxX = info.getMaxX() == null ? 1 : info.getMaxX();
        if(minX==maxX){
            minX=minX-(minX/2);
            maxX=maxX+(maxX/2);
            if (minX==0){
                minX=-1;
                maxX=1;
            }
        }

        step = (maxX - minX)/100;

        double st = step * 3;

        switch (info.getDegree()) {
            case 1:
                for (double i = minX - st; i < maxX + st; i += step) {
                    double y = k0 + k1 * i;
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("x", new BigDecimal(i).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    jsonObject.put("y", new BigDecimal(y).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    dataList.add(jsonObject);
                }
                break;
            case 2:
                for (double i = minX - st; i < maxX + st; i += step) {
                    double y = k0 + k1 * i + k2 * i * i;
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("x", new BigDecimal(i).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    jsonObject.put("y", new BigDecimal(y).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    dataList.add(jsonObject);
                }
                break;
            case 3:
                for (double i = minX - st; i < maxX + st; i += step) {
                    double y = k0 + k1 * i + k2 * i * i + k3 * i * i * i;
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("x", new BigDecimal(i).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    jsonObject.put("y", new BigDecimal(y).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    dataList.add(jsonObject);
                }
                break;

            case 4:
                for (double i = minX - st; i < maxX + st; i += step) {
                    double y = k0 + k1 * i + k2 * i * i + k3 * i * i * i + k4 * i * i * i * i;
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("x", new BigDecimal(i).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    jsonObject.put("y", new BigDecimal(y).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    dataList.add(jsonObject);

                }
                break;
            default:
                break;
        }
        FRationFitCurveVO vo = new FRationFitCurveVO();
        vo.setCoefficientList(list);
        vo.setDataList(dataList);
        vo.setEquation(info.getEquation());
        vo.setCurveType(RationCurveParamEnums.getName(String.valueOf(info.getDegree())));

        if (StringUtils.hasText(info.getR())) {
            Double r = null;
            try  {
                r = Double.valueOf(info.getR());
            } catch (Exception ignored) {
            }
            if (r != null) {
                if (!r.isNaN() && !r.isInfinite()) {
                    vo.setR(DataCountUtil.addZero(r, dataDigit));
                } else {
                    vo.setR(String.valueOf(r));
                }
            }
        }
        if (StringUtils.hasText(info.getR2())) {
            Double r2 = null;
            try  {
                r2 = Double.valueOf(info.getR2());
            } catch (Exception ignored) {
            }

            if (r2 != null) {
                if (!r2.isNaN() && !r2.isInfinite()) {
                    vo.setR2(DataCountUtil.addZero(r2, dataDigit));
                } else {
                    vo.setR2(String.valueOf(r2));
                }
            }
        }
        return vo;
    }

    /**
     * 值计算
     * @param info 曲线信息
     * @param value 待计算的值
     * @param equationType 曲线类型
     * @param digit 小数位数
     * @return
     */
    @Override
    public String valueCalculate(FittingReceiveInfo info, Double value, String equationType, Integer digit) {

        String s = "";
        String ans = null;

        try {
            if (equationType.equals(RationCurveParamEnums.typeAbs.getIdentity())) {
                s = restUtil.concentrationCalculate(info.getCoefficients(), value, 1);
            } else if (equationType.equals(RationCurveParamEnums.typeCurve.getIdentity())) {
                s = restUtil.concentrationCalculate(info.getCoefficients(), value, 0);
            }
            if (StringUtils.hasText(s)) {
                JSONObject jsonObject = JSON.parseObject(s);

                ans = jsonObject.get("value").toString();
            }
            return ans;
        } catch (Exception e) {
            log.error("数据计算异常", e);
            return null;
        }


    }

    @Override
    public String valueCalculate(FittingReceiveInfo info, Double value, String equationType) {

        String s = "";
        String ans = null;

        if (equationType.equals(RationCurveParamEnums.typeAbs.getIdentity())) {
            s = restUtil.concentrationCalculate(info.getCoefficients(), value, 1);
        } else if (equationType.equals(RationCurveParamEnums.typeCurve.getIdentity())) {
            s = restUtil.concentrationCalculate(info.getCoefficients(), value, 0);
        }
        if (StringUtils.hasText(s)) {
            JSONObject jsonObject = JSON.parseObject(s);

            ans = jsonObject.get("value").toString();
        }
        return ans;
    }

    @Override
    public List<FRationCountDataInfo> dataRounding(List<FRationCountDataInfo> dataVO, FRationParameter parameter) {

        List<FRoundingVO> approxParams = parameter.getApproxParams();
        if (approxParams == null || approxParams.isEmpty()) {
            log.info("没有设置修约参数");
            return dataVO;
        }
        List<FPreCountResultParam> resultParams = parameter.getResultParams();
        if (resultParams == null || resultParams.isEmpty()) {
            log.info("没有设置待计算结果参数，不做修约处理");
            return dataVO;
        }

        if (dataVO == null || dataVO.isEmpty()) {
            log.info("没有待修约的数据");
            return dataVO;
        }
        Map<String, String> map = new HashMap<>();
        for (FPreCountResultParam param : resultParams) {
            map.put(param.getResultIdentity(), param.getResultIdentity() + "-" + param.getFormulaText());
        }

        map.put(SampleTableEnum.FIT_CONCENTRATION.getIdentity(), SampleTableEnum.FIT_CONCENTRATION.getIdentity());
        Map<String, List<FRoundingParamVO>> roundingInfo = new HashMap<>();

        for (FRoundingVO param : approxParams) {
            String identity = param.getResultIdentity();
            if (map.containsKey(identity)) {
                String key = map.get(identity);
                roundingInfo.put(key, param.getRoundingParams());
            }
        }
        for (FRationCountDataInfo count : dataVO) {

            String resultIdentity = count.getIdentity();

            List<FRoundingParamVO> list = roundingInfo.get(resultIdentity);
            if (roundingInfo.containsKey(resultIdentity) && list != null) {
                String s = roundData(count.getColValue(), list);
                count.setColValue(s);
            }
        }

        return dataVO;
    }

    @Override
    public String fitDataRounding(String fitConcentration, FRationParameter parameter) {

        List<FRoundingVO> approxParams = parameter.getApproxParams();
        if (approxParams == null || approxParams.isEmpty()) {
            log.info("无需修约");
            return fitConcentration;
        }

        if (!StringUtils.hasText(fitConcentration)) {
            log.info("数据为空");
            return fitConcentration;
        }

        List<FRoundingParamVO> roundingInfo = new ArrayList<>();

        for (FRoundingVO param : approxParams) {
            String identity = param.getResultIdentity();
            if (SampleTableEnum.FIT_CONCENTRATION.getIdentity().equals(identity)) {
                roundingInfo = param.getRoundingParams();
            }
        }

        if (roundingInfo.isEmpty()) {
            log.info("没有设置修约规则，无需修约");
            return fitConcentration;
        }

        return roundData(fitConcentration, roundingInfo);
    }

    private String roundData(String value, List<FRoundingParamVO> roundingParams) {
        double middle = Double.parseDouble(value);
        String  ans = value;
        for (FRoundingParamVO param : roundingParams) {
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

    @Override
    public R<List<FRationCountDataInfo>> formula(String analysisDataId, List<FRationWavelengthDataInfo> vo, FMeasureSample sample, FMeasureTask task, FRationParameter parameter, Integer digit) {
        if (!StringUtils.hasText(task.getTableInfo())) {
            //缺少分析参数，无法计算分析
            return R.fail(YamlMessageSource.getMessageStatic("00300003"));
        }

        FRationTaskHeadSnapShot rationTaskHeadSnapShot = JSON.parseObject(task.getTableInfo(), FRationTaskHeadSnapShot.class);
        if (rationTaskHeadSnapShot == null || rationTaskHeadSnapShot.getUnknownHead() == null) {
            //缺少分析参数，无法计算分析
            return R.fail(YamlMessageSource.getMessageStatic("00300003"));
        }
        FTaskHeadSnapshot snapshot = rationTaskHeadSnapShot.getUnknownHead();

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

        FMeasureAnalysisData one = fMeasureAnalysisDataService.getById(analysisDataId);
        if (one != null && StringUtils.hasText(one.getAnalysisParam())) {
            String analysisParam = one.getAnalysisParam();
            JSONObject object = JSON.parseObject(analysisParam);
            allData.putAll(object);
        }

        if (one != null && StringUtils.hasText(one.getAnalysisResult())) {
            JSONObject object = JSON.parseObject(one.getAnalysisResult());
            allData.putAll(object);
        }

        for (FRationWavelengthDataInfo dataInfo : vo) {
            if (dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                allData.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue()==null? null :dataInfo.getAbsValue());
                allData.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue()==null? null :dataInfo.getTPercentValue());
            }
        }

        StringBuilder values = new StringBuilder();

        StringBuilder identity = new StringBuilder();

        for (FInputParam inputParam : parameter.getSampleInputParams()) {
            boolean b = snapshot.getMapRelation().stream().anyMatch(
                    mapRelation -> inputParam.getResultIdentity().equals(mapRelation.getParamIdentity()));

            Optional<FInputParamMapTableHead> mapTableHead = snapshot.getMapRelation().stream().filter(
                    mapRelation -> inputParam.getResultIdentity().equals(mapRelation.getParamIdentity())).findFirst();
            if (!mapTableHead.isPresent()) {
                log.info("参数{}未添加映射，跳过", inputParam.getParamName());
                continue;
            }

            String s = allData.getString(mapTableHead.get().getHeadIdentity());
            if (!StringUtils.hasText(s)) {
                log.info("变量{}缺少数据！跳过", inputParam.getParamName());
                continue;
            }
            identity.append(mapTableHead.get().getParamIdentity()).append(",");
            s = DataCountUtil.formatValueWithBigDecimal(digit, s);
            values.append(s).append(",");
        }
        identity.deleteCharAt(identity.length()-1);
        values.deleteCharAt(values.length()-1);

        List<FRationCountDataInfo> countList = new ArrayList<>();
        for (FPreCountResultParam param : parameter.getResultParams()) {
            String[] split = param.getFormulaText().split("=");
            String text = split[split.length - 1];
            String result = restUtil.sum(text, identity.toString(), values.toString());
            FRationCountDataInfo countDataInfo = new FRationCountDataInfo();
            BeanUtils.copyProperties(param, countDataInfo);
            JSONObject resultJson = JSONObject.parseObject(result);

            if (!resultJson.get("code").equals("0") && !resultJson.get("code").equals(0)) {
               // "公式计算出错！" + resultJson.get("message")
                return R.fail(YamlMessageSource.getMessageStatic("00300007", resultJson.get("message")));
            }
            countDataInfo.setColValue(String.valueOf(resultJson.get("result")));
            countList.add(countDataInfo);
        }

        return R.ok(countList);
    }

    @Override
    public R<Boolean> changeStandSampleDataDigit(String taskId, Integer digit) {
        /*
        数位数的影响---测量数据不影响，平均值，sd，rsd影响
        abs,sumAbs，曲线拟合，拟合浓度，公式计算都会受影响
        首先，平均值，sd,rsd计算abs
        然后，sumAbs计算
        最后，曲线拟合，
        拟合浓度，公式计算
        标准样品和未知样品，需要一起查询
       不然针对api操作，会出现小数位数不一致的问题
       那重新计算，应该针对单样品？
       光度可以针对任务，定量应该针对样品来操作
         */
        String dealSampleKind = SampleTypeEnum.kind_stand.getIdentity();

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("没有样品");
            //"没有样品"
            return R.fail(YamlMessageSource.getMessageStatic("00303010"));
        }
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);
        boolean repeat = parameter.getRepeatMode().equals("单池重复");

        if (StringUtils.hasText(parameter.getCoefficientMode()) && parameter.getCoefficientMode().equals(RationCurveParamEnums.direct_coefficient.getIdentity())) {
            FittingReceiveInfo fittingInfo = createFittingInfo(task.getMethodKey());
            task.setFittingParam(JSON.toJSONString(fittingInfo));
            fMeasureTaskService.updateById(task);
            return R.ok();
        }

        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        boolean empty = false;
        boolean lastIsEmpty = false;
        Double minuend = 0.0;
        //存放空白样数据（计算后的值）
        List<FRationSaveCountDataInfo> emptyDataList = new ArrayList<>();
        //存放空白样计算出的Abs的值
        List<FRationCountDataInfo> emptyList = new ArrayList<>();

        //参与曲线拟合的数据
        List<FRationCountDataInfo> fitCurveData = new ArrayList<>();
        //参与曲线拟合的参数
        List<String> fitCurveParams = new ArrayList<>();

        empty = taskSpecimen.stream()
                .anyMatch(specimen ->
                        specimen.getSpecimenKey().equals(dealSampleKind)
                                && specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity()) &&
                                specimen.getDeleteType() != null &&
                                specimen.getDeleteType().equals("0")
                );

        for (FTaskSpecimen specimen : taskSpecimen) {

            if (specimen.getDeleteType().equals(deleteType)) {
                log.info("删除的样品");
                continue;
            }
            if (specimen.getIsTested().equals(noTested)) {
                log.info("没有测过的样品，跳过");
                continue;
            }

            if (!specimen.getSampleKind().equals(dealSampleKind)) {
                log.info("不是需要处理的样品，跳过");
                continue;
            }

            FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());

            LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FMeasureData::getTaskId, taskId)
                    .eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                    .orderByDesc(FMeasureData::getSerialNumber)
                    .last("limit 1");
            FMeasureData data = fMeasureDataService.getOne(queryWrapper);
            if (data == null) {
                log.info("没有测量数据");
                continue;
            }
            if (!StringUtils.hasText(data.getOriginalCalculationData())) {
                log.info("没有测量数据");
                continue;
            }
            List<FRationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), FRationWavelengthDataInfo.class);
            if (list == null || list.isEmpty()) {
                log.info("没有测量数据");
                continue;
            }
            FRationCountDataInfo ansData;
            FRationSaveCountDataInfo dealData = new FRationSaveCountDataInfo();
            dealData.setSampleId(specimen.getSpecimenKey());
            dealData.setTaskId(taskId);
            dealData.setMeasureDataId(data.getId());

            if (repeat) {
                List<FRationWavelengthDataInfo> wavelengthDataInfos = countRepeatData(list, digit);
                R<FRationCountDataInfo> absValue = countAbsValue(wavelengthDataInfos, parameter, digit);
                if (!absValue.getSuccess()) {
                    log.info("计算abs失败");
                    FRationCountDataInfo value = getAbsValue(taskId, sample.getId(), data.getId(), empty);
                    if (value == null) {
                        //数据处理出错且没有找到旧数据，处理失败
                        return R.fail(YamlMessageSource.getMessageStatic("00303010"));
                    }
                    fitCurveData.add(value);
                    fitCurveParams.add(sample.getNormalConcentration());

                    continue;
                }
                ansData = absValue.getData();

            } else {

                R<FRationCountDataInfo> absValue = countAbsValue(list, parameter, digit);
                if (!absValue.getSuccess()) {
                    log.info("重新计算abs失败");
                    FRationCountDataInfo value = getAbsValue(taskId, sample.getId(), data.getId(), empty);
                    if (value == null) {
                        //数据处理出错且没有找到旧数据，处理失败
                        return R.fail(YamlMessageSource.getMessageStatic("00303011"));
                    }
                    fitCurveData.add(value);
                    fitCurveParams.add(sample.getNormalConcentration());

                    continue;
                }
                ansData = absValue.getData();
            }

            if (specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {

                empty = true;
                FRationCountDataInfo yAbs = new FRationCountDataInfo();
                yAbs.setHeadName(SampleTableEnum.SUM_Abs.getName());
                yAbs.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
                yAbs.setColValue(DataCountUtil.addZero(0.0, digit));

                List<FRationCountDataInfo> currentList = new ArrayList<>();

                if (lastIsEmpty) {
                    emptyList.add(ansData);

                    double sub = 0.0;
                    //计算平均值
                    for (FRationCountDataInfo dataInfo : emptyList) {
                        sub += Double.parseDouble(dataInfo.getColValue());
                    }
                    minuend = DataCountUtil.formatValue(digit, sub / emptyList.size());
                    //添加到空白列表
                    currentList.add(ansData);
                    double v = Double.parseDouble(ansData.getColValue()) - minuend;
                    yAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, v), digit));
                    currentList.add(yAbs);
                    dealData.setCountData(currentList);
                    emptyDataList.add(dealData);

                    for (FRationSaveCountDataInfo emptyData : emptyDataList) {
                        List<FRationCountDataInfo> emList = emptyData.getCountData();
                        List<FRationCountDataInfo> newList = new ArrayList<>();

                        for (FRationCountDataInfo dataInfo : emList) {
                            if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                                newList.add(dataInfo);
                                double v1 = Double.parseDouble(dataInfo.getColValue()) - minuend;

                                FRationCountDataInfo sumAbs = new FRationCountDataInfo();
                                BeanUtils.copyProperties(yAbs, sumAbs);

                                sumAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit,v1), digit));
//                                yAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, v1), digit));
                                newList.add(sumAbs);
                            }
                        }
                        emptyData.setCountData(newList);
                    }
                } else {
                    emptyList.clear();
                    emptyDataList.clear();
                    emptyList.add(ansData);
                    minuend = Double.parseDouble(ansData.getColValue());
                    double v = Double.parseDouble(ansData.getColValue()) - minuend;
                    yAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, v), digit));
                    currentList.add(yAbs);
                    currentList.add(ansData);
                    dealData.setCountData(currentList);
                    emptyDataList.add(dealData);

                }
                saveEmptySampleData(emptyDataList);
                lastIsEmpty = true;

            } else {
                List<FRationCountDataInfo> currentList = new ArrayList<>();

                //测量过的数据，非空白样，添加标准浓度为了拟合曲线
                //TODO 未来需要添加质量和浓度的区分
                fitCurveParams.add(sample.getNormalConcentration());

                if (empty) {
                    FRationCountDataInfo sumAbsData = new FRationCountDataInfo();
                    sumAbsData.setHeadName(SampleTableEnum.SUM_Abs.getName());
                    sumAbsData.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
                    double v = Double.parseDouble(ansData.getColValue()) - minuend;
                    sumAbsData.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, v), digit));
                    currentList.add(sumAbsData);
                    currentList.add(ansData);
                    dealData.setCountData(currentList);
                    fitCurveData.add(sumAbsData);
                } else {
                    currentList.add(ansData);
                    dealData.setCountData(currentList);
                    fitCurveData.add(ansData);
                }
                saveAnalyzeData(dealData);
                lastIsEmpty = false;
            }
            specimen.setIsTested(tested);
        }
        task.setSampleKey(JSON.toJSONString(taskSpecimen));
        task.setFittingParam("{}");

        //拟合曲线
        R<FittingReceiveInfo> infoR = fittingCurve(fitCurveData, empty, task.getMethodKey(), fitCurveParams);
        if (infoR.getSuccess()) {
            log.info("拟合曲线成功，修改任务中的拟合参数");
            task.setFittingParam(JSON.toJSONString(infoR.getData()));
        }
        fMeasureTaskService.updateById(task);
        return R.ok();
    }

    @Override
    public R<Boolean> changeUnknowSampleDigit(String taskId, Integer digit) {
        String dealSampleKind = SampleTypeEnum.kind_unknown.getIdentity();
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("没有样品");
            //没有样品
            return R.fail(YamlMessageSource.getMessageStatic("00303010"));
        }
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);
        boolean repeat = parameter.getRepeatMode().equals("单池重复");

        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        boolean empty = false;
        boolean lastIsEmpty = false;
        Double minuend = 0.0;
        //存放空白样数据（计算后的值）
        List<FRationSaveCountDataInfo> emptyDataList = new ArrayList<>();
        //存放空白样计算出的Abs的值
        List<FRationCountDataInfo> emptyList = new ArrayList<>();

        empty = taskSpecimen.stream()
                .anyMatch(specimen ->
                        specimen.getSpecimenKey().equals(dealSampleKind)
                                && specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity()) &&
                                specimen.getDeleteType() != null &&
                                specimen.getDeleteType().equals(createType)
                );

        for (FTaskSpecimen specimen : taskSpecimen) {

            if (specimen.getDeleteType().equals(deleteType)) {
                log.info("删除的样品");
                continue;
            }
            if (specimen.getIsTested().equals(noTested)) {
                log.info("没有测过的样品，跳过");
                continue;
            }

            if (!specimen.getSampleKind().equals(dealSampleKind)) {
                log.info("不是需要处理的样品，跳过");
                continue;
            }

            FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());

            LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FMeasureData::getTaskId, taskId)
                    .eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                    .orderByDesc(FMeasureData::getSerialNumber)
                    .last("limit 1");
            FMeasureData data = fMeasureDataService.getOne(queryWrapper);
            if (data == null ||!StringUtils.hasText(data.getOriginalCalculationData())) {
                log.info("没有测量数据");
                continue;
            }

            List<FRationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), FRationWavelengthDataInfo.class);
            if (list == null || list.isEmpty()) {
                log.info("没有测量数据");
                continue;
            }
            FRationCountDataInfo ansData;
            FRationSaveCountDataInfo dealData = new FRationSaveCountDataInfo();
            dealData.setSampleId(specimen.getSpecimenKey());
            dealData.setTaskId(taskId);
            dealData.setMeasureDataId(data.getId());

            if (repeat) {
                List<FRationWavelengthDataInfo> wavelengthDataInfos = countRepeatData(list, digit);
                R<FRationCountDataInfo> absValue = countAbsValue(wavelengthDataInfos, parameter, digit);
                if (!absValue.getSuccess()) {
                    log.info("当前样品处理出错，跳过");
                    continue;
                }
                ansData = absValue.getData();

            } else {
                R<FRationCountDataInfo> absValue = countAbsValue(list, parameter, digit);
                if (!absValue.getSuccess()) {
                    log.info("计算abs出错，跳过");
                    continue;
                }
                ansData = absValue.getData();
            }

            if (specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {

                empty = true;
                FRationCountDataInfo yAbs = new FRationCountDataInfo();
                yAbs.setHeadName(SampleTableEnum.SUM_Abs.getName());
                yAbs.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
                yAbs.setColValue(DataCountUtil.addZero(0.0, digit));

                List<FRationCountDataInfo> currentList = new ArrayList<>();

                if (lastIsEmpty) {
                    emptyList.add(ansData);

                    double sub = 0.0;
                    //计算平均值
                    for (FRationCountDataInfo dataInfo : emptyList) {
                        sub += Double.parseDouble(dataInfo.getColValue());
                    }
                    minuend = DataCountUtil.formatValue(digit, sub / emptyList.size());
                    //添加到空白列表
                    currentList.add(ansData);
                    double v = Double.parseDouble(ansData.getColValue()) - minuend;
                    yAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit,v),digit));
                    currentList.add(yAbs);
                    dealData.setCountData(currentList);
                    emptyDataList.add(dealData);

                    for (FRationSaveCountDataInfo emptyData : emptyDataList) {
                        List<FRationCountDataInfo> emList = emptyData.getCountData();
                        List<FRationCountDataInfo> newList = new ArrayList<>();

                        for (FRationCountDataInfo dataInfo : emList) {
                            if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                                newList.add(dataInfo);
                                double v1 = Double.parseDouble(dataInfo.getColValue()) - minuend;
                                FRationCountDataInfo sumAbs = new FRationCountDataInfo();
                                BeanUtils.copyProperties(yAbs, sumAbs);

                                sumAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit,v1), digit));
//                                yAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit,v1), digit));
                                newList.add(sumAbs);
                            }
                        }
                        emptyData.setCountData(newList);
                    }
                } else {
                    emptyList.clear();
                    emptyDataList.clear();
                    emptyList.add(ansData);
                    minuend = Double.parseDouble(ansData.getColValue());
                    double v = Double.parseDouble(ansData.getColValue()) - minuend;
                    yAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, v), digit));
                    currentList.add(yAbs);
                    currentList.add(ansData);
                    dealData.setCountData(currentList);
                    emptyDataList.add(dealData);

                }
                saveEmptySampleData(emptyDataList);
                lastIsEmpty = true;
            } else {
                List<FRationCountDataInfo> currentList = new ArrayList<>();
                /*
                未知样品
                计算对象：countData
                首先，有空白，就计算sumAbs值，并且赋值到countData中
                然后，根据曲线计算，公式计算
                最后保存，计算完，abs值就保存一次，但是要注意替换。
                 */
                double toCountData = 0.0;

                if (empty) {
                    FRationCountDataInfo sumAbsData = new FRationCountDataInfo();
                    sumAbsData.setHeadName(SampleTableEnum.SUM_Abs.getName());
                    sumAbsData.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
                    double v = Double.parseDouble(ansData.getColValue()) - minuend;
                    sumAbsData.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, v), digit));
                    toCountData = Double.parseDouble(sumAbsData.getColValue());
                    currentList.add(sumAbsData);
                } else {
                    toCountData = Double.parseDouble(ansData.getColValue());
                }
                currentList.add(ansData);
                /*
                获取拟合的值
                 */
                FittingReceiveInfo fitInfo = JSON.parseObject(task.getFittingParam(), FittingReceiveInfo.class);

                if (fitInfo != null && fitInfo.getCoefficients()!= null) {
                    String ans = valueCalculate(fitInfo, toCountData, parameter.getEquationType(), digit);

                    if (ans != null) {
                        FRationCountDataInfo valueData = new FRationCountDataInfo();
                        valueData.setOriginalValue(ans);
                        String s = fitDataRounding(ans, parameter);

                        valueData.setHeadName(SampleTableEnum.FIT_CONCENTRATION.getName());
                        valueData.setIdentity(SampleTableEnum.FIT_CONCENTRATION.getIdentity());
                        valueData.setColValue(s);
                        currentList.add(valueData);
                    }
                }

                //TODO 公式计算，待做
                dealData.setCountData(currentList);
                saveAnalyzeData(dealData);
                lastIsEmpty = false;
            }
        }
        return R.ok();
    }

    public FRationCountDataInfo getAbsValue(String taskId, String sampleId, String measureDataId, Boolean empty) {
        LambdaQueryWrapper<FMeasureAnalysisData> query = new LambdaQueryWrapper<>();
        query.eq(FMeasureAnalysisData::getTaskId, taskId)
                .eq(FMeasureAnalysisData::getSampleKey, sampleId)
                .eq(FMeasureAnalysisData::getMeasureDataId, measureDataId).orderByDesc(FMeasureAnalysisData::getDataVersion);
        List<FMeasureAnalysisData> list = fMeasureAnalysisDataService.list(query);
        if (list != null && !list.isEmpty()) {
            if (empty) {
                for (FMeasureAnalysisData analysisData : list) {
                    if (analysisData.getAnalysisResult() != null) {
                        List<FRationCountDataInfo> countDataInfoList = JSON.parseArray(analysisData.getAnalysisResult(), FRationCountDataInfo.class);
                        for (FRationCountDataInfo dataInfo : countDataInfoList) {
                            if (dataInfo.getIdentity().equals(SampleTableEnum.SUM_Abs.getIdentity())) {
                                return dataInfo;
                            }
                        }
                    }
                }
            } else {
                for (FMeasureAnalysisData analysisData : list) {
                    if (analysisData.getAnalysisResult() != null) {
                        List<FRationCountDataInfo> countDataInfoList = JSON.parseArray(analysisData.getAnalysisResult(), FRationCountDataInfo.class);
                        for (FRationCountDataInfo dataInfo : countDataInfoList) {
                            if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                                return dataInfo;
                            }
                        }
                    }
                }

            }

        }
        return null;

    }
    public List<FRationWavelengthDataInfo> countRepeatData(List<FRationWavelengthDataInfo> originData, Integer digit) {

        List<FRationWavelengthDataInfo> ans = new ArrayList<>();

        Map<String, List<Double>> absValue = new HashMap<>();
        Map<String, List<Double>> tPercentValue = new HashMap<>();
        Map<String, FRationWavelengthDataInfo> dataMap = new HashMap<>();
        Set<String> wavelengthSet = new HashSet<>();

        for (FRationWavelengthDataInfo data : originData) {
            if (data.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                dataMap.putIfAbsent(data.getWavelength(), data);
                continue;
            }
            ans.add(data);
            double abs = Double.parseDouble(data.getAbsValue());
            double tPercent = Double.parseDouble(data.getTPercentValue());

            absValue.computeIfAbsent(data.getWavelength(), k -> new ArrayList<>()).add(abs);
            tPercentValue.computeIfAbsent(data.getWavelength(), k -> new ArrayList<>()).add(tPercent);
            wavelengthSet.add(data.getWavelength());
        }

        for (String wavelength : wavelengthSet) {
            FRationWavelengthDataInfo dataInfo = dataMap.get(wavelength);
            FRepeatMeasureData repeatData;
            Map<String, FRepeatMeasureData> otherData = new HashMap<>();

            List<Double> allAbs = absValue.get(wavelength);
            double mean = DataCountUtil.calculateMean(allAbs, digit);
            Double absSD = DataCountUtil.calculateSD(allAbs, digit);
            Double absRsd = DataCountUtil.calculateRSD(allAbs, digit);
            repeatData = new FRepeatMeasureData(absSD == null ? "未检出" :String.valueOf(absSD), absRsd == null ? "未检出" :String.valueOf(absRsd));
            otherData.put(wavelength + "-" + WaveLengthEnum.abs.getCode(), repeatData);

            List<Double> allPercent = tPercentValue.get(wavelength);
            double meanT = DataCountUtil.calculateMean(allPercent, digit);
            Double percentSD = DataCountUtil.calculateSD(allPercent, digit);
            Double percentRsd = DataCountUtil.calculateRSD(allPercent, digit);
            repeatData = new FRepeatMeasureData(percentSD == null ? "未检出" :String.valueOf(percentSD), percentRsd == null ? "未检出" :String.valueOf(percentRsd));
            otherData.put(wavelength + "-" + WaveLengthEnum.abs.getCode(), repeatData);

            dataInfo.setAbsValue(String.valueOf(mean));
            dataInfo.setTPercentValue(String.valueOf(meanT));
            dataInfo.setOtherData(otherData);

            ans.add(dataInfo);
        }
        return ans;
    }

    @Override
    public Result zero(String deviceSerialNumber, String methodId, String taskId, String updateBy) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            //未连接仪器
            return Result.fail(YamlMessageSource.getMessageStatic("0000001D"));
        }

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        if (!StringUtils.hasText(taskId)){
            //创建任务

            //创建任务
            FMeasureTask newTask = new FMeasureTask();

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
            newTask.setEndSaveTime(LocalDateTime.now());
            boolean save = false;
            for (int i = 0; i < 3; i++) {
                if (fMeasureTaskService.save(newTask)) {
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

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);
        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            //缺少波长
            return Result.fail(YamlMessageSource.getMessageStatic("0000001E"));
        }
        List<String> waveLength = parameter.getWaveLength();
        // 通过序列号获取ip
        String ip = deviceInfoService.getIp(deviceSerialNumber);
        List<ChangeParam> list = new ArrayList<>();
        String wave = "--";
        R<Boolean> r = deviceInfoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_ZERO);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        }
        DetectionConstants.zeroWavelength = "";
        for (String s : waveLength) {
            Command command = new Command(SetParamEnum.SetPhotoMode.getComId(), new Object[]{WaveLengthEnum.abs.getCode()});
            devMessageUtil.wavelengthLocate(command, ip);

            Double step = formulaUtil.calculateStep(Double.parseDouble(s), deviceSerialNumber);

            command = new Command(SetParamEnum.SetWl.getComId(), new Object[]{step});
            DetectionConstants.zeroWavelength = s;
            devMessageUtil.wavelengthLocate(command, ip);

            command = new Command(SetParamEnum.Zero.getComId(), new Object[]{});
            R<ChangeParam> zero = null;
            try {
                zero = devMessageUtil.zero(command, ip);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (!zero.getSuccess()) {
                return Result.fail(zero.getMessage());
            }
            ChangeParam changeParam = zero.getData();
            Double zeroWave = formulaUtil.calculateWl(Double.parseDouble(changeParam.getWl()), deviceSerialNumber);
            changeParam.setWl(s);
            list.add(changeParam);
            wave = changeParam.getWl();
            if (StringUtils.hasText(changeParam.getWl())) {
                DetectionConstants.zeroResult.put(zeroWave, changeParam);
            }
        }
        task.setZeroParam(JSON.toJSONString(list));

        fMeasureTaskService.updateById(task);

        deviceInfoService.releaseDevice(deviceSerialNumber);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("waveLength", wave + "nm");
        return Result.ok(jsonObject);
    }

    @Override
    public Result zero2(String deviceSerialNumber, String methodId, String taskId, String updateBy) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            //未连接仪器
            return Result.fail(YamlMessageSource.getMessageStatic("0000001D"));
        }

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }

        if (!StringUtils.hasText(taskId)){
            //创建任务

            //创建任务
            FMeasureTask newTask = new FMeasureTask();

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
            newTask.setEndSaveTime(LocalDateTime.now());
            boolean save = false;
            for (int i = 0; i < 3; i++) {
                if (fMeasureTaskService.save(newTask)) {
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

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);
        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            //缺少波长
            return Result.fail(YamlMessageSource.getMessageStatic("0000001E"));
        }
        List<String> waveLength = parameter.getWaveLength();
        // 通过序列号获取ip
        String ip = deviceInfoService.getIp(deviceSerialNumber);
        if (RationCurveParamEnums.no_differential.equals(parameter.getDifferentialModeName())){
            List<ChangeParam> list = new ArrayList<>();
            for (String s : waveLength) {

                double v = Double.parseDouble(s);
                Command command = new Command(SetParamEnum.SetWl.getComId(), new Object[]{v});
                devMessageUtil.wavelengthLocate(command, ip);

                command = new Command(SetParamEnum.Zero.getComId(), new Object[]{});
                R<ChangeParam> zero = null;
                try {
                    zero = devMessageUtil.zero(command, ip);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (!zero.getSuccess()) {
                    return Result.fail(zero.getMessage());
                }
                ChangeParam changeParam = zero.getData();
                changeParam.setWl(s);
                list.add(changeParam);
            }
            task.setZeroParam(JSON.toJSONString(list));
        }else{
            Map<String, List<ChangeParam>> map = new HashMap<>();
            for (String s : waveLength) {
                double startWaveLength = Double.parseDouble(s)+5;
                double endWaveLength = Double.parseDouble(s)-5;
                List<Command> commands = new ArrayList<>();
                Command com1 = new Command(SetParamEnum.SetStartWl.getComId(), new Object[]{startWaveLength});
                Command com2 = new Command(SetParamEnum.SetEndWl.getComId(), new Object[]{endWaveLength});
                commands.add(com1);
                commands.add(com2);
                R<String> stringR = null;
                try {
                    stringR = devMessageUtil.spectrumScanSet(commands,ip);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (!stringR.getSuccess()) {
                    return Result.fail(stringR.getMessage());
                }
                Command command = new Command(SetParamEnum.BaseLine.getComId(), new Object[]{});
                R<List<ChangeParam>> listR = devMessageUtil.spectrumScanGetBaseline(command, ip);
                if (!listR.getSuccess()) {
                    return Result.fail(listR.getMessage());
                }
                List<ChangeParam> data = listR.getData();
                map.put(s, data);
            }
            task.setZeroParam(JSON.toJSONString(map));
        }
        boolean update = fMeasureTaskService.updateById(task);
        return update ? Result.ok() : Result.fail();
    }

    @Override
    public R<List<FInputParam>> queryInputParam(String methodId) {
        if (!StringUtils.hasText(methodId)) {
            //未选择方法
            return R.fail(YamlMessageSource.getMessageStatic("00000008"));
        }
        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return R.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);
        List<FInputParam> inputParams = parameter.getSampleInputParams();

        return R.ok(inputParams);
    }

    @Override
    public R<List<FInputParamMapTableHead>> queryInputParamMapTableHead(String taskId, String sampleKind) {

        if (!StringUtils.hasText(taskId)) {
            //未选择任务
            return R.fail(YamlMessageSource.getMessageStatic("00000008"));
        }
        if (!StringUtils.hasText(sampleKind)) {
            //未选择样品种类
            return R.fail(YamlMessageSource.getMessageStatic("00303012"));
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        List<FInputParamMapTableHead> ans = new ArrayList<>();
        if (!StringUtils.hasText(task.getTableInfo())) {
            return R.ok(ans);
        }
        FRationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), FRationTaskHeadSnapShot.class);

        if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity())) {
            FTaskHeadSnapshot standHead = shot.getStandHead();
            ans = standHead.getMapRelation();
        } else if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
            FTaskHeadSnapshot unknownHead = shot.getUnknownHead();
            ans = unknownHead.getMapRelation();
        }
        return R.ok(ans);

    }

    @Override
    public R<Boolean> saveInputParamMapTableHead(FTaskHeadSnapshotDTO snapshotDTO) {

        if (!StringUtils.hasText(snapshotDTO.getTaskId())) {
            //未选择任务
            return R.fail(YamlMessageSource.getMessageStatic("0000001B"));
        }
        if (!StringUtils.hasText(snapshotDTO.getSampleKind())) {
            //未选择样品种类
            return R.fail(YamlMessageSource.getMessageStatic("00303012"));
        }

        FMeasureTask task = fMeasureTaskService.getById(snapshotDTO.getTaskId());
        if (task == null) {
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        FTaskHeadSnapshot taskHeadSnapshot = new FTaskHeadSnapshot();
        List<FHeadVO> taskHeads = new ArrayList<>();
        List<FHeadVO> sampleHeads = new ArrayList<>();
        for (FHeadVO head : snapshotDTO.getTaskHeads()) {
            if (head.getLocate().equals(TableHeadILocateEnums.sample.getIdentity()) || head.getLocate().equals(TableHeadILocateEnums.system.getIdentity())) {
                sampleHeads.add(head);
            } else {
                taskHeads.add(head);
            }
        }
        BeanUtils.copyProperties(snapshotDTO, taskHeadSnapshot);
        taskHeadSnapshot.setTaskHeads(taskHeads);
        taskHeadSnapshot.setSampleHeads(sampleHeads);

        FRationTaskHeadSnapShot shot = new FRationTaskHeadSnapShot();
        if (StringUtils.hasText(task.getTableInfo())) {
            shot = JSON.parseObject(task.getTableInfo(), FRationTaskHeadSnapShot.class);
            if (shot == null) {
                shot = new FRationTaskHeadSnapShot();
            }
        }
        if (snapshotDTO.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
            shot.setStandHead(taskHeadSnapshot);
        } else if (snapshotDTO.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {
            shot.setUnknownHead(taskHeadSnapshot);
        } else {
            log.info("样品种类有误");
            //样品种类有误
            return R.fail(YamlMessageSource.getMessageStatic("00303013"));
        }

        task.setTableInfo(JSON.toJSONString(shot));

        //保存失败
        return fMeasureTaskService.updateById(task) ? R.ok(true) : R.fail(YamlMessageSource.getMessageStatic("00300008"));
    }

    /**
     * 多格式文件导出（支持word/txt/xlsx）
     * @param taskId 任务ID
     * @param allDisplay 是否显示全部数据
     * @param fileType 导出格式（word/txt/xlsx）
     * @return 生成的文件对象
     */
    public R<File> downloadFile(String taskId, Boolean allDisplay, String fileType, Integer digit) {
        log.info("开始导出11111111，taskId: {}, fileType: {}, allDisplay: {}, digit: {}", taskId, fileType, allDisplay, digit);
        // 1. 基础参数校验（复用原逻辑，无修改）
        if (!StringUtils.hasText(taskId)) {
            //缺少任务id
            return R.fail(YamlMessageSource.getMessageStatic("00000019"));
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        if (!StringUtils.hasText(fileType)) {
            fileType = "word";
        } else {
            fileType = fileType.toLowerCase();
        }
        if (!"word".equals(fileType) && !"txt".equals(fileType) && !"xlsx".equals(fileType)) {
            //不支持的导出格式，仅支持word/txt/xlsx
            return R.fail(YamlMessageSource.getMessageStatic("00300001"));
        }
        if (allDisplay == null) {
            allDisplay = true;
        }

        // 2. 准备表头和数据（核心：独立获取标准/未知样品表头）
        String methodId = task.getMethodKey();
        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            //测量方法不存在
            return R.fail(YamlMessageSource.getMessageStatic("00300009"));
        }

        // 2.1 获取标准样品表头
        Result<List<FHeadVO>> standHeadResult = getStandSampleTableHead(taskId, methodId, allDisplay, null);
        if (!standHeadResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode())) {
            //获取标准样品表头失败：standHeadResult.getMessage()
            return R.fail(YamlMessageSource.getMessageStatic("00102027",standHeadResult.getMessage()));
        }
        List<FHeadVO> standHeads = standHeadResult.getData();

        // 2.2 获取未知样品表头
        Result<List<FHeadVO>> unknownHeadResult = getUnknownSampleTableHead(taskId, methodId, allDisplay, null);
        if (!unknownHeadResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode())) {
            //"获取未知样品表头失败：" + unknownHeadResult.getMessage()
            return R.fail(YamlMessageSource.getMessageStatic("00102028：" ,unknownHeadResult.getMessage()));
        }
        List<FHeadVO> unknownHeads = unknownHeadResult.getData();
        // 补充未知样品固定列（序号+样品类型）
        if (unknownHeads == null) unknownHeads = new ArrayList<>();
        unknownHeads.add(0, new FHeadVO("序号", "serialNumber"));
        unknownHeads.add(1, new FHeadVO("样品类型", "sampleType"));

        // 2.3 页面设置参数
        int vosSize = standHeads.size();

        // 3. 查询数据（复用原逻辑）
        FQueryDataDTO queryDataDTO = new FQueryDataDTO();
        queryDataDTO.setOption(new ArrayList<>());
        queryDataDTO.setTaskId(taskId);
        queryDataDTO.setAllDisplay(allDisplay);
        queryDataDTO.setDigit(digit);
        Result<FRationSampleDataVO> sampleDataResult = querySampleData(queryDataDTO);
        if (!sampleDataResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode())) {
            //"查询样品数据失败：" + sampleDataResult.getMessage()
            return R.fail(YamlMessageSource.getMessageStatic("00303014：",sampleDataResult.getMessage()));
        }
        FRationSampleDataVO dataVO = sampleDataResult.getData();
        if (dataVO == null) {
            //样品数据为空
            return R.fail(YamlMessageSource.getMessageStatic("00303015"));
        }

        // 4. 数据处理（仅加类型标识）
        List<Map<String, Object>> allData = new ArrayList<>();
        List<Map<String, Object>> standData = dataVO.getStandSampleData();
        if (standData != null && !standData.isEmpty()) {
            for (Map<String, Object> row : standData) {
                row.put("sampleType", "标准样品");
                allData.add(row);
            }
        }
        List<Map<String, Object>> unknownData = dataVO.getUnknowSampleData();
        if (unknownData != null && !unknownData.isEmpty()) {
            for (Map<String, Object> row : unknownData) {
                row.put("sampleType", "未知样品");
                allData.add(row);
            }
        }
        if (allData.isEmpty()) {
            //没有可导出的样品数据
            return R.fail(YamlMessageSource.getMessageStatic("00102029"));
        }

        // 5. 生成文件（关键：传递独立表头）
        String fileName = task.getTaskName() + "." + getFileSuffix(fileType);
        File file = new File(fileName);
        try {
            switch (fileType) {
                case "word":
                    // 调用修改后的generateWordFile，传递standHeads和unknownHeads
                    generateWordFile(file, task, method, standHeads, unknownHeads, allData, vosSize);
                    break;
                case "txt":
                    generateTxtFile(file, standHeads, unknownHeads, allData);
                    break;
                case "xlsx":
                    generateExcelFile(file, standHeads, unknownHeads, allData, taskId, methodId);
                    break;
            }
            return R.ok(file);
        } catch (Exception e) {
            log.error("文件生成失败！taskId={}, fileType={}", taskId, fileType, e);
            if (file.exists()) file.delete();
            //"文件生成失败：" + e.getMessage()
            return R.fail(YamlMessageSource.getMessageStatic("00302006",e.getMessage()));
        }
    }

    // ---------------------- 原有downloadWord方法（可保留或删除，建议用上面的downloadFile替代） ----------------------
    public R<File> downloadWord(String taskId, Boolean allDisplay, Integer digit) {
        // 直接调用多格式方法，指定格式为word
        return downloadFile(taskId, allDisplay, "word", digit);
    }

    @Override
    public R<File> downloadFileWithChildren(String taskId, Boolean allDisplay, String fileType, Integer digit, List<String> option) {
        log.info("开始导出（集成children和option），taskId: {}, fileType: {}, option: {}", taskId, fileType, option);
        
        // 1. 基础参数校验
        if (!StringUtils.hasText(taskId)) {
            //缺少任务id
            return R.fail(YamlMessageSource.getMessageStatic("00000019"));
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            log.error("导出失败：任务不存在，taskId: {}", taskId);
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        
        // 处理fileType默认值和合法性
        if (!StringUtils.hasText(fileType)) {
            fileType = "word"; 
        } else {
            fileType = fileType.toLowerCase();
        }
        if (!"word".equals(fileType) && !"txt".equals(fileType) && !"xlsx".equals(fileType)) {
            //不支持的导出格式，仅支持word/txt/xlsx
            return R.fail(YamlMessageSource.getMessageStatic("00300001"));
        }
        if (allDisplay == null) {
            allDisplay = true;
        }

        // 2. 准备表头和数据（集成children和option）
        String methodId = task.getMethodKey();
        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            //测量方法不存在
            return R.fail(YamlMessageSource.getMessageStatic("00300009"));
        }

        // 2.1 获取标准样品表头
        Result<List<FHeadVO>> standHeadResult = getStandSampleTableHead(taskId, methodId, allDisplay, null);
        if (!standHeadResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode())) {
            //"获取标准样品表头失败：" + standHeadResult.getMessage()
            return R.fail(YamlMessageSource.getMessageStatic("00102027",standHeadResult.getMessage()));
        }
        List<FHeadVO> standHeads = standHeadResult.getData();

        // 2.2 获取未知样品表头
        Result<List<FHeadVO>> unknownHeadResult = getUnknownSampleTableHead(taskId, methodId, allDisplay, null);
        if (!unknownHeadResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode())) {
            //"获取未知样品表头失败：" + unknownHeadResult.getMessage()
            return R.fail(YamlMessageSource.getMessageStatic("00102028",unknownHeadResult.getMessage()));
        }
        List<FHeadVO> unknownHeads = unknownHeadResult.getData();
        // 补充未知样品固定列（序号+样品类型）
        if (unknownHeads == null) unknownHeads = new ArrayList<>();
        unknownHeads.add(0, new FHeadVO("序号", "serialNumber"));
        unknownHeads.add(1, new FHeadVO("样品类型", "sampleType"));

        // 2.3 页面设置参数
        int vosSize = standHeads.size();

        // 3. 查询数据（使用querySampleData方法，传入option参数）
        FQueryDataDTO queryDataDTO = new FQueryDataDTO();
        queryDataDTO.setOption(option != null ? option : new ArrayList<>());
        queryDataDTO.setTaskId(taskId);
        queryDataDTO.setAllDisplay(allDisplay);
        queryDataDTO.setDigit(digit);
        Result<FRationSampleDataVO> sampleDataResult = querySampleData(queryDataDTO);
        if (!sampleDataResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode())) {
            //"查询样品数据失败：" + sampleDataResult.getMessage()
            return R.fail(YamlMessageSource.getMessageStatic("00303014" ,sampleDataResult.getMessage()));
        }
        FRationSampleDataVO dataVO = sampleDataResult.getData();
        if (dataVO == null) {
            //样品数据为空
            return R.fail(YamlMessageSource.getMessageStatic("00303015"));
        }

        // 4. 处理children数据（展开children到主数据中）
        List<Map<String, Object>> allData = new ArrayList<>();
        
        // 处理标准样品数据
        List<Map<String, Object>> standData = dataVO.getStandSampleData();
        if (standData != null && !standData.isEmpty()) {
            for (Map<String, Object> item : standData) {
                // 添加主数据行
                item.put("sampleType", "标准样品");
                allData.add(item);
                
                // 如果有children，展开children数据
                Object childrenObj = item.get("children");
                if (childrenObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) childrenObj;
                    for (Map<String, Object> child : children) {
                        // 为children数据添加父级信息
                        Map<String, Object> childRow = new HashMap<>(child);
                        childRow.put("sampleType", "标准样品");
                        childRow.put("isChildData", true);
                        allData.add(childRow);
                    }
                }
            }
        }
        
        // 处理未知样品数据
        List<Map<String, Object>> unknownData = dataVO.getUnknowSampleData();
        if (unknownData != null && !unknownData.isEmpty()) {
            for (Map<String, Object> item : unknownData) {
                // 添加主数据行
                item.put("sampleType", "未知样品");
                allData.add(item);
                
                // 如果有children，展开children数据
                Object childrenObj = item.get("children");
                if (childrenObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) childrenObj;
                    for (Map<String, Object> child : children) {
                        // 为children数据添加父级信息
                        Map<String, Object> childRow = new HashMap<>(child);
                        childRow.put("sampleType", "未知样品");
                        childRow.put("isChildData", true);
                        allData.add(childRow);
                    }
                }
            }
        }

        // 如果没有数据，构造空行
        if (allData.isEmpty()) {
            log.warn("无实际数据，构造空行导出，taskId: {}", taskId);
            Map<String, Object> emptyRow = new HashMap<>();
            emptyRow.put("serialNumber", 1);
            emptyRow.put("sampleType", "标准样品");
            for (FHeadVO head : standHeads) {
                if (head != null && StringUtils.hasText(head.getIdentity())) {
                    emptyRow.put(head.getIdentity(), "");
                }
            }
            allData.add(emptyRow);
        } else {
            // 重新编号
            for (int i = 0; i < allData.size(); i++) {
                allData.get(i).put("serialNumber", i + 1);
            }
        }

        // 5. 按格式生成文件（关键：传递独立表头）
        String fileName = task.getTaskName() + "." + getFileSuffix(fileType);
        File file = new File(fileName);
        try {
            switch (fileType) {
                case "word":
                    // 调用修改后的generateWordFile，传递standHeads和unknownHeads
                    generateWordFile(file, task, method, standHeads, unknownHeads, allData, vosSize);
                    break;
                case "txt":
                    generateTxtFile(file, standHeads, unknownHeads, allData);
                    break;
                case "xlsx":
                    generateExcelFile(file, standHeads, unknownHeads, allData, taskId, methodId);
                    break;
            }
            return R.ok(file);
        } catch (Exception e) {
            log.error("文件生成失败", e);
            if (file.exists()) {
                file.delete();
            }
            //"文件生成失败：" + e.getMessage()
            return R.fail(YamlMessageSource.getMessageStatic("00302006", e.getMessage()));
        }
    }

    // word导出
    private void generateWordFile(File file, FMeasureTask task, FMeasureMethod method,
                                  List<FHeadVO> standHeads,  // 标准样品表头
                                  List<FHeadVO> unknownHeads, // 未知样品表头
                                  List<Map<String, Object>> allData,
                                  int vosSize) throws Exception {

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
            pageSize.setOrient(STPageOrientation.LANDSCAPE);
        } else {
            pageSize.setOrient(STPageOrientation.PORTRAIT);
        }

        // 添加标题（复用原逻辑）
        XWPFParagraph paragraph = doc.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText("任务：" + task.getTaskName() + "\r");
        run.setText("方法：" + method.getMethodName() + "\r");
        run.setText("测试数据：");

        // -------------------------- 标准样品表格（添加序号列） --------------------------
        List<Map<String, Object>> standData = allData.stream()
                .filter(row -> row != null && "标准样品".equals(row.get("sampleType")))
                .collect(Collectors.toList());
        if (standData != null && !standData.isEmpty()) {
            // 标准样品标题
            XWPFParagraph standTitlePara = doc.createParagraph();
            XWPFRun standTitleRun = standTitlePara.createRun();
            standTitleRun.setText("标准样品数据");
            standTitleRun.setBold(true);
            standTitleRun.setFontSize(12);

            // 1. 计算表格列数（原有表头列数 + 1列序号）
            int originalColumnCount = standHeads != null ? standHeads.size() : 0;
            int columnCount = originalColumnCount + 1; // 新增序号列，总列数+1
            if (columnCount <= 0) {
                log.warn("标准样品表头为空，跳过表格生成！taskId={}", task.getId());
                return;
            }

            // 2. 创建表格（列数=原有列数+1，包含序号列）
            XWPFTable standTable = doc.createTable(1, columnCount);
            standTable.setWidth("100%");

            // 3. 填充表头（第0列为"序号"，后续为原有standHeads表头）
            XWPFTableRow standHeaderRow = standTable.getRow(0);
            if (standHeaderRow == null) {
                log.error("标准样品表头行对象为null！");
                throw new RuntimeException("标准样品表头行对象为空");
            }

            // 第0列：序号表头
            XWPFTableCell indexHeaderCell = standHeaderRow.getCell(0);
            if (indexHeaderCell == null) {
                indexHeaderCell = standHeaderRow.createCell();
            }
            indexHeaderCell.setText("序号");
            // 序号表头加粗（与其他表头样式一致）
            XWPFParagraph indexHeaderPara = indexHeaderCell.getParagraphArray(0);
            if (indexHeaderPara != null && !indexHeaderPara.getRuns().isEmpty()) {
                indexHeaderPara.getRuns().get(0).setBold(true);
            }

            // 后续列：原有standHeads表头（跳过可能存在的序号列，避免重复）
            for (int i = 0; i < originalColumnCount; i++) {
                FHeadVO head = (standHeads != null && i < standHeads.size()) ? standHeads.get(i) : null;
                // 跳过原表头中可能存在的序号列（按identity或name判断）
                if (head != null && ("serialNumber".equals(head.getIdentity()) || "序号".equals(head.getName()))) {
                    continue;
                }

                // 获取当前列的单元格（i+1：跳过第0列序号列）
                XWPFTableCell cell = standHeaderRow.getCell(i + 1);
                if (cell == null) {
                    cell = standHeaderRow.createCell();
                }

                // 处理表头名称
                String headName = "未命名列";
                if (head != null) {
                    headName = head.getName() != null ? head.getName().trim() : headName;
                    if (headName.isEmpty()) {
                        headName = "未命名列（" + (head.getIdentity() != null ? head.getIdentity() : i) + "）";
                    }
                }
                cell.setText(headName);

                // 表头加粗
                XWPFParagraph cellPara = cell.getParagraphArray(0);
                if (cellPara != null && !cellPara.getRuns().isEmpty()) {
                    cellPara.getRuns().get(0).setBold(true);
                }
            }

            // 4. 填充数据行（第0列为序号，后续为原有数据）
            int standRowIndex = 0;
            for (Map<String, Object> map : standData) {
                if (map == null) continue;
                // 创建新行（列数与表头一致）
                XWPFTableRow row = standTable.createRow();
                standRowIndex++;

                // 第0列：序号（自增）
                XWPFTableCell indexCell = row.getCell(0);
                if (indexCell != null) {
                    indexCell.setText(String.valueOf(standRowIndex));
                }

                // 后续列：填充原有standHeads对应的数据（跳过原有序号列）
                int dataColumnIndex = 1; // 从第1列开始填充数据（跳过序号列）
                for (int i = 0; i < originalColumnCount; i++) {
                    FHeadVO head = (standHeads != null && i < standHeads.size()) ? standHeads.get(i) : null;
                    // 跳过原表头中的序号列，避免重复填充
                    if (head != null && ("serialNumber".equals(head.getIdentity()) || "序号".equals(head.getName()))) {
                        continue;
                    }

                    // 获取当前数据列的单元格
                    XWPFTableCell cell = row.getCell(dataColumnIndex);
                    if (cell == null) {
                        cell = row.createCell();
                    }

                    // 处理单元格数据
                    String cellData = "-";
                    if (head != null && head.getIdentity() != null) {
                        Object originalValue = map.get(head.getIdentity());
                        cellData = (originalValue == null || originalValue.toString().trim().isEmpty()) ? "-" : originalValue.toString();
                    }
                    cell.setText(cellData);

                    dataColumnIndex++; // 数据列索引自增（跳过序号列）
                }
            }

            // 5. 表格样式（添加边框，与未知样品表格样式一致）
            for (XWPFTableRow row : standTable.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    XWPFParagraph para = cell.getParagraphArray(0);
                    if (para != null) para.setAlignment(ParagraphAlignment.CENTER);
                    cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                    CTTcBorders borders = cell.getCTTc().addNewTcPr().addNewTcBorders();
                    borders.addNewTop().setVal(STBorder.SINGLE);
                    borders.addNewBottom().setVal(STBorder.SINGLE);
                    borders.addNewLeft().setVal(STBorder.SINGLE);
                    borders.addNewRight().setVal(STBorder.SINGLE);
                }
            }
        }

        // -------------------------- 未知样品表格（保留原逻辑，确保与标准样品格式对齐） --------------------------
        List<Map<String, Object>> unknownData = allData.stream()
                .filter(row -> row != null && "未知样品".equals(row.get("sampleType")))
                .collect(Collectors.toList());
        if (unknownData != null && !unknownData.isEmpty()) {
            // 未知样品标题（空行分隔）
            doc.createParagraph();
            XWPFParagraph unknownTitlePara = doc.createParagraph();
            XWPFRun unknownTitleRun = unknownTitlePara.createRun();
            unknownTitleRun.setText("未知样品数据");
            unknownTitleRun.setBold(true);
            unknownTitleRun.setFontSize(12);

            // 计算表格列数（原有表头列数，确保包含序号和样品类型列）
            int unknownColumnCount = unknownHeads != null ? unknownHeads.size() : 0;
            if (unknownColumnCount <= 0) {
                log.warn("未知样品表头为空，跳过表格生成！taskId={}", task.getId());
                return;
            }
            XWPFTable unknownTable = doc.createTable(1, unknownColumnCount);
            unknownTable.setWidth("100%");

            // 填充表头（使用unknownHeads）
            XWPFTableRow unknownHeaderRow = unknownTable.getRow(0);
            if (unknownHeaderRow == null) {
                log.error("未知样品表头行对象为null！");
                throw new RuntimeException("未知样品表头行对象为空");
            }
            for (int i = 0; i < unknownColumnCount; i++) {
                XWPFTableCell cell = unknownHeaderRow.getCell(i);
                if (cell == null) {
                    cell = unknownHeaderRow.createCell();
                }
                FHeadVO head = (unknownHeads != null && i < unknownHeads.size()) ? unknownHeads.get(i) : null;
                String headName = "未命名列";
                if (head != null) {
                    headName = head.getName() != null ? head.getName().trim() : headName;
                    if (headName.isEmpty()) {
                        headName = "未命名列（" + (head.getIdentity() != null ? head.getIdentity() : i) + "）";
                    }
                }
                cell.setText(headName);
                // 表头加粗
                XWPFParagraph cellPara = cell.getParagraphArray(0);
                if (cellPara != null && !cellPara.getRuns().isEmpty()) {
                    cellPara.getRuns().get(0).setBold(true);
                }
            }

            // 填充数据行（第0列序号，第1列样品类型）
            int unknownRowIndex = 0;
            for (Map<String, Object> map : unknownData) {
                if (map == null) continue;
                XWPFTableRow row = unknownTable.createRow();
                unknownRowIndex++;
                // 第0列：序号
                XWPFTableCell indexCell = row.getCell(0);
                if (indexCell != null) {
                    indexCell.setText(String.valueOf(unknownRowIndex));
                }
                // 第1列：样品类型（固定为“未知样品”）
                XWPFTableCell typeCell = row.getCell(1);
                if (typeCell != null) {
                    typeCell.setText("未知样品");
                }
                // 填充其他列
                for (int i = 2; i < unknownColumnCount; i++) {
                    FHeadVO head = (unknownHeads != null && i < unknownHeads.size()) ? unknownHeads.get(i) : null;
                    if (head == null) continue;

                    String identity = head.getIdentity();
                    Object originalValue = map.get(identity);
                    String cellData = (originalValue == null || originalValue.toString().trim().isEmpty()) ? "-" : originalValue.toString();

                    XWPFTableCell cell = row.getCell(i);
                    if (cell != null) {
                        cell.setText(cellData);
                    }
                }
            }

            // 表格样式（添加边框）
            for (XWPFTableRow row : unknownTable.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    XWPFParagraph para = cell.getParagraphArray(0);
                    if (para != null) para.setAlignment(ParagraphAlignment.CENTER);
                    cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                    CTTcBorders borders = cell.getCTTc().addNewTcPr().addNewTcBorders();
                    borders.addNewTop().setVal(STBorder.SINGLE);
                    borders.addNewBottom().setVal(STBorder.SINGLE);
                    borders.addNewLeft().setVal(STBorder.SINGLE);
                    borders.addNewRight().setVal(STBorder.SINGLE);
                }
            }
        }

        // 写入文件
        try (FileOutputStream out = new FileOutputStream(file)) {
            doc.write(out);
            log.info("Word文件生成成功！路径：{}", file.getAbsolutePath());
        } catch (Exception e) {
            log.error("Word文件写入失败！taskId={}", task.getId(), e);
            throw e;
        } finally {
            doc.close();
        }
    }

    // txt导出
    private void generateTxtFile(File file,
                                 List<FHeadVO> standHeads,  // 标准样品表头
                                 List<FHeadVO> unknownHeads, // 未知样品表头
                                 List<Map<String, Object>> allData) throws Exception {
        // 前置校验（与Word逻辑对齐）
        if (standHeads == null) {
            throw new IllegalArgumentException("标准样品表头standHeads不能为null");
        }
        if (unknownHeads == null) {
            throw new IllegalArgumentException("未知样品表头unknownHeads不能为null");
        }
        if (allData == null) {
            allData = new ArrayList<>();
            log.warn("数据列表allData为null，已初始化为空集合");
        }
        if (file == null) {
            throw new IllegalArgumentException("输出文件File不能为null");
        }

        // 确保父目录存在（修复路径问题）
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                String err = "创建父目录失败：" + parentDir.getAbsolutePath();
                log.error(err);
                throw new RuntimeException(err);
            }
        }

        // 数据分组（与Word完全一致的过滤逻辑）
        List<Map<String, Object>> standData = allData.stream()
                .filter(row -> row != null && "标准样品".equals(row.get("sampleType")))
                .collect(Collectors.toList());
        List<Map<String, Object>> unknownData = allData.stream()
                .filter(row -> row != null && "未知样品".equals(row.get("sampleType")))
                .collect(Collectors.toList());
        log.info("数据分组完成：标准样品{}行，未知样品{}行", standData.size(), unknownData.size());

        // 写入文件
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            // -------------------------- 标准样品数据（添加序号列） --------------------------
            if (!standData.isEmpty()) {
                // 1. 标准样品标题
                writer.write("===== 标准样品数据 =====");
                writer.newLine();

                // 2. 标准样品表头（第0列为"序号"，后续为standHeads的表头）
                List<String> standHeaderList = new ArrayList<>();
                // 第0列：固定添加"序号"表头（与Word一致）
                standHeaderList.add("序号");
                // 后续列：添加standHeads的表头（排除原有可能的序号列，避免重复）
                for (FHeadVO head : standHeads) {
                    if (head == null) {
                        standHeaderList.add("未命名列");
                        continue;
                    }
                    // 跳过standHeads中可能存在的"序号"列（防止重复）
                    if ("serialNumber".equals(head.getIdentity()) || "序号".equals(head.getName())) {
                        continue;
                    }
                    String headName = head.getName() != null ? head.getName().trim() : "未命名列";
                    if (headName.isEmpty()) {
                        headName = "未命名列（" + (head.getIdentity() != null ? head.getIdentity() : "") + "）";
                    }
                    standHeaderList.add(headName);
                }
                // 拼接表头行
                String standHeaderLine = String.join("\t", standHeaderList);
                writer.write(standHeaderLine);
                writer.newLine();

                // 3. 标准样品数据行（第0列为序号，从1开始）
                int standRowIndex = 0;
                for (Map<String, Object> map : standData) {
                    if (map == null) {
                        log.warn("标准样品数据行对象为null，跳过");
                        continue;
                    }
                    standRowIndex++; // 序号自增

                    List<String> rowData = new ArrayList<>();
                    // 第0列：固定填充序号
                    rowData.add(String.valueOf(standRowIndex));

                    // 后续列：按standHeads的identity取数据（跳过原有序号列）
                    for (FHeadVO head : standHeads) {
                        if (head == null) {
                            rowData.add("-");
                            continue;
                        }
                        // 跳过原有序号列，避免重复填充
                        if ("serialNumber".equals(head.getIdentity()) || "序号".equals(head.getName())) {
                            continue;
                        }

                        String identity = head.getIdentity();
                        Object value = map.get(identity);
                        String cellData = value != null ? value.toString().trim() : "-";
                        if (cellData.isEmpty() || "null".equalsIgnoreCase(cellData)) {
                            cellData = "-";
                        }
                        rowData.add(cellData);
                    }

                    writer.write(String.join("\t", rowData));
                    writer.newLine();
                }

                writer.newLine(); // 空行分隔
            }

            // -------------------------- 未知样品数据（保留原有序号和样品类型列） --------------------------
            if (!unknownData.isEmpty()) {
                // 1. 未知样品标题
                writer.write("===== 未知样品数据 =====");
                writer.newLine();

                // 2. 未知样品表头（第0列序号，第1列样品类型，后续为unknownHeads表头）
                List<String> unknownHeaderList = new ArrayList<>();
                unknownHeaderList.add("序号"); // 第0列：序号
                unknownHeaderList.add("样品类型"); // 第1列：样品类型
                // 后续列：添加unknownHeads表头（排除原有序号和样品类型列）
                for (FHeadVO head : unknownHeads) {
                    if (head == null) {
                        unknownHeaderList.add("未命名列");
                        continue;
                    }
                    // 跳过原有序号和样品类型列
                    if ("serialNumber".equals(head.getIdentity()) || "sampleType".equals(head.getIdentity())
                            || "序号".equals(head.getName()) || "样品类型".equals(head.getName())) {
                        continue;
                    }
                    String headName = head.getName() != null ? head.getName().trim() : "未命名列";
                    if (headName.isEmpty()) {
                        headName = "未命名列（" + (head.getIdentity() != null ? head.getIdentity() : "") + "）";
                    }
                    unknownHeaderList.add(headName);
                }
                String unknownHeaderLine = String.join("\t", unknownHeaderList);
                writer.write(unknownHeaderLine);
                writer.newLine();

                // 3. 未知样品数据行（第0列序号，第1列固定"未知样品"）
                int unknownRowIndex = 0;
                for (Map<String, Object> map : unknownData) {
                    if (map == null) {
                        log.warn("未知样品数据行对象为null，跳过");
                        continue;
                    }
                    unknownRowIndex++;

                    List<String> rowData = new ArrayList<>();
                    rowData.add(String.valueOf(unknownRowIndex)); // 第0列：序号
                    rowData.add("未知样品"); // 第1列：固定样品类型

                    // 后续列：按unknownHeads的identity取数据
                    for (FHeadVO head : unknownHeads) {
                        if (head == null) {
                            rowData.add("-");
                            continue;
                        }
                        // 跳过原有序号和样品类型列
                        if ("serialNumber".equals(head.getIdentity()) || "sampleType".equals(head.getIdentity())
                                || "序号".equals(head.getName()) || "样品类型".equals(head.getName())) {
                            continue;
                        }

                        String identity = head.getIdentity();
                        Object value = map.get(identity);
                        String cellData = value != null ? value.toString().trim() : "-";
                        if (cellData.isEmpty() || "null".equalsIgnoreCase(cellData)) {
                            cellData = "-";
                        }
                        rowData.add(cellData);
                    }

                    writer.write(String.join("\t", rowData));
                    writer.newLine();
                }
            }

            log.info("TXT文件生成成功，路径：{}", file.getAbsolutePath());
        } catch (Exception e) {
            log.error("TXT文件生成失败", e);
            if (file.exists() && !file.delete()) {
                log.warn("删除无效TXT文件失败：{}", file.getAbsolutePath());
            }
            throw e;
        }
    }

    // excel导出
    private void generateExcelFile(File file,
                                   List<FHeadVO> standHeads,
                                   List<FHeadVO> unknownHeads,  // 未知样品表头
                                   List<Map<String, Object>> allData,
                                   String taskId,
                                   String methodId) throws Exception {
        // 1. 参数校验（强化非空校验）
        if (standHeads == null) {
            throw new IllegalArgumentException("标准样品表头standHeads不能为null");
        }
        if (unknownHeads == null) {
            throw new IllegalArgumentException("未知样品表头unknownHeads不能为null");
        }
        if (allData == null) {
            allData = new ArrayList<>(); // 允许空数据，避免直接抛异常
            log.warn("数据列表allData为null，已初始化为空集合");
        }
        if (file == null) {
            throw new IllegalArgumentException("输出文件file不能为null");
        }
        if (taskId == null || methodId == null) {
            throw new IllegalArgumentException("taskId和methodId不能为null");
        }

        // 确保父目录存在（修复路径问题）
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new RuntimeException("创建父目录失败：" + parentDir.getAbsolutePath());
        }

        // 2. 处理标准样品表头（添加序号列作为第0列）
        List<List<String>> standExcelHeads = new ArrayList<>();
        // 第0列：固定添加"序号"表头
        standExcelHeads.add(Collections.singletonList("序号"));
        // 后续列：添加原有standHeads（跳过可能存在的序号列，避免重复）
        for (int i = 0; i < standHeads.size(); i++) {
            FHeadVO head = standHeads.get(i);
            // 跳过原表头中可能存在的序号列（根据identity或name判断）
            if (head != null && ("serialNumber".equals(head.getIdentity()) || "序号".equals(head.getName()))) {
                continue;
            }
            // 处理表头名称
            String headName = head != null && head.getName() != null ? head.getName().trim() : "未命名列_" + i;
            if (headName.isEmpty()) {
                String identity = head != null && head.getIdentity() != null ? head.getIdentity() : "unknown_" + i;
                headName = "未命名列（" + identity + "）";
            }
            standExcelHeads.add(Collections.singletonList(headName));
        }

        // 3. 处理标准样品数据（添加序号列作为第0列）
        List<List<Object>> standExcelData = new ArrayList<>();
        List<Map<String, Object>> standData = allData.stream()
                .filter(row -> row != null && "标准样品".equals(row.get("sampleType")))
                .collect(Collectors.toList());
        int standRowIndex = 0; // 序号从1开始
        for (Map<String, Object> map : standData) {
            standRowIndex++; // 自增序号
            List<Object> row = new ArrayList<>();
            // 第0列：添加序号
            row.add(standRowIndex);
            // 后续列：按standHeads填充数据（跳过原有序号列）
            for (FHeadVO head : standHeads) {
                // 跳过原表头中的序号列，避免重复填充
                if (head != null && ("serialNumber".equals(head.getIdentity()) || "序号".equals(head.getName()))) {
                    continue;
                }
                // 处理单元格数据
                String cellData = "-";
                if (head != null && head.getIdentity() != null) {
                    Object value = map.get(head.getIdentity());
                    if (value != null) {
                        cellData = String.valueOf(value)
                                .replace("\r", "")
                                .replace("\n", "")
                                .replace("\t", " ");
                        if (cellData.isEmpty() || "null".equalsIgnoreCase(cellData)) {
                            cellData = "-";
                        }
                        // 样品位置特殊处理
                        if ("sampleLocation".equals(head.getIdentity())) {
                            cellData = "样品" + cellData;
                        }
                    }
                }
                row.add(cellData);
            }
            standExcelData.add(row);
        }

        // 4. 处理未知样品表头（修复序号列显示）
        List<List<String>> unknownExcelHeads = new ArrayList<>();
        // 第0列：固定添加"序号"表头（与标准样品对齐）
        unknownExcelHeads.add(Collections.singletonList("序号"));
        // 第1列：固定添加"样品类型"表头（与Word/TXT逻辑一致）
        unknownExcelHeads.add(Collections.singletonList("样品类型"));
        // 后续列：添加原有unknownHeads（跳过可能存在的序号和样品类型列）
        for (int i = 0; i < unknownHeads.size(); i++) {
            FHeadVO head = unknownHeads.get(i);
            // 跳过原表头中可能存在的序号列或样品类型列
            if (head != null && (
                    "serialNumber".equals(head.getIdentity()) || "序号".equals(head.getName()) ||
                            "sampleType".equals(head.getIdentity()) || "样品类型".equals(head.getName())
            )) {
                continue;
            }
            // 处理表头名称
            String headName = head != null && head.getName() != null ? head.getName().trim() : "未命名列_" + i;
            if (headName.isEmpty()) {
                String identity = head != null && head.getIdentity() != null ? head.getIdentity() : "unknown_" + i;
                headName = "未命名列（" + identity + "）";
            }
            unknownExcelHeads.add(Collections.singletonList(headName));
        }

        // 5. 处理未知样品数据（修复序号为"-"的问题）
        List<List<Object>> unknownExcelData = new ArrayList<>();
        List<Map<String, Object>> unknownData = allData.stream()
                .filter(row -> row != null && "未知样品".equals(row.get("sampleType")))
                .collect(Collectors.toList());
        int unknownRowIndex = 0; // 序号从1开始
        for (Map<String, Object> map : unknownData) {
            unknownRowIndex++; // 自增序号
            List<Object> row = new ArrayList<>();
            // 第0列：添加序号（修复原逻辑中显示为"-"的问题）
            row.add(unknownRowIndex);
            // 第1列：固定显示"未知样品"（与Word/TXT逻辑一致）
            row.add("未知样品");
            // 后续列：按unknownHeads填充数据（跳过原有序号和样品类型列）
            for (FHeadVO head : unknownHeads) {
                // 跳过原表头中的序号列或样品类型列，避免重复填充
                if (head != null && (
                        "serialNumber".equals(head.getIdentity()) || "序号".equals(head.getName()) ||
                                "sampleType".equals(head.getIdentity()) || "样品类型".equals(head.getName())
                )) {
                    continue;
                }
                // 处理单元格数据
                String cellData = "-";
                if (head != null && head.getIdentity() != null) {
                    Object value = map.get(head.getIdentity());
                    if (value != null) {
                        cellData = String.valueOf(value)
                                .replace("\r", "")
                                .replace("\n", "")
                                .replace("\t", " ");
                        if (cellData.isEmpty() || "null".equalsIgnoreCase(cellData)) {
                            cellData = "-";
                        }
                        // 样品位置特殊处理
                        if ("sampleLocation".equals(head.getIdentity())) {
                            cellData = "样品" + cellData;
                        }
                    }
                }
                row.add(cellData);
            }
            unknownExcelData.add(row);
        }

        // 6. 写入Excel（使用EasyExcel）
        try (OutputStream out = new FileOutputStream(file)) {
            ExcelWriter writer = EasyExcel.write(out).build();
            // 标准样品Sheet（即使无数据也创建Sheet，保持结构完整）
            writer.write(standExcelData, EasyExcel.writerSheet(0, "标准样品").head(standExcelHeads).build());
            // 未知样品Sheet（有数据才创建，避免空Sheet）
            if (!unknownExcelData.isEmpty()) {
                writer.write(unknownExcelData, EasyExcel.writerSheet(1, "未知样品").head(unknownExcelHeads).build());
            }
            writer.finish();
            log.info("Excel生成成功：{}，标准样品{}行，未知样品{}行",
                    file.getAbsolutePath(), standExcelData.size(), unknownExcelData.size());
        } catch (Exception e) {
            log.error("Excel生成失败，taskId={}", taskId, e);
            // 清理无效文件
            if (file.exists() && !file.delete()) {
                log.warn("删除无效Excel文件失败：{}", file.getAbsolutePath());
            }
            throw e; // 抛出异常，让上层处理
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
}
