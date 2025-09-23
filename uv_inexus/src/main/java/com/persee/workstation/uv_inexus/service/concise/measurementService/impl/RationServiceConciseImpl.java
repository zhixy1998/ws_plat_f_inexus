package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;


import com.alibaba.excel.EasyExcel;
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
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FRationAddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FTaskHeadSnapshotDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.NewRationMethodParamCheckDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.*;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingParam;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingReceiveInfo;
import com.persee.workstation.uv_inexus.util.pythonUtil.RestUtil;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FMeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FMethodChangeCheckVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FSampleVO;
import com.persee.workstation.uv_inexus.vo.concise.ration.*;
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
    RestUtil restUtil;


    @Value("1")
    private String deleteType;

    @Value("0")
    private String createType;

    @Value("0")
    String noSave;

    @Value("1")
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

    @Override
    public Result updateMethodParameter(MethodParamsDto methodDto) {
        if (!StringUtils.hasText(methodDto.getMethodId())){
            return Result.fail("方法id不能为空");
        }

        // 根据方法id查询当前方法
        FMeasureMethod method = fMeasureMethodService.getById(methodDto.getMethodId());
        if (method == null) {
            return Result.fail("方法不存在");
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
            return Result.fail("请填写波长参数");
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
            return Result.ok("创建成功");
        }else {
            return Result.fail("创建失败");
        }
    }

    @Override
    public Result<FMeasurementCurrentInfoVO> newDraft(String methodId, String updateBy) {

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

        List<FHeadVO> standHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), SampleTypeEnum.kind_stand.getIdentity());
        List<FHeadVO> unknownHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), SampleTypeEnum.kind_unknown.getIdentity());

        FRationTaskHeadSnapShot rationTaskHeadSnapShot = new FRationTaskHeadSnapShot();

        FTaskHeadSnapshot standSnapshot = new FTaskHeadSnapshot();
        standSnapshot.setTaskHeads(standHead);
        rationTaskHeadSnapShot.setStandHead(standSnapshot);

        FTaskHeadSnapshot unKnowSnapshot = new FTaskHeadSnapshot();
        unKnowSnapshot.setTaskHeads(unknownHead);
        rationTaskHeadSnapShot.setStandHead(unKnowSnapshot);

        task.setTableInfo(JSON.toJSONString(rationTaskHeadSnapShot));

        boolean save = fMeasureTaskService.save(task);
        FMeasurementCurrentInfoVO vo = new FMeasurementCurrentInfoVO();
        vo.setMethodId(task.getMethodKey());

        if (save) {
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getTaskName());
            return Result.ok(vo);
        } else {
            return Result.fail("新建失败");
        }
    }

    @Override
    public Result<FMethodChangeCheckVO> checkMethodParam(NewRationMethodParamCheckDTO checkDTO) {

        if (checkDTO.getNewMethodParam() == null) {
            return Result.fail("没有可应用的方法参数");
        }

        FMethodChangeCheckVO vo = new FMethodChangeCheckVO();

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

        FRationParameter methodParam = checkDTO.getNewMethodParam();

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
        List<FPreCountResultParam> oldResult = oldParam.getResultParams();
        List<FPreCountResultParam> newResult = newParam.getResultParams();
        if (!oldResult.equals(newResult)) {
            if (newResult.size() != oldResult.size()) {
                return false;
            }

            for (int i = 0; i < oldResult.size(); i++) {
                FPreCountResultParam oldResultParam = oldResult.get(i);
                FPreCountResultParam newResultParam = newResult.get(i);
                if (oldResultParam.equals(newResultParam)) {
                    continue;
                }
                if (!oldResultParam.getFormulaText().equals(newResultParam.getFormulaText())) {
                    return false;
                }
            }
        }
        return true;
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
        return oldParam.getCurveType().equals(newParam.getCurveType()) &&
                oldParam.getEquationType().equals(newParam.getEquationType());

    }


    @Override
    public Result<FMeasurementCurrentInfoVO> applyMethod(NewRationMethodParamCheckDTO methodDTO) {

        if (methodDTO.getNewMethodParam() == null && !StringUtils.hasText(methodDTO.getNewMethodId())) {
            return Result.fail("没有可应用的方法或参数");
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
            if (StringUtils.hasText(methodDTO.getMethodName())) {
                String[] split = methodDTO.getMethodName().split("-");
                method.setMethodName(split[0] + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            } else {
                method.setMethodName(newTaskName + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            }
            method.setMeasurementType(methodDTO.getMeasureType());
            method.setProjectItemKey(defaultProjectId);
            method.setMethodCreator(methodDTO.getUpdateBy());
            method.setMethodCreateTime(LocalDateTime.now());
            method.setMethodUseTime(LocalDateTime.now());
            boolean save = fMeasureMethodService.save(method);

            if (!save) {
                return Result.fail("应用失败");
            }
            methodId = method.getId();
            ans.setMethodName(method.getMethodName());
        }
        ans.setMethodId(methodId);

        FMeasureTask task = new FMeasureTask();
        if (!StringUtils.hasText(methodDTO.getCurrentTaskId())) {
            //任务不存在，创建任务和方法
            task.setTaskName(newTaskName + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            task.setMethodKey(methodId);
            task.setCreateTime(LocalDateTime.now());
            task.setUserId(userId);
            task.setProjectItemKey(defaultProjectId);
            task.setIsDelete(createType);
            task.setMeasureType(methodDTO.getMeasureType());

            List<FHeadVO> standHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), SampleTypeEnum.kind_stand.getIdentity());
            List<FHeadVO> unknownHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), SampleTypeEnum.kind_unknown.getIdentity());

            FRationTaskHeadSnapShot rationTaskHeadSnapShot = new FRationTaskHeadSnapShot();

            FTaskHeadSnapshot standSnapshot = new FTaskHeadSnapshot();
            standSnapshot.setTaskHeads(standHead);
            rationTaskHeadSnapShot.setStandHead(standSnapshot);

            FTaskHeadSnapshot unKnowSnapshot = new FTaskHeadSnapshot();
            unKnowSnapshot.setTaskHeads(unknownHead);
            rationTaskHeadSnapShot.setStandHead(unKnowSnapshot);

            task.setTableInfo(JSON.toJSONString(rationTaskHeadSnapShot));

            fMeasureTaskService.save(task);

            ans.setTaskId(task.getId());
            ans.setTaskName(task.getTaskName());
            return Result.ok(ans);

        } else {
            task = fMeasureTaskService.getById(methodDTO.getCurrentTaskId());

            if (task == null) {
                task = new FMeasureTask();
                task.setTaskName(newTaskName + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
                task.setMethodKey(methodId);
                task.setCreateTime(LocalDateTime.now());
                task.setUserId(userId);
                task.setProjectItemKey(defaultProjectId);
                task.setIsDelete(createType);
                task.setMeasureType(methodDTO.getMeasureType());

                List<FHeadVO> standHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), SampleTypeEnum.kind_stand.getIdentity());
                List<FHeadVO> unknownHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), SampleTypeEnum.kind_unknown.getIdentity());

                FRationTaskHeadSnapShot rationTaskHeadSnapShot = new FRationTaskHeadSnapShot();

                FTaskHeadSnapshot standSnapshot = new FTaskHeadSnapshot();
                standSnapshot.setTaskHeads(standHead);
                rationTaskHeadSnapShot.setStandHead(standSnapshot);

                FTaskHeadSnapshot unKnowSnapshot = new FTaskHeadSnapshot();
                unKnowSnapshot.setTaskHeads(unknownHead);
                rationTaskHeadSnapShot.setStandHead(unKnowSnapshot);

                task.setTableInfo(JSON.toJSONString(rationTaskHeadSnapShot));

                fMeasureTaskService.save(task);

                ans.setTaskId(task.getId());
                ans.setTaskName(task.getTaskName());
                return Result.ok(ans);
            }

            if (!StringUtils.hasText(task.getSampleKey())) {
                task.setMethodKey(methodId);
                fMeasureTaskService.updateById(task);

                ans.setTaskId(task.getId());
                ans.setTaskName(task.getTaskName());
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

                    newTask.setTableInfo(task.getTableInfo());

                    newTask.setId(null);
                    newTask.setMethodKey(methodId);
                    newTask.setDeviceSerialNumber(null);
                    newTask.setIsDelete(createType);
                    newTask.setCreateTime(LocalDateTime.now());
                    newTask.setMeasureType(methodDTO.getMeasureType());
                    fMeasureTaskService.save(newTask);
                    ans.setTaskId(newTask.getId());
                    ans.setTaskName(newTask.getTaskName());
                    return Result.ok(ans);

                } else if (methodDTO.getClearDataRange().equals(RationChangeMethodTypeEnums.curve.getIdentity())) {
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

                    ans.setTaskId(task.getId());
                    ans.setTaskName(task.getTaskName());
                    return Result.ok(ans);

                } else if (methodDTO.getClearDataRange().equals(RationChangeMethodTypeEnums.analyze.getIdentity())) {
                    task.setMethodKey(methodId);
                    fMeasureTaskService.updateById(task);

                    //TODO 公式重算


                    ans.setTaskId(task.getId());
                    ans.setTaskName(task.getTaskName());
                    return Result.ok(ans);
                } else {
                    task.setMethodKey(methodId);
                    fMeasureTaskService.updateById(task);
                    ans.setTaskId(task.getId());
                    ans.setTaskName(task.getTaskName());
                    return Result.ok(ans);
                }
            } else {
                //保存数据

                saveData(task.getId());

                FMeasureTask newTask = new FMeasureTask();
                newTask.setProjectItemKey(defaultProjectId);
                newTask.setUserId(userId);
                newTask.setTaskName(newTaskName + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
                newTask.setTableInfo(task.getTableInfo());

                newTask.setId(null);
                newTask.setMethodKey(methodId);
                newTask.setDeviceSerialNumber(null);
                newTask.setIsDelete(createType);
                fMeasureTaskService.save(newTask);

                ans.setTaskId(newTask.getId());
                ans.setTaskName(newTask.getTaskName());
                return Result.ok(ans);

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

    @Override
    public Result<FMeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType) {

        SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
        String id = po.getId();

        LambdaQueryWrapper<FMeasureTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FMeasureTask::getUserId, id)
                .eq(FMeasureTask::getMeasureType, measureType)
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

            return Result.ok(vo);
        }

        FMeasurementCurrentInfoVO vo = new FMeasurementCurrentInfoVO();

        LambdaQueryWrapper<FMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureMethod::getMethodCreator, updateBy)
                .eq(FMeasureMethod::getMeasurementType, measureType)
                .orderByDesc(FMeasureMethod::getMethodCreateTime);
        List<FMeasureMethod> list = fMeasureMethodService.list(queryWrapper);
        if (list == null|| list.isEmpty()){
            LambdaQueryWrapper<FMeasureProject> projectWrapper = new LambdaQueryWrapper<>();
            projectWrapper.eq(FMeasureProject::getCreatorKey, updateBy)
                    .orderByDesc(FMeasureProject::getCreateTime);
            List<FMeasureProject> projectList = fMeasureProjectService.list(projectWrapper);
            if (projectList == null || projectList.isEmpty()){
                return Result.ok(vo);
            }

        } else {
            FMeasureMethod fMeasureMethod = list.get(0);

            vo.setMethodId(fMeasureMethod.getId());
            vo.setMethodName(fMeasureMethod.getMethodName());
        }
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
            return Result.fail("任务不存在");
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
            return Result.fail("缺少样品种类");
        }

        if (!StringUtils.hasText(taskId)) {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), sampleKind);
            return Result.ok(preHead);
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
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
                return Result.fail("样品种类出错");
            }
        }

    }


    @Override
//    @DS("concise")
//    @Transactional
    public Result addOrDeleteSample(FRationAddOrDeleteSampleDTO dto) {

        if (!StringUtils.hasText(dto.getMethodId())) {
            return Result.fail("没有方法！");
        }

        if (dto.getStandHeads() == null || dto.getStandHeads().isEmpty()) {
            return Result.fail("标准样品表头为空");
        }

        if (dto.getUnknowHeads() == null || dto.getUnknowHeads().isEmpty()) {
            return Result.fail("未知样品表头为空");
        }

        if (dto.getStandHeads().size() > 10) {
            return Result.fail("标准样品表格新增列数不能超过10列");
        }

        if (dto.getUnknowHeads().size() > 10) {
            return Result.fail("未知样品表格新增列数不能超过10列");
        }

        if (!StringUtils.hasText(dto.getTaskId())) {
            //创建任务
            FMeasureTask newTask = new FMeasureTask();

            FMeasureMethod method = fMeasureMethodService.getById(dto.getMethodId());
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
                if (fMeasureTaskService.save(newTask)) {
                    save = true;
                    break;
                }
            }
            if (!save) {
                return Result.fail("系统出错");
            }
            dto.setTaskId(newTask.getId());
        }

        FMeasureTask task = fMeasureTaskService.getById(dto.getTaskId());
        if (task == null) {
            return Result.fail("任务不存在");
        }

        List<FTaskSpecimen> taskSpecimen = new ArrayList<>();
        if (StringUtils.hasText(task.getSampleKey())) {
            taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        }
        if (taskSpecimen == null) {
            taskSpecimen = new ArrayList<>();
        }

        for (FTaskSpecimen specimen : taskSpecimen) {
            if (dto.getDelSampleIds().contains(specimen.getSpecimenKey())) {
                specimen.setDeleteType(deleteType);
                LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(FMeasureData::getTaskId, task.getId()).orderByDesc(FMeasureData::getSerialNumber).last("limit 1");
                FMeasureData one = fMeasureDataService.getOne(queryWrapper);
                if (one != null) {
                    one.setIsDelete(deleteType);
                    fMeasureDataService.updateById(one);
                }
            }
        }

        FRationTaskHeadSnapShot taskHeadSnapShot = new FRationTaskHeadSnapShot();
        if (StringUtils.hasText(task.getTableInfo())) {
            taskHeadSnapShot = JSON.parseObject(task.getTableInfo(), FRationTaskHeadSnapShot.class);
        }

        /*
        处理表头
         */
        if (dto.getStandHeads() != null && !dto.getStandHeads().isEmpty()) {
            FTaskHeadSnapshot standHead = new FTaskHeadSnapshot();
            if (taskHeadSnapShot.getStandHead() != null) {
                BeanUtils.copyProperties(taskHeadSnapShot.getStandHead(), standHead);
            }
            standHead.setTaskId(task.getId());
            standHead.setSampleHeads(dto.getStandHeads());
            taskHeadSnapShot.setStandHead(standHead);
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
            } else {
                FTaskSpecimen specimen = new FTaskSpecimen();
                specimen.setDeleteType(createType);
                specimen.setIsTested(noTested);
                specimen.setSampleType(newSample.getSampleType());
                specimen.setSampleKind(stand);
                log.info("新增的标准样品：{}", newSample);
                fMeasureSampleService.save(newSample);
                specimen.setSpecimenKey(newSample.getId());
                taskSpecimen.add(specimen);
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
                    String s = map.get(head.getIdentity());
                    newColData.put(head.getIdentity(), s);
                }

                newSample.setHeads(JSON.toJSONString(dto.getUnknowHeads()));
                newSample.setNewColData(JSON.toJSONString(newColData));
            }

            if (StringUtils.hasText(newSample.getId())) {
                fMeasureSampleService.updateById(newSample);
            } else {
                FTaskSpecimen specimen = new FTaskSpecimen();
                specimen.setDeleteType(createType);
                specimen.setIsTested(noTested);
                specimen.setSampleKind(unknown);
                specimen.setSampleType(newSample.getSampleType());
                log.info("新增的未知样品：{}", newSample);
                fMeasureSampleService.save(newSample);
                specimen.setSpecimenKey(newSample.getId());
                taskSpecimen.add(specimen);
            }
        }

        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);

        return Result.ok(fMeasureTaskService.updateById(task));
    }

    @Override
    public Result<Boolean> deleteSample(String taskId, String sampleId) {

        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(sampleId)) {
            return Result.fail("请选择需要删除的任务和样品");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            return Result.fail("任务无样品");
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
                    fMeasureDataService.updateById(one);
                }
            }
        }
        task.setSampleKey(JSON.toJSONString(taskSpecimen));
        fMeasureTaskService.updateById(task);
        return Result.ok(true);
    }

    @Override
    public Result revokeDel(String taskId, String sampleId) {
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        String sampleKey = task.getSampleKey();

        FRationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), FRationTaskHeadSnapShot.class);

        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(sampleKey, FTaskSpecimen.class);
        for (FTaskSpecimen specimen : taskSpecimen) {
            if (specimen.getSpecimenKey().equals(sampleId)) {
                specimen.setDeleteType(createType);
                FMeasureSample newSample = fMeasureSampleService.getById(sampleId);
                if (specimen.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                    newSample.setHeads(String.valueOf(shot.getUnknownHead().getSampleHeads()));
                } else if (specimen.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                    newSample.setHeads(String.valueOf(shot.getStandHead().getSampleHeads()));
                }
                fMeasureSampleService.updateById(newSample);

                LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(FMeasureData::getTaskId, task.getId())
                        .orderByDesc(FMeasureData::getSerialNumber).last("limit 1");
                FMeasureData one = fMeasureDataService.getOne(queryWrapper);
                if (one != null) {
                    one.setIsDelete(createType);
                    fMeasureDataService.updateById(one);
                }
            }
        }
        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);

        return Result.ok(fMeasureTaskService.updateById(task));
    }

    @Override
    public Result<FRationMethodParam> queryMethodParam(String methodId) {
        if (!StringUtils.hasText(methodId)) {
            return Result.fail("方法ID不能为空");
        }

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }

        if (!StringUtils.hasText(method.getMeasurementParamInfo())) {
            return Result.fail("未查询到该方法参数");
        }
        FRationParameter rationParameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);

        FRationMethodParam param = new FRationMethodParam();
        BeanUtils.copyProperties(rationParameter, param);
        param.setMethodId(method.getId());
        param.setMethodName(method.getMethodName());
        param.setMeasurementType(method.getMeasurementType());
        return Result.ok(param);
    }

    //标准样品表头
    @Override
    public Result<List<FHeadVO>> getStandSampleTableHead(String taskId, String methodId, Boolean allDisplay) {

        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(methodId)) {
            return Result.fail("请选择方法或任务");
        }

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
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
        List<FHeadVO> result = dealHeads(new ArrayList<>(ans), parameter, SampleTypeEnum.kind_stand.getIdentity(), allDisplay, empty);
        return Result.ok(result);

    }

    private List<FHeadVO> dealHeads(List<FHeadVO> heads, FRationParameter parameter, String sampleKind, Boolean allDisplay, Boolean empty) {
        List<String> waveLength = parameter.getWaveLength();
        Map<String, FHeadVO> map = heads.stream().collect(Collectors.toMap(FHeadVO::getIdentity, head -> head));

        Map<String, List<FHeadVO>> headsMap = new HashMap<>();

        Map<String, FHeadVO> analyzeMap = new HashMap<>();

        for (FHeadVO head : heads) {
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
            String absName = s + WaveLengthEnum.abs.getName();
            String tPercentName = s + WaveLengthEnum.tPercent.getName();

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
        if (empty && map.containsKey(sumAbsIdentity)) {
            sumAbsHead = map.get(sumAbsIdentity);
        }
        allMeasureHeads.add(sumAbsHead);

        List<FHeadVO> ans = new ArrayList<>();
        List<FHeadVO> systemHeads = headsMap.get(TableHeadILocateEnums.system.getIdentity());
        if (systemHeads != null) ans.addAll(systemHeads);

        List<FHeadVO> sampleHeads = headsMap.get(TableHeadILocateEnums.sample.getIdentity());
        if (sampleHeads != null) ans.addAll(sampleHeads);

        ans.addAll(allMeasureHeads);

        List<FHeadVO> taskHeads = headsMap.get(TableHeadILocateEnums.data.getIdentity());
        if (taskHeads != null)ans.addAll(taskHeads);

        if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
            String fitConcentrationName = SampleTableEnum.FIT_CONCENTRATION.getName();
            String fitConcentrationIdentity = SampleTableEnum.FIT_CONCENTRATION.getIdentity();

            FHeadVO fitHead = new FHeadVO(fitConcentrationName, SampleTableEnum.FIT_CONCENTRATION.getIdentity(), TableHeadILocateEnums.analyze.getIdentity(), fitConcentrationName);
            if (analyzeMap.containsKey(fitConcentrationIdentity)) {
                fitHead = analyzeMap.get(fitConcentrationIdentity);
            }
            ans.add(fitHead);

            List<FPreCountResultParam> resultParams = parameter.getResultParams();
            List<FHeadVO> headAnalyze = new ArrayList<>();
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
            FHeadVO remark = new FHeadVO(SampleTableEnum.REMARK.getName(), SampleTableEnum.REMARK.getIdentity(), TableHeadILocateEnums.data.getIdentity(), SampleTableEnum.REMARK.getName());
            other.add(samplingTimeHead);
            other.add(remark);
        }

        ans.addAll(other);

        return ans;
    }


    //未知样品表头
    @Override
    public Result<List<FHeadVO>> getUnknownSampleTableHead(String taskId, String methodId, Boolean allDisplay) {

        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(methodId)) {
            return Result.fail("请选择任务或方法");
        }

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
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
        List<FHeadVO> result = dealHeads(new ArrayList<>(ans), parameter, SampleTypeEnum.kind_unknown.getIdentity(), allDisplay, empty);
        return Result.ok(result);
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
            fMeasureAnalysisDataService.save(analysisData);
        } else {
            if (!analysisData.getMeasureDataId().equals(measureDataId)) {
                analysisData.setMeasureDataId(measureDataId);
            }
            analysisData.setAnalysisResult(JSON.toJSONString(countData.getCountData()));
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
                fMeasureAnalysisDataService.updateById(analysisData);
            } else {
                analysisData = new FMeasureAnalysisData();
                analysisData.setTaskId(emptyData.getTaskId());
                analysisData.setSampleKey(emptyData.getSampleId());
                analysisData.setMeasureDataId(emptyData.getMeasureDataId());
                analysisData.setDataVersion(0);
                analysisData.setAnalysisResult(JSON.toJSONString(emptyData.getCountData()));
                analysisData.setIsDelete(createType);
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
                return R.fail("未找到对应波长数据");
            }
            if (i < lengthParams.size()) {
                value = value * Double.parseDouble(lengthParams.get(i));
            } else {
                value = value * 1;
            }
            ans += value;
        }
        FRationCountDataInfo countDataInfo = new FRationCountDataInfo();
        countDataInfo.setColValue(DataCountUtil.formatValue(digit, ans));
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
                return R.fail("未找到对应波长数据");
            }
            if (i < lengthParams.size()) {
                value = value * Double.parseDouble(lengthParams.get(i));
            } else {
                value = value * 1;
            }
            ans += value;
        }
        FRationCountDataInfo countDataInfo = new FRationCountDataInfo();
        countDataInfo.setColValue(ans);
        countDataInfo.setIdentity(SampleTableEnum.Abs.getIdentity());
        countDataInfo.setHeadName(SampleTableEnum.Abs.getName());

        return R.ok(countDataInfo);
    }

    @Override
    public Result<List<Map<String, Object>>> queryStandSampleData(String taskId, Boolean allDisplay) {
        if (!StringUtils.hasText(taskId) || allDisplay == null) {
            return Result.fail("未选择任务");
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
            map.put(SampleTableEnum.SAMPLE_LOCATION.getIdentity(), "样品池"+ sample.getSampleLocation());
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
            return Result.fail("未选择任务");
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            log.info("未查询到任务信息");
            return Result.fail("任务不存在");
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

                    attachment.setDataParam(JSON.toJSONString(queryDataDTO));
                    fMeasureTaskAttachmentService.saveOrUpdate(attachment);
                }
            }
        }

        if (recount) {
            changeStandSampleDataDigit(taskId, digit);
            changeUnknowSampleDigit(taskId, digit);
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

                standSampleData.add(singleSampleData);
            } else if (specimen.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {

                FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());

                Map<String, Object> sampleData = getSingleSampleData(task, sample, repeat, option, digit);

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

        if (option != null && !option.isEmpty()) {
            for (String s : option) {
                Map<String, String> children = new HashMap<>();
                if (DataIdentityEnums.avg.getIdentity().equals(s)) {
                    children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.avg.getName());

                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), wlData.getAbsValue());
                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(),wlData.getTPercentValue());
                    ans.putIfAbsent(sample.getSampleNumber() + "-" + DataIdentityEnums.avg.getIdentity(), children);
                }
                if (DataIdentityEnums.sd.getIdentity().equals(s)) {
                    children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.sd.getName());

                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), absData.getSd());
                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tPercentData.getSd());
                    ans.putIfAbsent(sample.getSampleNumber() + "-" + DataIdentityEnums.sd.getIdentity(), children);
                }
                if (DataIdentityEnums.rsd.getIdentity().equals(s)) {
                    children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getName());
                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), absData.getRsd());
                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tPercentData.getRsd());
                    ans.putIfAbsent(sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getIdentity(), children);
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
        map.put(SampleTableEnum.SAMPLE_LOCATION.getIdentity(), "样品池"+sample.getSampleLocation());

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
                        map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), DataCountUtil.formatValueWithBigDecimal(digit, dataInfo.getAbsValue()));
                        map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), DataCountUtil.formatValueWithBigDecimal(digit, dataInfo.getTPercentValue()));
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
                        map.put(countData.getIdentity(), String.valueOf(countData.getColValue()));
                    }
                }

            }
        } else {
            //重复模式，获取数据方式待定----暂时先以不重复做回显
            if (data != null) {
                map.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), data.getSamplingTime());
                map.put(SampleTableEnum.REMARK.getIdentity(), data.getRemark());
                if (StringUtils.hasText(data.getOriginalCalculationData())) {
                    Map<String, Map<String, String>> childrenList = new HashMap<>();
                    List<Map<String, String>> middle = new ArrayList<>();
                    List<FRationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), FRationWavelengthDataInfo.class);
                    for (FRationWavelengthDataInfo dataInfo : list) {
                        Map<String, String> children = new HashMap<>();

                        if (dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), DataCountUtil.formatValueWithBigDecimal(digit,dataInfo.getAbsValue()));
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), DataCountUtil.formatValueWithBigDecimal(digit, dataInfo.getTPercentValue()));

                            Map<String, Map<String, String>> ch = extracted(option, dataInfo, sample);
                            if (!ch.isEmpty()) {
                                for (Map.Entry<String, Map<String, String>> entry : ch.entrySet()) {
                                    String key = entry.getKey();
                                    Map<String, String> value = entry.getValue();

                                    if (childrenList.containsKey(key)) {
                                        // 如果键已存在，将新值中的键值对合并到现有Map中
                                        Map<String, String> existingMap = childrenList.get(key);
                                        // 将value中的所有键值对放入existingMap中（相同键会覆盖，新键会添加）
                                        existingMap.putAll(value);
                                    } else {
                                        // 如果键不存在，直接添加（创建副本以避免引用问题）
                                        childrenList.put(key, new HashMap<>(value));
                                    }
                                }
                            }

                        } else {

                            children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), DataCountUtil.formatValueWithBigDecimal(digit,dataInfo.getAbsValue()));
                            children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), DataCountUtil.formatValueWithBigDecimal(digit,dataInfo.getTPercentValue()));
                            String[] split = dataInfo.getIdentity().split("-");
                            children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + split[split.length - 1]);
                            childrenList.computeIfAbsent(dataInfo.getIdentity(), k -> new HashMap<>()).putAll(children);
                        }
                    }
                    childrenList.forEach((key, value) -> middle.add(value));
                    map.put("children", middle);
                }

                LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                        .eq(FMeasureAnalysisData::getSampleKey, sampleId)
                        .eq(FMeasureAnalysisData::getMeasureDataId, data.getId())
                        .orderByDesc(FMeasureAnalysisData::getDataVersion).last("limit 1");

                FMeasureAnalysisData one = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                if (one != null && one.getAnalysisResult() != null) {
                    List<FRationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), FRationCountDataInfo.class);
                    for (FRationCountDataInfo countData : countDataList) {
                        map.put(countData.getIdentity(), String.valueOf(countData.getColValue()));
                    }
                }

            }
        }
        return map;
    }


    @Override
    public Result<List<Map<String, Object>>> queryUnknowSampleData(String taskId, Boolean allDisplay) {
        if (!StringUtils.hasText(taskId) || allDisplay == null) {
            return Result.fail("未选择任务");
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
        if (empty) {
            for (FRationCountDataInfo dataInfo : countData) {
                if (dataInfo.getIdentity().equals(SampleTableEnum.SUM_Abs.getIdentity())) {
                    data.add(dataInfo.getColValue());

                }
            }
        } else {
            for (FRationCountDataInfo dataInfo : countData) {
                if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                    data.add(dataInfo.getColValue());
                }
            }
        }

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);

        List<String> zeroMethod = parameter.getZeroMethod();

        param.setForce_zero(0L);

        for (String zero: zeroMethod) {
            if (zero.equals(RationCurveParamEnums.insert_zero.getIdentity())) {
                data.add(0.00);
                inputParam.add("0.0");
            }

            if (zero.equals(RationCurveParamEnums.force_zero.getIdentity())) {
                param.setForce_zero(1L);
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
            return R.fail("曲线拟合失败");
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
            return Result.fail("未选择任务");
        }
        if (!StringUtils.hasText(methodId)) {
            log.info("缺少方法id");
            return Result.fail("未选择方法");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            log.info("任务不存在");
            return Result.fail("任务不存在");
        }

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);

        FRationFitCurveVO resultVo = new FRationFitCurveVO();

        if (StringUtils.hasText(task.getFittingParam())) {
            FittingReceiveInfo fittingParam = JSONObject.parseObject(task.getFittingParam(), FittingReceiveInfo.class);
            resultVo = curveData(fittingParam);
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

        double k0 = info.getK0() == null ? 0 : (double) Math.round(info.getK0() * 1000) / 1000;
        double k1 = info.getK1() == null ? 0 : (double) Math.round(info.getK1() * 1000) / 1000;
        double k2 = info.getK2() == null ? 0 : (double) Math.round(info.getK2() * 1000) / 1000;
        double k3 = info.getK3() == null ? 0 : (double) Math.round(info.getK3() * 1000) / 1000;
        double k4 = info.getK4() == null ? 0 : (double) Math.round(info.getK4() * 1000) / 1000;

        double step = 0.1;
        //采用3位小数来计算
        double minX = (double) Math.round(info.getMinX() * 1000) / 1000;
        double maxX = (double) Math.round(info.getMaxX() * 1000) / 1000;

        switch (info.getDegree()) {
            case 1:
                for (double i = minX - 1; i < maxX + 1; i += step) {
                    double y = k0 + k1 * i;
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("x", new BigDecimal(i).setScale(3, RoundingMode.HALF_UP).doubleValue());
                    jsonObject.put("y", new BigDecimal(y).setScale(3, RoundingMode.HALF_UP).doubleValue());
                    dataList.add(jsonObject);
                }
                break;
            case 2:
                for (double i = minX - 1; i < maxX + 1; i += step) {
                    double y = k0 + k1 * i + k2 * i * i;
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("x", new BigDecimal(i).setScale(3, RoundingMode.HALF_UP).doubleValue());
                    jsonObject.put("y", new BigDecimal(y).setScale(3, RoundingMode.HALF_UP).doubleValue());
                    dataList.add(jsonObject);
                }
                break;
            case 3:
                for (double i = minX - 1; i < maxX + 1; i += step) {
                    double y = k0 + k1 * i + k2 * i * i + k3 * i * i * i;
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("x", new BigDecimal(i).setScale(3, RoundingMode.HALF_UP).doubleValue());
                    jsonObject.put("y", new BigDecimal(y).setScale(3, RoundingMode.HALF_UP).doubleValue());
                    dataList.add(jsonObject);
                }
                break;

            case 4:
                for (double i = minX - 1; i < maxX + 1; i += step) {
                    double y = k0 + k1 * i + k2 * i * i + k3 * i * i * i + k4 * i * i * i * i;
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("x", new BigDecimal(i).setScale(3, RoundingMode.HALF_UP).doubleValue());
                    jsonObject.put("y", new BigDecimal(y).setScale(3, RoundingMode.HALF_UP).doubleValue());
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
        vo.setR(info.getR());
        vo.setR2(info.getR2());
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

        if (equationType.equals(RationCurveParamEnums.typeAbs.getIdentity())) {
            s = restUtil.concentrationCalculate(info.getCoefficients(), value, 1);
        } else if (equationType.equals(RationCurveParamEnums.typeCurve.getIdentity())) {
            s = restUtil.concentrationCalculate(info.getCoefficients(), value, 0);
        }
        if (StringUtils.hasText(s)) {
            JSONObject jsonObject = JSON.parseObject(s);

            String mid = jsonObject.get("value").toString();
            ans = DataCountUtil.formatValueWithBigDecimal(digit, mid);
        }
        return ans;
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
    public R<List<FRationCountDataInfo>> formula(String analysisDataId, List<FRationWavelengthDataInfo> vo, FMeasureSample sample, FMeasureTask task, FRationParameter parameter, Integer digit) {
        if (!StringUtils.hasText(task.getTableInfo())) {
            return R.fail("缺少分析参数，无法计算分析");
        }

        FRationTaskHeadSnapShot rationTaskHeadSnapShot = JSON.parseObject(task.getTableInfo(), FRationTaskHeadSnapShot.class);
        if (rationTaskHeadSnapShot == null || rationTaskHeadSnapShot.getUnknownHead() == null) {
            return R.fail("缺少分析参数，无法计算分析");
        }
        FTaskHeadSnapshot snapshot = rationTaskHeadSnapShot.getUnknownHead();

        if (snapshot.getMapRelation() == null || snapshot.getMapRelation().isEmpty()) {
            return R.fail("缺少映射关系，无法计算分析");
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
                return R.fail("缺少参数映射：" + inputParam.getParamName());
            }
            identity.append(mapTableHead.get().getParamIdentity()).append(",");
            String s = allData.getString(mapTableHead.get().getHeadIdentity());
            if (!StringUtils.hasText(s)) {
                return R.fail("缺少数据！");
            }
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
                return R.fail("公式计算出错！" + resultJson.get("message"));
            }
            countDataInfo.setColValue((Double) resultJson.get("result"));
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
            return R.fail("没有样品");
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

        //参与曲线拟合的数据
        List<FRationCountDataInfo> fitCurveData = new ArrayList<>();
        //参与曲线拟合的参数
        List<String> fitCurveParams = new ArrayList<>();

        empty = taskSpecimen.stream()
                .anyMatch(specimen ->
                        specimen.getSpecimenKey().equals(dealSampleKind)
                                && specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())
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
                        return R.fail("数据处理出错且没有找到旧数据，处理失败");
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
                        return R.fail("数据处理出错且没有找到旧数据，处理失败");
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
                yAbs.setColValue(0.0);

                List<FRationCountDataInfo> currentList = new ArrayList<>();

                if (lastIsEmpty) {
                    emptyList.add(ansData);

                    double sub = 0.0;
                    //计算平均值
                    for (FRationCountDataInfo dataInfo : emptyList) {
                        sub += dataInfo.getColValue();
                    }
                    minuend = DataCountUtil.formatValue(digit, sub / emptyList.size());
                    //添加到空白列表
                    currentList.add(ansData);
                    yAbs.setColValue(ansData.getColValue() - minuend);
                    currentList.add(yAbs);
                    dealData.setCountData(currentList);
                    emptyDataList.add(dealData);

                    for (FRationSaveCountDataInfo emptyData : emptyDataList) {
                        List<FRationCountDataInfo> emList = emptyData.getCountData();
                        List<FRationCountDataInfo> newList = new ArrayList<>();

                        for (FRationCountDataInfo dataInfo : emList) {
                            if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                                newList.add(dataInfo);
                                yAbs.setColValue(dataInfo.getColValue() - minuend);
                                newList.add(yAbs);
                            }
                        }
                        emptyData.setCountData(newList);
                    }
                } else {
                    emptyList.clear();
                    emptyDataList.clear();
                    emptyList.add(ansData);
                    minuend = ansData.getColValue();
                    yAbs.setColValue(ansData.getColValue() - minuend);
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
                    sumAbsData.setColValue(ansData.getColValue() - minuend);
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
            return R.fail("没有样品");
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
                                && specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())
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
                yAbs.setColValue(0.0);

                List<FRationCountDataInfo> currentList = new ArrayList<>();

                if (lastIsEmpty) {
                    emptyList.add(ansData);

                    double sub = 0.0;
                    //计算平均值
                    for (FRationCountDataInfo dataInfo : emptyList) {
                        sub += dataInfo.getColValue();
                    }
                    minuend = DataCountUtil.formatValue(digit, sub / emptyList.size());
                    //添加到空白列表
                    currentList.add(ansData);
                    yAbs.setColValue(ansData.getColValue() - minuend);
                    currentList.add(yAbs);
                    dealData.setCountData(currentList);
                    emptyDataList.add(dealData);

                    for (FRationSaveCountDataInfo emptyData : emptyDataList) {
                        List<FRationCountDataInfo> emList = emptyData.getCountData();
                        List<FRationCountDataInfo> newList = new ArrayList<>();

                        for (FRationCountDataInfo dataInfo : emList) {
                            if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                                newList.add(dataInfo);
                                yAbs.setColValue(dataInfo.getColValue() - minuend);
                                newList.add(yAbs);
                            }
                        }
                        emptyData.setCountData(newList);
                    }
                } else {
                    emptyList.clear();
                    emptyDataList.clear();
                    emptyList.add(ansData);
                    minuend = ansData.getColValue();
                    yAbs.setColValue(ansData.getColValue() - minuend);
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
                    sumAbsData.setColValue(ansData.getColValue() - minuend);
                    toCountData = sumAbsData.getColValue();
                    currentList.add(sumAbsData);
                } else {
                    toCountData = ansData.getColValue();
                }
                currentList.add(ansData);
                /*
                获取拟合的值
                 */
                FittingReceiveInfo fitInfo = JSON.parseObject(task.getFittingParam(), FittingReceiveInfo.class);

                String ans = valueCalculate(fitInfo, toCountData, parameter.getEquationType(), digit);
                FRationCountDataInfo valueData = new FRationCountDataInfo();
                valueData.setHeadName(SampleTableEnum.FIT_CONCENTRATION.getName());
                valueData.setIdentity(SampleTableEnum.FIT_CONCENTRATION.getIdentity());
                valueData.setColValue(ans==null? null : Double.valueOf(ans));
                currentList.add(valueData);

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
            return Result.fail("未连接仪器");
        }

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
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
            boolean save = false;
            for (int i = 0; i < 3; i++) {
                if (fMeasureTaskService.save(newTask)) {
                    save = true;
                    break;
                }
            }
            if (!save) {
                return Result.fail("系统出错");
            }
            taskId = newTask.getId();
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);
        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            return Result.fail("缺少波长");
        }
        List<String> waveLength = parameter.getWaveLength();
        // 通过序列号获取ip
        String ip = deviceInfoService.getIp(deviceSerialNumber);
        List<ChangeParam> list = new ArrayList<>();
        for (String s : waveLength) {

            double v = Double.parseDouble(s);
            Command command = new Command(SetParamEnum.SetWl.getComId(), new Object[]{v});
            devMessageUtil.wavelengthLocate(command, ip);

            command = new Command(SetParamEnum.Zero.getComId(), new Object[]{});
            R<ChangeParam> zero = devMessageUtil.zero(command, ip);
            if (!zero.getSuccess()) {
                return Result.fail(zero.getMessage());
            }
            ChangeParam changeParam = zero.getData();
            changeParam.setWl(s);
            list.add(changeParam);
        }
        task.setZeroParam(JSON.toJSONString(list));

        boolean update = fMeasureTaskService.updateById(task);
        return update ? Result.ok() : Result.fail();
    }

    @Override
    public Result zero2(String deviceSerialNumber, String methodId, String taskId, String updateBy) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            return Result.fail("未连接仪器");
        }

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
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
            boolean save = false;
            for (int i = 0; i < 3; i++) {
                if (fMeasureTaskService.save(newTask)) {
                    save = true;
                    break;
                }
            }
            if (!save) {
                return Result.fail("系统出错");
            }
            taskId = newTask.getId();
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);
        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            return Result.fail("缺少波长");
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
                R<ChangeParam> zero = devMessageUtil.zero(command, ip);
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
                R<String> stringR = devMessageUtil.spectrumScanSet(commands,ip);
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
            return R.fail("未选择方法");
        }
        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return R.fail("方法不存在");
        }
        FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);
        List<FInputParam> inputParams = parameter.getSampleInputParams();

        return R.ok(inputParams);
    }

    @Override
    public R<List<FInputParamMapTableHead>> queryInputParamMapTableHead(String taskId, String sampleKind) {

        if (!StringUtils.hasText(taskId)) {
            return R.fail("未选择任务");
        }
        if (!StringUtils.hasText(sampleKind)) {
            return R.fail("未选择样品种类");
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return R.fail("任务不存在");
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
            return R.fail("未选择任务");
        }
        if (!StringUtils.hasText(snapshotDTO.getSampleKind())) {
            return R.fail("未选择样品种类");
        }

        FMeasureTask task = fMeasureTaskService.getById(snapshotDTO.getTaskId());
        if (task == null) {
            return R.fail("任务不存在");
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

        FRationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), FRationTaskHeadSnapShot.class);
        if (snapshotDTO.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
            shot.setStandHead(taskHeadSnapshot);
        } else if (snapshotDTO.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {
            shot.setUnknownHead(taskHeadSnapshot);
        } else {
            log.info("样品种类有误");
            return R.fail("样品种类有误");
        }

        task.setTableInfo(JSON.toJSONString(shot));

        return fMeasureTaskService.updateById(task) ? R.ok(true) : R.fail("保存失败");
    }

    /**
     * 多格式文件导出（支持word/txt/xlsx）
     * @param taskId 任务ID
     * @param allDisplay 是否显示全部数据
     * @param fileType 导出格式（word/txt/xlsx）
     * @return 生成的文件对象
     */
    public R<File> downloadFile(String taskId, Boolean allDisplay, String fileType) {
        log.info("开始导出11111111，taskId: {}, fileType: {}", taskId, fileType);
        // 1. 基础参数校验（复用原逻辑）
        if (!StringUtils.hasText(taskId)) {
            return R.fail("缺少任务id");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            // log.error("导出失败：任务不存在，taskId: {}", taskId);
            return R.fail("任务不存在");
        }
        // 处理fileType默认值和合法性
        if (!StringUtils.hasText(fileType)) {
            fileType = "word"; // 默认导出word
        } else {
            fileType = fileType.toLowerCase();
        }
        if (!"word".equals(fileType) && !"txt".equals(fileType) && !"xlsx".equals(fileType)) {
            return R.fail("不支持的导出格式，仅支持word/txt/xlsx");
        }
        // 处理allDisplay默认值
        if (allDisplay == null) {
            allDisplay = true;
        }

        // 2. 准备表头和数据（完全复用原downloadWord逻辑）
        String methodId = task.getMethodKey();
        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return R.fail("测量方法不存在");
        }

        // 构建表头（与原逻辑一致）
        List<FHeadVO> heads = new ArrayList<>();
        // 添加动态表头
        List<FHeadVO> vos = getStandSampleTableHead(taskId, methodId, allDisplay).getData();
        heads.addAll(vos);
        // // 最前面加序号列（所有格式统一需要）
        heads.add(0, new FHeadVO("序号", "serialNumber"));

        // 查询数据（复用原逻辑）
        FQueryDataDTO queryDataDTO = new FQueryDataDTO();
        queryDataDTO.setOption(new ArrayList<>());
        queryDataDTO.setTaskId(taskId);
        queryDataDTO.setAllDisplay(allDisplay);
        Result<FRationSampleDataVO> sampleDataResult = querySampleData(queryDataDTO);
        if (!sampleDataResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode())) { // 校验查询是否成功
            return R.fail("查询样品数据失败：" + sampleDataResult.getMessage());
        }
        FRationSampleDataVO dataVO = sampleDataResult.getData();
        if (dataVO == null) { // 校验VO对象是否为空
            return R.fail("样品数据为空");
        }
        // 2. 仅提取标准样品数据（standSampleData）
        List<Map<String, Object>> data = dataVO.getStandSampleData();
        // 3. 校验标准样品数据是否为空
        if (data == null || data.isEmpty()) {
            return R.fail("没有可导出的标准样品数据");
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
            // 关键：打印完整异常堆栈（包含具体错误代码行）
            log.error("文件生成失败！taskId={}, fileType={}, allDisplay={}", taskId, fileType, allDisplay, e);
            if (file.exists()) {
                file.delete();
            }
            // 返回具体错误信息（而非模糊的“文件生成失败”）
            return R.fail("文件生成失败：" + e.getMessage());
        }
    }

    // ---------------------- 原有downloadWord方法（可保留或删除，建议用上面的downloadFile替代） ----------------------
    public R<File> downloadWord(String taskId, Boolean allDisplay) {
        // 直接调用多格式方法，指定格式为word
        return downloadFile(taskId, allDisplay, "word");
    }

    /**
     * 生成Word文件（修复原逻辑的表格列错位问题）
     */
    private void generateWordFile(File file, FMeasureTask task, FMeasureMethod method,
                                 List<FHeadVO> heads, List<Map<String, Object>> data, int vosSize) throws Exception {
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

        // 填充表头（修复原逻辑的表头赋值问题 + 增加日志排查）
        XWPFTableRow headerRow = table.getRow(0);
        // 先打印headerRow是否为空，排除行对象本身的问题
        if (headerRow == null) {
            log.error("表头行对象headerRow为null！无法填充表头");
            throw new RuntimeException("表头行对象为空，无法继续生成Word文件");
        }
        // log.info("表头行headerRow创建成功，开始填充表头（总列数：{}）", columnCount);

        for (int i = 0; i < columnCount; i++) {
            // 1. 打印当前循环索引，方便定位具体哪一列出问题
            // log.info("正在处理表头第 {} 列（从0开始计数）", i);

            // 2. 验证单元格cell是否存在
            XWPFTableCell cell = headerRow.getCell(i);
            if (cell == null) {
                // log.error("表头第 {} 列的单元格cell为null！", i);
                // 可选：若单元格为null，主动创建一个（避免后续空指针）
                cell = headerRow.createCell();
                // log.warn("已为表头第 {} 列主动创建空单元格", i);
            }

            // 3. 验证heads列表在当前索引是否有值
            if (i >= heads.size()) {
                // log.error("表头索引 {} 超出heads列表长度！heads列表实际长度：{}", i, heads.size());
                cell.setText("未定义列"); // 给默认值，避免空指针
                continue;
            }
            FHeadVO head = heads.get(i);
            if (head == null) {
                // log.error("表头第 {} 列的HeadVO对象为null！", i);
                cell.setText("空表头"); // 给默认值
                continue;
            }

            // 4. 验证表头名称name是否存在（字段缺失检查）
            String headName = head.getName();
            if (headName == null || headName.trim().isEmpty()) {
                // log.warn("表头第 {} 列的名称name为空（HeadVO.getIdentity()：{}）", i, head.getIdentity());
                headName = "未命名列（" + head.getIdentity() + "）"; // 用identity补全，方便排查
            }

            // 最终填充单元格
            cell.setText(headName);
            // log.info("表头第 {} 列填充完成：cell内容 = {}", i, headName);
        }

        // 填充数据行（修复原逻辑的索引越界问题）
        int rowIndex = 0;
        for (Map<String, Object> map : data) {
            XWPFTableRow row = table.createRow();
            rowIndex++;
            // 第0列：序号
            XWPFTableCell indexCell = row.getCell(0);
            if (indexCell != null) {
                indexCell.setText(String.valueOf(rowIndex));
            }

            // 填充其他列（从1开始，与表头对应）
            for (int i = 1; i < columnCount; i++) {
                // 1. 获取当前列对应的表头信息
                FHeadVO head = i < heads.size() ? heads.get(i) : null;
                if (head == null) {
                    continue; // 表头不存在则跳过
                }

                // 2. 获取原始数据（不做默认值处理，保留null）
                String identity = head.getIdentity();
                Object originalValue = map != null ? map.get(identity) : null;

                // 3. 原始值直接转换为字符串（不做任何额外处理）
                String cellData;
                if (originalValue == null) {
                    cellData = ""; // null值保留空字符串
                } else {
                    cellData = originalValue.toString(); // 原值直接转换
                }

                // 4. 仅处理单元格为空的情况（避免空指针）
                XWPFTableCell cell = row.getCell(i);
                if (cell != null) {
                    cell.setText(cellData);
                }
            }
        }

        // 表格样式（复用原逻辑）
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                XWPFParagraph paragraph1 = cell.getParagraphArray(0);
                paragraph1.setAlignment(ParagraphAlignment.CENTER);
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }

        // 写入文件（修复原逻辑的流关闭问题）
        try (FileOutputStream out = new FileOutputStream(file)) {
            doc.write(out);
        } finally {
            doc.close(); // 关闭文档对象，释放资源
        }
    }

    /**
     * 生成TXT文件（UTF-8编码，制表符分隔）
     */
    private void generateTxtFile(File file, List<FHeadVO> heads, List<Map<String, Object>> data) throws Exception {
        // 前置校验：避免核心参数为null导致后续流处理报错
        if (heads == null) {
            throw new IllegalArgumentException("表头列表heads不能为null");
        }
        if (data == null) {
            throw new IllegalArgumentException("数据列表data不能为null");
        }
        if (file == null) {
            throw new IllegalArgumentException("输出文件File不能为null");
        }

        // 使用BufferedWriter保证效率，UTF-8避免中文乱码（try-with-resources自动关流）
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            // 1. 写入表头（制表符分隔）—— 重点防护heads中的null元素
            String headerLine = heads.stream()
                    // 过滤并处理heads中的null元素
                    .map(head -> {
                        if (head == null) {
                            // log.warn("表头列表中存在null元素，使用默认表头名称");
                            return "未命名列";
                        }
                        // 处理表头名称为null/空的情况
                        String headName = head.getName();
                        return (headName == null || headName.trim().isEmpty()) ? "未命名列" : headName;
                    })
                    .collect(Collectors.joining("\t"));
            writer.write(headerLine);
            writer.newLine(); // 换行

            // 2. 写入数据（与表头顺序一致）—— 防护null的Map、null的字段值、枚举处理null
            for (int rowIdx = 0; rowIdx < data.size(); rowIdx++) {
                Map<String, Object> map = data.get(rowIdx);
                // 防护：当前数据行的Map为null
                if (map == null) {
                    // log.warn("数据列表第{}行的Map为null，跳过该行数据", rowIdx + 1);
                    continue;
                }

                List<String> rowData = new ArrayList<>();
                for (FHeadVO head : heads) {
                    // 防护：表头为null（避免后续head.getIdentity()报错）
                    String identity = head != null ? head.getIdentity() : "unknown";
                    // 从Map取值：用getOrDefault避免key不存在，同时防护value为null
                    Object value = map.getOrDefault(identity, "");
                    String cellData = value == null ? "" : String.valueOf(value);

                    // 特殊字段格式化（与Word/Excel保持一致，增加null防护）
                    if (head != null && "sampleType".equals(identity)) {
                        // 防护：cellData为null/空/字符串"null"
                        if (cellData == null || cellData.trim().isEmpty() || "null".equalsIgnoreCase(cellData)) {
                            cellData = "-";
                        } else {
                            // 防护：枚举方法返回null时用"-"替代
                            String typeName = SampleTypeEnum.getName(cellData);
                            cellData = typeName == null ? "-" : typeName;
                        }
                    }
                    if (head != null && "sampleLocation".equals(identity)) {
                        // 防护：避免拼接"样品null"或"样品"（空值）
                        if (cellData == null || cellData.trim().isEmpty() || "null".equalsIgnoreCase(cellData)) {
                            cellData = "-";
                        } else {
                            cellData = "样品" + cellData;
                        }
                    }

                    // 最终统一空值处理：将空字符串/"null"转为"-"
                    if (cellData == null || cellData.trim().isEmpty() || "null".equalsIgnoreCase(cellData)) {
                        cellData = "-";
                    }

                    rowData.add(cellData);
                }

                // 写入当前行数据（制表符分隔）
                writer.write(String.join("\t", rowData));
                writer.newLine();
                // log.debug("TXT文件第{}行数据写入完成", rowIdx + 1);
            }

            // log.info("TXT文件生成成功，路径：{}", file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            // log.error("TXT文件路径不存在或无法写入：{}", file.getAbsolutePath(), e);
            throw e;
        } catch (IOException e) {
            // log.error("TXT文件写入IO异常", e);
            throw e;
        } catch (Exception e) {
            // log.error("TXT文件生成未知异常", e);
            throw e;
        }
    }

    /**
     * 生成 XLSX 文件（修复空指针异常）
     */
    private void generateExcelFile(File file, List<FHeadVO> heads, List<Map<String, Object>> data) throws Exception {
        // 1. 校验核心参数是否为null（避免后续调用方法时报错）
        if (heads == null) {
            throw new IllegalArgumentException("表头列表heads不能为null");
        }
        if (data == null) {
            throw new IllegalArgumentException("数据列表data不能为null");
        }

        // 2. 转换表头（处理空表头和null元素）
        List<List<String>> excelHeads = new ArrayList<>();
        for (int i = 0; i < heads.size(); i++) {
            FHeadVO head = heads.get(i);
            // 防护：当前表头为null
            if (head == null) {
                // log.warn("表头列表第{}个元素为null，使用默认表头", i);
                excelHeads.add(Collections.singletonList("未命名列_" + i));
            continue;
        }
        // 防护：表头名称为null/空
        String headName = head.getName();
        if (headName == null || headName.trim().isEmpty()) {
            headName = "未命名列（" + head.getIdentity() + "）";
            // log.warn("表头第{}列名称为空，使用默认名称：{}", i, headName);
        }
        excelHeads.add(Collections.singletonList(headName));
    }

    // 3. 转换数据（处理空Map和null值）
    List<List<Object>> excelData = new ArrayList<>();
        for (int rowIdx = 0; rowIdx < data.size(); rowIdx++) {
            Map<String, Object> map = data.get(rowIdx);
            // 防护：当前数据行为null
            if (map == null) {
                // log.warn("数据列表第{}行的Map为null，跳过该行", rowIdx);
                continue;
            }

            List<Object> rowData = new ArrayList<>();
            for (int colIdx = 0; colIdx < heads.size(); colIdx++) {
                FHeadVO head = heads.get(colIdx);
                // 防护：表头为null时，直接填充默认值
                if (head == null) {
                    rowData.add("-");
                    continue;
                }

                // 提取字段值，防护null
                Object value = map.get(head.getIdentity());
                String cellData = "-"; // 默认空值

                if (value != null) {
                    cellData = String.valueOf(value);
                    // 特殊字段格式化（增加null防护）
                    if ("sampleType".equals(head.getIdentity())) {
                        // 防护：cellData为null/空
                        if (cellData == null || cellData.trim().isEmpty() || "null".equalsIgnoreCase(cellData)) {
                            cellData = "-";
                        } else {
                            // 防护：枚举方法返回null
                            String typeName = SampleTypeEnum.getName(cellData);
                            cellData = typeName == null ? "-" : typeName;
                        }
                    }
                    if ("sampleLocation".equals(head.getIdentity())) {
                        // 防护：避免拼接"样品null"
                        cellData = (cellData == null || "null".equalsIgnoreCase(cellData)) ? "-" : ("样品" + cellData);
                    }
                    // 清除特殊字符，防护空字符串
                    cellData = cellData.replace("\r", "").replace("\n", "").replace("\t", " ");
                    if (cellData.isEmpty() || "null".equalsIgnoreCase(cellData)) {
                        cellData = "-";
                    }
                }

                rowData.add(cellData);
            }
            excelData.add(rowData);
        }

        // 4. 写入Excel（确保流正确关闭，防护空列表）
        if (excelHeads.isEmpty()) {
            log.warn("表头为空，生成空Excel文件");
        }
        try (OutputStream out = new FileOutputStream(file)) {
            EasyExcel.write(out)
                    .head(excelHeads)
                    .sheet("测量数据")
                    .doWrite(excelData);
        } catch (Exception e) {
            log.error("EasyExcel写入文件失败", e);
            throw e; // 向上抛出，由外层处理
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
