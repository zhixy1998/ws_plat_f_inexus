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
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureMethodService;
import com.persee.workstation.uv_inexus.service.guide.parseReport.LaTeXConverter;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingReceiveInfo;
import com.persee.workstation.uv_inexus.vo.concise.common.*;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricReportDataVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import com.persee.workstation.uv_inexus.vo.concise.ration.FRationFitCurveVO;
import com.persee.workstation.uv_inexus.vo.concise.ration.FRationReportDataVO;
import com.persee.workstation.uv_inexus.vo.concise.ration.FRationSampleDataVO;
import com.persee.workstation.uv_inexus.vo.concise.ration.FRationTaskHeadSnapShot;
import com.persee.workstation.uv_inexus.vo.concise.spectrum.FSpectrumReportDataVO;
import com.persee.workstation.uv_inexus.vo.concise.timesweep.FTimeSweepReportDataVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.spectrum.SpectralReport;
import com.persee.workstation.uv_inexus.vo.measurementVo.spectrum.SpectralReportParametersVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.timeSweep.TimeSweepReport;
import com.persee.workstation.uv_inexus.vo.measurementVo.timeSweep.TimeSweepReportParametersVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum.QUANTITATIVE_MEASUREMENT;

@Service("ConciseCommonService")
public class ConciseCommonServiceImpl implements ConciseCommonService {

    @Autowired
    private SpectrumConciseService spectrumConciseService;
    @Autowired
    private TimeSweepConciseService timeSweepConciseService;
    @Autowired
    private FMeasureMethodService fMeasureMethodService;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private FMeasureTaskService fMeasureTaskService;
    @Autowired
    private FMeasureTaskAttachmentService fMeasureTaskAttachmentService;
    @Autowired
    private FMeasureDataService fMeasureDataService;
    @Autowired
    private FMeasureAnalysisDataService fMeasureAnalysisDataService;
    @Autowired
    private TMeasureMethodService tMeasureMethodService;
    @Autowired
    private FMeasureSampleService fMeasureSampleService;

    @Autowired
    private FMeasureAnalysisImageService fMeasureAnalysisImageService;

    @Autowired
    private PhotometricConciseService photometricService;

    @Autowired
    private RationConciseService rationService;

    @Autowired
    private FSamplePoolConfigService fSamplePoolConfigService;

    @Autowired
    DeviceManageService deviceManageService;

    @Autowired
    LaTeXConverter latexConverter;

    public DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 查询默认参数
     * @param measureType
     * @return
     */
    @Override
    public Result queryDefaultParameters(String measureType) {
        // 根据测量类型以及统一的"默认方法"的方法名称进行查询默认方法参数
        QueryWrapper<FMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_name", "默认方法");
        wrapper.eq("measurement_type", measureType);
        FMeasureMethod one = fMeasureMethodService.getOne(wrapper);
        if (one == null) {
            return Result.fail(measureType+"没有默认方法");
        }
        return Result.ok(one);

    }


    @Override
    public Result<FMethodListWithPageVO> searchMethodButton(FQueryMethodWithPageDto dto) {
        // 对传入的参数进行校验
        if (!StringUtils.hasText(dto.getUpdateBy())) {
            return Result.fail("用户不能为空");
        }
        if (!StringUtils.hasText(dto.getMeasureType())) {
            return Result.fail("方法类型不能为空");
        }

        if (dto.getPageNum() == null || dto.getPageNum() < 1) {
            dto.setPageNum(1);
        }

        if (dto.getPageSize() == null || dto.getPageSize() < 1) {
            dto.setPageSize(10);
        }
        // 设置查询条件
        QueryWrapper<TMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_creator", dto.getUpdateBy());
        wrapper.eq("measurement_type", dto.getMeasureType());
        if (StringUtils.hasText(dto.getMethodName())) {
            wrapper.like("method_name", dto.getMethodName());
        }
        // 分页查询
        int pageNum = dto.getPageNum();
        int pageSize = dto.getPageSize();
        Page<TMeasureMethod> page = tMeasureMethodService.page(new Page<>(pageNum, pageSize), wrapper);
        List<TMeasureMethod> records = page.getRecords();
        FMethodListWithPageVO ans = new FMethodListWithPageVO();
        ans.setTotal(page.getTotal());
        ans.setSize(page.getSize());
        ans.setCurrent(page.getCurrent());
        if (dto.getMeasureType().equals(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription())) {
            List<FMethodInfoVO> infoVOList = new ArrayList<>();
            for (TMeasureMethod record : records) {
                FMethodInfoVO info = new FMethodInfoVO();
                BeanUtils.copyProperties(record, info);

                PhotometricParameter parameter = JSON.parseObject(record.getMeasurementParamInfo(), PhotometricParameter.class);
                parameter.setLuminosityName(WaveLengthEnum.getName(parameter.getLuminosity()));
                parameter.setSamplePoolWayName(RepeatConfigEnum.getName(parameter.getSamplePoolWay()));

                info.setMethodParam(JSONObject.parseObject(JSONObject.toJSONString(parameter)));
                infoVOList.add(info);

            }
            ans.setRecords(infoVOList);

        } else if (dto.getMeasureType().equals(QUANTITATIVE_MEASUREMENT.getDescription())) {
            List<FMethodInfoVO> infoVOList = new ArrayList<>();
            for (TMeasureMethod record : records) {
                FMethodInfoVO info = new FMethodInfoVO();
                BeanUtils.copyProperties(record, info);
                RationParameter parameter = JSON.parseObject(record.getMeasurementParamInfo(), RationParameter.class);

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
                info.setMethodParam(JSONObject.parseObject(JSONObject.toJSONString(parameter)));

                infoVOList.add(info);
            }
            ans.setRecords(infoVOList);
        } else {
            return Result.fail("该类型暂未开发");
        }
        return Result.ok(ans);
    }


    /**
     * 查询保存状态
     * @param taskId
     * @return
     */
    @Override
    public Result querySaveState(String taskId) {
        if (taskId == null){
            return Result.fail("任务id不能为空");
        }
        SaveStateVo vo = new SaveStateVo();
        // 根据任务id查询任务
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null){
            return Result.fail("草稿不存在");
        }
        // 根据任务中的方法key查询方法
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        if (method == null){
            return Result.fail("方法不存在");
        }
        vo.setTaskName(task.getTaskName());
        vo.setMethodName(method.getMethodName());
        // 根据任务id查询测量数据和分析数据
        List<FMeasureData> measureDataList = fMeasureDataService.list(new QueryWrapper<FMeasureData>().eq("task_id", taskId).eq("is_save", "1"));
        List<FMeasureAnalysisData> analysisDataList = fMeasureAnalysisDataService.list(new QueryWrapper<FMeasureAnalysisData>().eq("task_id", taskId).eq("is_save", "1"));
        if (!measureDataList.isEmpty() || !analysisDataList.isEmpty()){
            vo.setSaveState(false);
            return Result.ok(vo);
        }
        vo.setSaveState(true);
        return Result.ok(vo);
    }

    @Override
    public Result<Integer> queryDeviceStatus(String deviceSerialNumber) {
        R<Integer> r = deviceManageService.queryDeviceStatus(deviceSerialNumber);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    /**
     * 保存草稿按钮
     * @param taskId
     * @return
     */
    @Override
    public Result saveDraftButton(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            return Result.fail("请先创建任务");
        }
        // 根据任务id查询任务
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        // 根据任务id和is_save字段为1的条件查询
        QueryWrapper<FMeasureData> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id", taskId).eq("is_save", "1");
        List<FMeasureData> measureDataList = fMeasureDataService.list(wrapper);
        if (measureDataList != null) {
            for (FMeasureData measureData : measureDataList) {
                measureData.setIsSave("0");
                boolean update1 = fMeasureDataService.updateById(measureData);
                if(!(update1)){
                    return Result.fail("保存草稿失败");
                }
            }
        }
        // 根据测量id,任务id还有is_save为1的条件查询分析数据
        QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("is_save", "1");
        List<FMeasureAnalysisData> analysisDataList = fMeasureAnalysisDataService.list(queryWrapper);
        if (analysisDataList != null) {
            for (FMeasureAnalysisData fMeasureAnalysisData : analysisDataList) {
                fMeasureAnalysisData.setIsSave("0");
                boolean update = fMeasureAnalysisDataService.updateById(fMeasureAnalysisData);
                if(!(update)){
                    return Result.fail("保存草稿失败");
                }
            }
        }
        task.setEndSaveTime(LocalDateTime.now());
        boolean update = fMeasureTaskService.updateById(task);
        if (update){
            return Result.ok();
        }else {
            return Result.fail("保存草稿失败");
        }
    }

    @Override
    public Result<Page<FMeasureMethodParam>> queryHistoryMethod(QueryMethodDto queryMethodDto) {
        String methodType = queryMethodDto.getMethodType();
        Integer pageSize = queryMethodDto.getPageSize();
        Integer pageNum = queryMethodDto.getPageNum();
        String userName = queryMethodDto.getUpdateBy();
        String selectKey = queryMethodDto.getSelectKey();

        Page<FMeasureMethod> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FMeasureMethod> methodLambdaQueryWrapper = new LambdaQueryWrapper<>();
        methodLambdaQueryWrapper.eq(FMeasureMethod::getMeasurementType, methodType)
                .eq(FMeasureMethod::getMethodCreator, userName)
                .orderByDesc(FMeasureMethod::getMethodUseTime);
        if (selectKey != null) {
            methodLambdaQueryWrapper.like(FMeasureMethod::getMethodName, selectKey);
        }

        Page<FMeasureMethod> fMeasureMethodPage = fMeasureMethodService.page(page, methodLambdaQueryWrapper);
        Page<FMeasureMethodParam> dtoPage = new Page<>();

        dtoPage.setCurrent(fMeasureMethodPage.getCurrent());
        dtoPage.setSize(fMeasureMethodPage.getSize());
        dtoPage.setTotal(fMeasureMethodPage.getTotal());
        dtoPage.setPages(fMeasureMethodPage.getPages());


        List<FMeasureMethodParam> dtoList = new ArrayList<>();
        fMeasureMethodPage.getRecords().forEach(fMeasureMethod -> {
            FMeasureMethodParam fMeasureMethodParam = new FMeasureMethodParam();
            BeanUtils.copyProperties(fMeasureMethod, fMeasureMethodParam);
            String measurementParamInfo = fMeasureMethod.getMeasurementParamInfo();

            JSONObject jsonObject = JSONObject.parseObject(measurementParamInfo);

            fMeasureMethodParam.setMeasurementParamInfo(jsonObject);
            dtoList.add(fMeasureMethodParam);
        });

        dtoPage.setRecords(dtoList);

        return Result.ok(dtoPage);


    }

    @Override
    public Result queryMethodParameter(String methodId) {
        // 根据方法id查询方法参数
        FMeasureMethod method = fMeasureMethodService.getById(methodId);

        if (method == null) {
            return Result.fail("方法不存在");
        }
        if (method.getMeasurementType().isEmpty()){
            return Result.fail("方法类型不存在");
        }
        MeasureTypeEnum measureTypeEnum = MeasureTypeEnum.fromDescription(method.getMeasurementType());
        if (measureTypeEnum == null) {

        }
        switch (measureTypeEnum){
            case PHOTOMETRIC_MEASUREMENT:
                return Result.ok();
            case QUANTITATIVE_MEASUREMENT:
                return Result.ok();
            case SPECTRAL_SCAN:
            {
                FSpectrumParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
                return Result.ok(parameter);
            }
            case TIME_SCAN:
            {
                FTimeSweepParamter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FTimeSweepParamter.class);
                return Result.ok(parameter);
            }
            default:
                return Result.fail("暂不支持");
        }

    }


    @Override
    public Result<Page<FMeasureTaskDTO>> queryHistoryTask(SelectTaskDto selectTaskDto) {
        String selectKey = selectTaskDto.getSelectKey();
        String methodType = selectTaskDto.getMethodType();
        String userName = selectTaskDto.getUpdateBy();
        Integer pageNum = selectTaskDto.getPageNum();
        Integer pageSize = selectTaskDto.getPageSize();

        SysUserPO sysUserPO = userMapper.selectByUsername(userName);
        if (sysUserPO == null) {
            return Result.fail("用户不存在");
        }

        String userId = sysUserPO.getId();
        LambdaQueryWrapper<FMeasureTask> fMeasureTaskLambdaQueryWrapper = new LambdaQueryWrapper<>();
        fMeasureTaskLambdaQueryWrapper.eq(FMeasureTask::getMeasureType, methodType)
                .eq(FMeasureTask::getUserId, userId)
                .eq(FMeasureTask::getIsDelete,"0")
                .orderByDesc(FMeasureTask::getEndSaveTime);

        if (selectKey != null) {
            fMeasureTaskLambdaQueryWrapper.like(FMeasureTask::getTaskName, selectKey);
        }

        Page<FMeasureTask> page = new Page<>(pageNum, pageSize);
        Page<FMeasureTask> fMeasureTaskPage = fMeasureTaskService.page(page, fMeasureTaskLambdaQueryWrapper);

        // 手动转换分页数据
        Page<FMeasureTaskDTO> fMeasureTaskDTOPage = convertToDtoPage(fMeasureTaskPage);

        return Result.ok(fMeasureTaskDTOPage);
    }

    /**
     * 将 FMeasureTask 的分页对象转换为 FMeasureTaskDTO 的分页对象
     */
    private Page<FMeasureTaskDTO> convertToDtoPage(Page<FMeasureTask> fMeasureTaskPage) {
        Page<FMeasureTaskDTO> dtoPage = new Page<>();
        // 复制分页信息
        BeanUtils.copyProperties(fMeasureTaskPage, dtoPage, "records");

        // 手动转换records列表
        List<FMeasureTaskDTO> dtoList = fMeasureTaskPage.getRecords().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    /**
     * 将单个 FMeasureTask 转换为 FMeasureTaskDTO
     */
    private FMeasureTaskDTO convertToDto(FMeasureTask entity) {
        if (entity == null) {
            return null;
        }

        FMeasureTaskDTO dto = new FMeasureTaskDTO();
        // 复制基本属性
        BeanUtils.copyProperties(entity, dto);

        // 设置方法名称
        String methodKey = entity.getMethodKey();
        if (methodKey != null) {
            LambdaQueryWrapper<FMeasureMethod> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(FMeasureMethod::getId, methodKey);
            FMeasureMethod method = fMeasureMethodService.getOne(lambdaQueryWrapper);
            if (method != null) {
                dto.setMethodName(method.getMethodName());
            }
        }

        return dto;
    }
    @Override
    public Result<String> updateMethod(UpdateMethodDto updateMethodDto) {
        if (updateMethodDto == null) {
            return Result.fail("参数不能为空");
        }
        String methodName = updateMethodDto.getMethodName();
        List<FMeasureMethod> methods = fMeasureMethodService.list();
        for (FMeasureMethod fMeasureMethod : methods) {
            String everyMethodName = fMeasureMethod.getMethodName();
            if (everyMethodName.equals(methodName)) {
                return Result.fail("方法名已存在");
            }
        }
        String methodId = updateMethodDto.getMethodId();
        LambdaQueryWrapper<FMeasureMethod> fMeasureMethodLambdaQueryWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<FMeasureMethod> eq = fMeasureMethodLambdaQueryWrapper.eq(FMeasureMethod::getId, methodId);
        FMeasureMethod method = fMeasureMethodService.getOne(eq);
        BeanUtils.copyProperties(updateMethodDto, method);
        fMeasureMethodService.updateById(method);
        return Result.ok("修改成功");
    }

    @Override
    public Result updateTask(UpdateTaskDto updateTaskDto) {
        if (updateTaskDto == null) {
            return Result.fail("参数不能为空");
        }
        String taskId = updateTaskDto.getTaskId();
        FMeasureTask byId = fMeasureTaskService.getById(taskId);
        if (byId == null) {
            return Result.fail("任务不存在");
        }
        byId.setTaskName(updateTaskDto.getTaskName());
        boolean update = fMeasureTaskService.updateById(byId);
        if (update) {
            return Result.ok("修改成功");
        } else {
            return Result.fail("修改失败");
        }

    }

    @Override
    public Result<Boolean> checkSampleInputParam(FCheckSampleInputParamDto dto) {

        FInputParam newInputParam = dto.getNewInputParam();
        if (newInputParam == null || !StringUtils.hasText(newInputParam.getResultIdentity())) {
            return Result.fail("请输入参数！");
        }

        if(newInputParam.getParamName().length() >= 256){
            return Result.fail("名称格式错误");
        }

        String methodMark = newInputParam.getResultIdentity();
        if (!Constants.PARAM_PATTERN.matcher(methodMark).matches()) {
            return Result.fail("标识格式错误：必须以大写字母开头，后跟1-10位数字");
        }

        if (dto.getInputParamList() != null && !dto.getInputParamList().isEmpty()) {

            for (FInputParam param : dto.getInputParamList()) {
                if (param.getResultIdentity().equals(newInputParam.getResultIdentity())) {
                    return Result.fail("标识已存在");
                }
            }
        }

        return Result.ok(true);
    }



    @Override
    public Result<Boolean> checkFormula(FCheckFormulaDto checkFormulaDto) {

        FPreCountResultParam newFormula = checkFormulaDto.getNewFormula();

        if (newFormula == null || !StringUtils.hasText(newFormula.getFormulaText())) {
            return Result.fail("请输入公式！");
        }

        List<FInputParam> inputParamList = checkFormulaDto.getInputParamList();
        if (inputParamList == null || inputParamList.isEmpty()) {
            return Result.fail("请先设置输入参数！");
        }

        List<String> paramList = new ArrayList<>();
        for (FInputParam param : inputParamList) {
            paramList.add(param.getResultIdentity());
        }

        List<String> formulaList = new ArrayList<>();
        formulaList.add(newFormula.getFormulaText());

        R<String> verified = latexConverter.verifyFormulasAgainstParamList(paramList, formulaList);
        if (!verified.getSuccess()) {
            return Result.fail(verified.getMessage());
        }

        return Result.ok(true);
    }


    /**
     * 获取报告参数
     * @param taskId
     * @return
     */
    @Override
    public Result obtainReportParameters(String taskId) {

        if ((taskId == null)) {
            return Result.fail("任务id不能为空");
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        if (task.getMeasureType().equals(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription())) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            SpectralReportVo vo = new SpectralReportVo();
//            TMeasureProject project =fMeasureProjectService.getById(task.getProjectItemKey());
            SysUserPO user = userMapper.selectById(task.getUserId());
            FMeasureMethod method =fMeasureMethodService.getById(task.getMethodKey());
            FPhotometricParameter methodparam = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);
            vo.setTaskName(task.getTaskName());

            if (StringUtils.hasText(task.getSampleKey())) {
                List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
                if (!taskSpecimen.isEmpty()) {
                    FTaskSpecimen specimen = taskSpecimen.get(0);
                    FMeasureSample sample =fMeasureSampleService.getById(specimen.getSpecimenKey());
                    vo.setSampleName(sample.getSampleName());
                    vo.setSampleNumber(sample.getSampleNumber());
                    vo.setSamplingTime(task.getCreateTime().format(formatter));

                    LambdaQueryWrapper<FMeasureData> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(FMeasureData::getTaskId, taskId)
                            .eq(FMeasureData::getSampleKey, sample.getId())
                            .orderByDesc(FMeasureData::getSerialNumber)
                            .last("LIMIT 1");
                    FMeasureData one =fMeasureDataService.getOne(wrapper);
                    if (one != null) {
                        if (one.getMeasurementStartTime() != null) {
                            vo.setDetectionStartTime(one.getMeasurementStartTime().format(formatter));
                        }
                        if (one.getMeasurementEndTime() != null) {
                            vo.setDetectionEndTime(one.getMeasurementEndTime().format(formatter));
                        }
                        if (one.getInstrumentData() != null) {
                            vo.setOriginalData(one.getInstrumentData());
                        }
                    }
                }
            }

            StringBuilder wave = new StringBuilder();
            if (methodparam.getWaveLength() != null && !methodparam.getWaveLength().isEmpty()) {
                for (String s : methodparam.getWaveLength()) {
                    wave.append(s).append(",");
                }
            }
            if (wave.length() > 0) {
                wave.deleteCharAt(wave.length() - 1);
            }
            vo.setInstrumentWavelength(wave.toString());

            vo.setReferenceMethod(method.getMethodName());
            vo.setReferenceStandard(methodparam.getReferenceStandardName());

            vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            vo.setLuminosityMode(WaveLengthEnum.getName(methodparam.getLuminosity()));
            vo.setSpectrumBandwidth(methodparam.getSpectralBandwidth());

            vo.setInstrumentName("紫外可见分光光度计");
            vo.setInstrumentModel("T600");

            vo.setReferenceMethod(method.getMethodName());
            vo.setReferenceStandard(methodparam.getReferenceStandardName());
            StringBuilder steps = new StringBuilder();
            if (methodparam.getPreProcessParameters() != null) {
                methodparam.getPreProcessParameters().forEach(preProcessParameter -> {
                    steps.append(preProcessParameter).append("\n");
                });
            }


            StringBuilder result = new StringBuilder();
            if (methodparam.getResultParams() != null) {
                methodparam.getResultParams().forEach(resultParam -> {
                    result.append(resultParam.getFormulaText()).append("\n");
                });
            }

            vo.setCalculationFormula(result.toString());

            result.setLength(0);
            if (methodparam.getSampleInputParams() != null) {
                methodparam.getSampleInputParams().forEach(sampleInputParam -> {
                    result.append(sampleInputParam.getResultIdentity()).append("——").append(sampleInputParam.getParamName()).append("\n");
                });
            }

            vo.setFormulaParams(result.toString());

            vo.setSteps(steps.toString());
            vo.setDetectionStartTime(task.getCreateTime().format(formatter));
            if (task.getEndSaveTime() != null){
                vo.setDetectionEndTime(task.getEndSaveTime().format(formatter));
            }
            vo.setDataType("测量数据");

            vo.setAmbientTemperature("26℃");
            vo.setRelativeHumidity("60%");
            vo.setSampleState("正常");
            vo.setColorPlateWavelength("1");


//            vo.setProject(project.getProjectName());
            vo.setDetector(user.getUsername());
            vo.setEnergyLight(methodparam.getEnergyLamp());
            vo.setMethodName(method.getMethodName());

            return Result.ok(vo);

        } else if (task.getMeasureType().equals(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            SpectralReportVo vo = new SpectralReportVo();
//            FMeasureProject project =fMeasureProjectService.getById(task.getProjectItemKey());
            vo.setTaskName(task.getTaskName());
            SysUserPO user = userMapper.selectById(task.getUserId());
            FMeasureMethod method =fMeasureMethodService.getById(task.getMethodKey());
            FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);

            vo.setRepetitionPattern(parameter.getRepeatMode());
            vo.setEquationType(RationCurveParamEnums.getName(parameter.getEquationType()));
            vo.setForceZeroPoint("否");
            vo.setInsertZeroPoint("否");
            for (String s : parameter.getZeroMethod()) {
                if (s.equals(RationCurveParamEnums.force_zero.getIdentity())) {
                    vo.setForceZeroPoint("是");
                }
                if (s.equals(RationCurveParamEnums.insert_zero.getIdentity())) {
                    vo.setInsertZeroPoint("是");
                }
            }
            vo.setAutoChangeSamplePool("否");

            R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(task.getDeviceSerialNumber());
            if (!poolInfo.getSuccess()) {
                FSamplePoolInfoVO poolInfoVO = poolInfo.getData();
                if (poolInfoVO != null) {
                    Boolean isAutoChangePool = poolInfoVO.getIsAutoChangePool();
                    if (isAutoChangePool) {
                        vo.setAutoChangeSamplePool("是");
                    }
                }

            }
            if (StringUtils.hasText(task.getSampleKey())) {
                List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);

                for (FTaskSpecimen specimen : taskSpecimen) {
                    if (specimen.getDeleteType().equals("1")) {
                        continue;
                    }
                    if (!specimen.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                        continue;
                    }
                    if (specimen.getIsTested().equals("0")) {
                        continue;
                    }
                    FMeasureSample sample =fMeasureSampleService.getById(specimen.getSpecimenKey());
                    vo.setStandardConcentration(sample.getNormalConcentration());
                    vo.setSampleName(sample.getSampleName());
                    vo.setSampleNumber(sample.getSampleNumber());
                    LambdaQueryWrapper<FMeasureData> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(FMeasureData::getTaskId, taskId)
                            .eq(FMeasureData::getSampleKey, sample.getId())
                            .orderByDesc(FMeasureData::getSerialNumber)
                            .last("LIMIT 1");
                    FMeasureData measureData =fMeasureDataService.getOne(wrapper);
                    vo.setSamplingTime(measureData.getSamplingTime());
                    if (measureData.getMeasurementStartTime() != null) {
                        vo.setDetectionStartTime(measureData.getMeasurementStartTime().format(formatter));
                    }
                    if (measureData.getMeasurementEndTime() != null) {
                        vo.setDetectionEndTime(measureData.getMeasurementEndTime().format(formatter));
                    }
                }
            }

            if (StringUtils.hasText(task.getFittingParam())){
                FittingReceiveInfo fittingReceiveInfo = JSON.parseObject(task.getFittingParam(), FittingReceiveInfo.class);
                StringBuilder param = new StringBuilder();
                for (Double coefficient : fittingReceiveInfo.getCoefficients()) {
                    param.append(coefficient).append(",");
                }
                param.deleteCharAt(param.length() - 1);
                vo.setCurveParams(param.toString());
                vo.setR(fittingReceiveInfo.getR() == null ? "" : fittingReceiveInfo.getR());
                vo.setR2(fittingReceiveInfo.getR2() == null ? "" : fittingReceiveInfo.getR2());
                vo.setCurveFormula(fittingReceiveInfo.getEquation() == null ? "" : fittingReceiveInfo.getEquation());
                vo.setCurveType(String.valueOf(fittingReceiveInfo.getDegree() == null ? "" : fittingReceiveInfo.getDegree()));
            }

            StringBuilder wave = new StringBuilder();
            if (parameter.getWaveLength() != null && !parameter.getWaveLength().isEmpty()) {
                for (String s : parameter.getWaveLength()) {
                    wave.append(s).append(",");
                }
            }
            if (wave.length() > 0) {
                wave.deleteCharAt(wave.length() - 1);
            }
            vo.setInstrumentWavelength(wave.toString());

            vo.setReferenceMethod(method.getMethodName());
            vo.setReferenceStandard(parameter.getReferenceStandardName());

            vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            vo.setSpectrumBandwidth(parameter.getSpectralBandwidth());

            vo.setInstrumentName("紫外可见分光光度计");
            vo.setInstrumentModel("T600");

            StringBuilder steps = new StringBuilder();
            if (parameter.getPreProcessParameters() != null) {
                parameter.getPreProcessParameters().forEach(preProcessParameter -> {
                    steps.append(preProcessParameter).append("\n");
                });
            }
            StringBuilder result = new StringBuilder();
            if (parameter.getResultParams() != null) {
                parameter.getResultParams().forEach(resultParam -> {
                    result.append(resultParam.getFormulaText()).append("\n");
                });
            }
            vo.setCalculationFormula(result.toString());

            result.setLength(0);
            if (parameter.getSampleInputParams() != null) {
                parameter.getSampleInputParams().forEach(sampleInputParam -> {
                    result.append(sampleInputParam.getResultIdentity()).append("——").append(sampleInputParam.getParamName()).append("\n");
                });
            }

            vo.setFormulaParams(result.toString());

            vo.setSteps(steps.toString());
            vo.setDetectionStartTime(task.getCreateTime().format(formatter));
            if (task.getEndSaveTime() != null){
                vo.setDetectionEndTime(task.getEndSaveTime().format(formatter));
            }
            vo.setDataType("测量数据");

            vo.setAmbientTemperature("26℃");
            vo.setRelativeHumidity("60%");
            vo.setSampleState("正常");
            vo.setColorPlateWavelength("1");

//            vo.setProject(project.getProjectName());
            vo.setDetector(user.getUsername());
            vo.setEnergyLight(parameter.getEnergyLamp());
            vo.setMethodName(method.getMethodName());
            return Result.ok(vo);
        } else if (task.getMeasureType().equals(MeasureTypeEnum.SPECTRAL_SCAN.getDescription())) {
            // 根据任务中的方法key查询方法
            FMeasureMethod method =fMeasureMethodService.getById(task.getMethodKey());
            if (method == null) {
                return Result.fail("方法不存在");
            }
            SpectralReportVo vo = new SpectralReportVo();
            // 将方法参数提取出来拷贝到report中
            SpectrumParameter param = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
            vo.setStartWaveLength(param.getStartWaveLength());
            vo.setEndWaveLength(param.getEndWaveLength());
            vo.setScanSpeed(param.getScanSpeed());
            vo.setTimeInterval(param.getTimeInterval());
            vo.setStairCorrection(param.getLadderCorrection() == true ? "启用" : "不启用");
            vo.setRepetitionPattern(param.getRepetitionPattern().equals("0")?"不重复":"单池重复");
            if (param.getRepetitionPattern().equals("1")){
                vo.setRepetitionTime(param.getRepetitionTime());
            }
            vo.setWaveLengthInterval(param.getWaveLengthInterval());
            ///  仪器
            vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            // 将光度模式转换为(1.abs 2.T% 3.Es 4.Er)用equals判断,首字母大写
            vo.setLuminosityMode(param.getLuminosity().equals("1")?"Abs":param.getLuminosity().equals("2")?"T%":param.getLuminosity().equals("3")?"Es":param.getLuminosity().equals("4")?"Er":"");
            vo.setResponseTime(param.getResponseTime());
            vo.setSpectrumBandwidth(param.getSpectralBandwidth());
            vo.setChangeLampWaveLength(param.getChangeLampWaveLength());
            // 能量灯(0.钨灯 1.氘灯 2.汞灯)
            vo.setEnergyLight(param.getEnergyLamp().equals("0") ? "钨灯" : param.getEnergyLamp().equals("1") ? "氘灯" : "汞灯");
            vo.setGain(param.getGain().toString());
//            vo.setProject(project.getProjectName());
            vo.setDetector(method.getMethodCreator());
            vo.setMethodName(method.getMethodName());
            vo.setTaskName(task.getTaskName());

            // 仪器型号
            vo.setInstrumentModel("T600");
            return Result.ok(vo);
        }else if (task.getMeasureType().equals(MeasureTypeEnum.TIME_SCAN.getDescription())){
            // 根据任务中的方法key查询方法
            FMeasureMethod method =fMeasureMethodService.getById(task.getMethodKey());
            if (method == null) {
                return Result.fail("方法不存在");
            }
            SpectralReportVo vo = new SpectralReportVo();
            // 将方法参数提取出来拷贝到report中
            FTimeSweepParamter param = JSON.parseObject(method.getMeasurementParamInfo(), FTimeSweepParamter.class);
            vo.setInstrumentWavelength(param.getWaveLength());
            vo.setScanTime(param.getScanTime());
            vo.setTimeUnit(param.getScanUnit().equals("0")?"分":"秒");
            vo.setTimeInterval(param.getIntervalTime());
            vo.setTotalNumber(param.getTotalNumber());
            vo.setMultiPoolMode(param.getMultiPoolMode().equals("0") ? "顺序单池连续采样" : "循环多池交替采样");
            ///  仪器
            vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            // 将光度模式转换为(1.abs 2.T% 3.Es 4.Er)用equals判断,首字母大写
            vo.setLuminosityMode(param.getLuminosity().equals("1")?"Abs":param.getLuminosity().equals("2")?"T%":param.getLuminosity().equals("3")?"Es":param.getLuminosity().equals("4")?"Er":"");
            vo.setResponseTime(param.getResponseTime());
            vo.setSpectrumBandwidth(param.getSpectralBandwidth());
            vo.setChangeLampWaveLength(param.getChangeLampWaveLength());
            // 能量灯(0.钨灯 1.氘灯 2.汞灯)
            vo.setEnergyLight(param.getEnergyLamp().equals("0") ? "钨灯" : param.getEnergyLamp().equals("1") ? "氘灯" : "汞灯");
            vo.setGain(param.getGain().toString());
//            vo.setProject(project.getProjectName());
            vo.setDetector(method.getMethodCreator());
            vo.setMethodName(method.getMethodName());
            vo.setTaskName(task.getTaskName());

            // 仪器型号
            vo.setInstrumentModel("T600");
            return Result.ok(vo);
        }else{
            return Result.ok();
        }
    }

    /**
     * 查询报告参数
     * @param taskId
     * @param sampleId
     * @param serialNumber
     * @return
     */
    @Override
    public Result queryReportAllParameters(String taskId, String sampleId, String serialNumber) {
        // 校验任务id
        if (StringUtils.isEmpty(taskId)) {
            return Result.fail("请选择任务");
        }
        // 校验样品id
        if (StringUtils.isEmpty(sampleId)) {
            return Result.fail("请选择样品");
        }
        // 根据任务id查询任务
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        // 根据任务中的所属项目的Key查询项目
//        FMeasureProject project =fMeasureProjectService.getById(task.getProjectItemKey());
//        if (project == null) {
//            return Result.fail("项目不存在");
//        }
        // 根据任务中的方法key查询方法
        FMeasureMethod method =fMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            return Result.fail("方法不存在");
        }
        // 根据样品id查询样品数据
        FMeasureSample sample =fMeasureSampleService.getById(sampleId);
        if (sample == null) {
            return Result.fail("样品不存在");
        }
        // 编写查询测量的条件
        QueryWrapper<FMeasureData> wrapper = new QueryWrapper<>();
        wrapper.eq("task_key",taskId);
        wrapper.eq("sample_key",sampleId);
        // 如果存在编号，则查询该设备编号的测量数据
        if (StringUtils.hasText(serialNumber)) {
            wrapper.eq("serial_number",serialNumber);
        }
        wrapper.last("limit 1");
        FMeasureData measureData =fMeasureDataService.getOne(wrapper);
        if (measureData == null) {
            return Result.fail("未找到该测量数据");
        }
        // 编写分析的查询条件
        QueryWrapper<FMeasureAnalysisData> analysisWrapper = new QueryWrapper<>();
        analysisWrapper.eq("task_id", taskId);
        analysisWrapper.eq("sample_key", sampleId);
        if (StringUtils.hasText(serialNumber)) {
            analysisWrapper.eq("analysis_number", serialNumber);
        }
        analysisWrapper.last("limit 1");
        FMeasureAnalysisData analysisData =fMeasureAnalysisDataService.getOne(analysisWrapper);
        if (analysisData == null) {
            return Result.fail("没有找到该分析数据");
        }
        // 根据分析id查询分析图片数据
        QueryWrapper<FMeasureAnalysisImage> attachmentWrapper = new QueryWrapper<>();
        attachmentWrapper.eq("analysis_id", analysisData.getId());
        attachmentWrapper.last("limit 1");
        FMeasureAnalysisImage attachment =fMeasureAnalysisImageService.getOne(attachmentWrapper);
        if (attachment == null) {
            return Result.fail("没有找到对应的图片数据");
        }
        // 根据任务中测量类型返回对应的参数信息
        MeasureTypeEnum measureTypeEnum = MeasureTypeEnum.fromDescription(task.getMeasureType());
        switch (measureTypeEnum) {
            case SPECTRAL_SCAN: {
                SpectralReportParametersVo vo = new SpectralReportParametersVo();
                SpectralReport report = new SpectralReport();
                // 将方法参数提取出来拷贝到report中
                SpectrumParameter param = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
                BeanUtils.copyProperties(param, report);
                /// 测量
                // 将扫描速度的参数转换为(0.慢速 1.中速 2.快速 3.极快)用equals判断
                report.setScanSpeed(param.getScanSpeed().equals("0")?"慢速":param.getScanSpeed().equals("1")?"中速":param.getScanSpeed().equals("2")?"快速":"极快");
                // 将阶梯矫正的参数转换为(true.启用 false.不启用)用equals判断
                report.setLadderCorrection(param.getLadderCorrection().equals(true)?"启用":"不启用");
                // 将重复模式转换为(0.不重复 1.单池重复)用equals判断
                report.setRepetitionPattern(param.getRepetitionPattern().equals("0")?"不重复":"单池重复");
                report.setDetectionStartTime(measureData.getMeasurementStartTime().format(formatter));
                report.setDetectionEndTime(measureData.getMeasurementEndTime().format(formatter));
                ///  仪器
                report.setDeviceSerialNumber(task.getDeviceSerialNumber());
                // 将光度模式转换为(1.abs 2.T% 3.Es 4.Er)用equals判断,首字母大写
                report.setLuminosityMode(param.getLuminosity().equals("1")?"Abs":param.getLuminosity().equals("2")?"T%":param.getLuminosity().equals("3")?"Es":"Er");
                // 将能量灯转换为(0.钨灯 1.氘灯 2.汞灯)
                report.setEnergyLamp(param.getEnergyLamp().equals("0")?"钨灯":param.getEnergyLamp().equals("1")?"氘灯":"汞灯");

                /// 其他
//                report.setProjectName(project.getProjectName());
                report.setUserBy(method.getMethodCreator());
                if (serialNumber!=null){
                    report.setSampleName(sample.getSampleName()+"-"+serialNumber);
                }else{
                    report.setSampleName(sample.getSampleName());
                }
                report.setSampleNumber(sample.getSampleNumber());
                report.setSpectrumScanImage(attachment.getImageBase64());
                report.setMethodName(method.getMethodName());
                report.setTaskName(task.getTaskName());
                vo.setData(report);
                return Result.ok(vo);
            }
            case TIME_SCAN: {
                TimeSweepReportParametersVo vo = new TimeSweepReportParametersVo();
                TimeSweepReport report = new TimeSweepReport();
                // 将方法参数提取出来拷贝到report中
                TimeSweepParamter param = JSON.parseObject(method.getMeasurementParamInfo(), TimeSweepParamter.class);
                BeanUtils.copyProperties(param, report);
                /// 测量
                // 将扫描单位的参数转换为(0.分 1.秒)用equals判断
                report.setScanUnit(param.getScanUnit().equals("0") ? "分" : "秒");
                // 将多联池模式参数转换为(0.顺序单池连续采样 1.循环多池交替采样)用equals判断
                report.setMultiPoolMode(param.getMultiPoolMode().equals("0") ? "顺序单池连续采样" : "循环多池交替采样");
                report.setDetectionStartTime(measureData.getMeasurementStartTime().format(formatter));
                report.setDetectionEndTime(measureData.getMeasurementEndTime().format(formatter));
                ///  仪器
                report.setDeviceSerialNumber(task.getDeviceSerialNumber());
                // 将光度模式转换为(1.abs 2.T% 3.Es 4.Er)用equals判断,首字母大写
                report.setLuminosityMode(param.getLuminosity().equals("1")?"Abs":param.getLuminosity().equals("2")?"T%":param.getLuminosity().equals("3")?"Es":"Er");
                // 将能量灯转换为(0.钨灯 1.氘灯 2.汞灯)
                report.setEnergyLamp(param.getEnergyLamp().equals("0")?"钨灯":param.getEnergyLamp().equals("1")?"氘灯":"汞灯");

                /// 其他
//                report.setProjectName(project.getProjectName());
                report.setUserBy(method.getMethodCreator());
                if (serialNumber!=null){
                    report.setSampleName(sample.getSampleName()+"-"+serialNumber);
                }else{
                    report.setSampleName(sample.getSampleName());
                }
                report.setSampleNumber(sample.getSampleNumber());
                report.setTimeSweepScanImage(attachment.getImageBase64());
                report.setMethodName(method.getMethodName());
                report.setTaskName(task.getTaskName());
                vo.setData(report);
                return Result.ok(vo);
            }
            default: {
                return Result.fail("未知的测量类型");
            }
        }
    }
    public static Object convertSciToDecimal(Object obj) {
        if (obj == null) { // 额外处理null，避免空指针
            return null;
        }
        String str = String.valueOf(obj);
        // 核心逻辑：只要含e或E，就尝试转小数
        if (str.toLowerCase().contains("e")) {
            try {
                // 尝试转成Double再格式化，成功则返回小数格式
                DecimalFormat df = new DecimalFormat("0.##########");
                return df.format(Double.parseDouble(str));
            } catch (NumberFormatException e) {
                // 转失败（如哈希串），直接返回原字符串
                return str;
            }
        } else {
            // 不含e/E，直接返回原字符串（封装为Object）
            return str;
        }}


    /**
     * 新建报告表数据查询
     * @param taskId
     * @param sampleId
     * @return
     */
    @Override
    public Result newReportTableDataQuery(String taskId, String sampleId,String analysisId, Boolean allDisplay) {

        if (!StringUtils.hasText(taskId)) {
            return Result.fail("请选择任务");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        // 根据任务id查询方法,再从json转换为对象方法参数
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            return Result.fail("方法不存在");
        }

        Integer digit = Constants.DEFAULT_DECIMAL_NUM;
        FQueryDataDTO dataDTO = new FQueryDataDTO();
        LambdaQueryWrapper<FMeasureTaskAttachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureTaskAttachment::getTaskId, taskId);
        FMeasureTaskAttachment attachment = fMeasureTaskAttachmentService.getOne(queryWrapper);
        if (attachment != null && attachment.getDataParam() != null) {
            dataDTO = JSON.parseObject(attachment.getDataParam(), FQueryDataDTO.class);
        }

        switch (task.getMeasureType()) {
            case "光度测量": {
                FPhotometricReportDataVO vo = new FPhotometricReportDataVO();
                if (allDisplay == null) {
                    allDisplay = false;
                }

                Result<List<FHeadVO>> result = photometricService.tableHead(taskId, task.getMethodKey(), allDisplay, null);
                if (result.getCode() == 200) {
                    List<FHeadVO> vos = result.getData();
                    vo.setHeadList(vos);
                }

                if (dataDTO == null) {
                    dataDTO = new FQueryDataDTO();
                    dataDTO.setTaskId(taskId);
                    dataDTO.setAllDisplay(false);
                    dataDTO.setDigit(digit);
                    dataDTO.setOption(new ArrayList<>());
                }

                Result<List<Map<String, Object>>> data = photometricService.queryData(dataDTO);
                if (data.getCode() == 200) {
                    vo.setDataList(data.getData());
//                    data.getData().forEach(item -> {
//                        item.put(SampleTableEnum.SAMPLE_TYPE.getIdentity(), SampleTableEnum.getName(item.get(SampleTableEnum.SAMPLE_TYPE.getIdentity()).toString()));
//                    });
                }
                if (StringUtils.hasText(task.getTableInfo())) {
                    FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
                    if (snapshot != null && snapshot.getMapRelation() != null) {
                        vo.setMapRelation(snapshot.getMapRelation());
                    }
                }
                List<Map<String, Object>> dataList1 = vo.getDataList();
                for (Map<String, Object> stringObjectMap : dataList1) {
                    for (String key : stringObjectMap.keySet()) {
                        if(key.equals("Abs")){
                        Object value = stringObjectMap.get(key);
                        if (value != null) {
                            Object o = convertSciToDecimal(value);
                            String strValue = o.toString();
                            if (strValue.length() >= 6) {
                                stringObjectMap.put(key, strValue.substring(0, 6));
                            }
                        }
                        }
                    }
                }
                return Result.ok(vo);
            }
            case "定量测定":  {
                FRationReportDataVO vo = new FRationReportDataVO();
                Result<List<FHeadVO>> standHeads = rationService.getStandSampleTableHead(taskId, task.getMethodKey(), allDisplay, null);
                if (standHeads.getCode() == 200) {
                    vo.setStandHeadList(standHeads.getData());
                }
                Result<List<FHeadVO>> unknowHeads = rationService.getUnknownSampleTableHead(taskId, task.getMethodKey(), allDisplay, null);
                if (unknowHeads.getCode() == 200) {
                    vo.setUnknowHeadList(unknowHeads.getData());
                }
                if (dataDTO == null) {
                    dataDTO = new FQueryDataDTO();
                    dataDTO.setTaskId(taskId);
                    dataDTO.setAllDisplay(false);
                    dataDTO.setDigit(digit);
                    dataDTO.setOption(new ArrayList<>());
                }
                Result<FRationSampleDataVO> data = rationService.querySampleData(dataDTO);
                if (data.getCode() == 200) {
                    FRationSampleDataVO sampleDataVO = data.getData();
                    vo.setStandDataList(sampleDataVO.getStandSampleData());
                    vo.setUnknowDataList(sampleDataVO.getUnknowSampleData());
                }
                FRationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), FRationTaskHeadSnapShot.class);
                if (shot != null && shot.getUnknownHead() != null) {
                    FTaskHeadSnapshot unknownHead = shot.getUnknownHead();
                    vo.setMapRelation(unknownHead.getMapRelation());
                }
                Result<FRationFitCurveVO> curveInfo = rationService.queryCurveInfo(taskId, task.getMethodKey());
                if (curveInfo.getCode() == 200) {
                    vo.setCurveInfo(curveInfo.getData());
                }
                List<Map<String, Object>> dataList1 = vo.getStandDataList();
                for (Map<String, Object> stringObjectMap : dataList1) {
                    for (String key : stringObjectMap.keySet()) {
                        if(key.equals("Abs")){
                            Object value = stringObjectMap.get(key);
                            if (value != null) {
                                Object o = convertSciToDecimal(value);
                                String strValue = o.toString();
                                if (strValue.length() >= 6) {
                                    stringObjectMap.put(key, strValue.substring(0, 6));
                                }
                            }
                        }
                    }
                }
                return Result.ok(vo);
            }
            case "光谱扫描":
            {
                if (!StringUtils.hasText(sampleId)){
                    return Result.fail("请选择样品");
                }
                // 样品根据逗号进行分割然后存入List中
                List<String> sampleIdList = Arrays.asList(sampleId.split(","));
                FSpectrumParameter param = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);

                // 判断方法参数中的重复模式为true的话就分割分析id
                List<String> analysisIdList = new ArrayList<>();
                if (param.getRepetitionPattern()!=null&&param.getRepetitionPattern().equals("单池重复")) {
                    analysisIdList = Arrays.asList(analysisId.split(","));
                    // 过滤掉分析结果为null的分析id
                    analysisIdList = analysisIdList.stream()
                            .filter(id -> {
                                // 根据分析ID查询分析数据结果
                                // `getAnalysisResultById` 来根据分析ID获取分析结果
                                String analysisResult = getAnalysisResultById(id);
                                return analysisResult != null; // 只保留分析结果不为null的分析ID
                            })
                            .collect(Collectors.toList());
                }

                FSpectrumReportDataVO vo = new FSpectrumReportDataVO();
                Result<List<FHeadVO>> result = spectrumConciseService.tableHead(taskId, task.getMethodKey(), sampleIdList, analysisIdList);
                if (result.getCode() == 200) {
                    List<FHeadVO> vos = result.getData();
                    vo.setHeadList(vos);
                }
                // 获取数据
                List<Map<String, Object>> dataList = spectrumConciseService.queryTableData(taskId, task.getMethodKey() ,sampleIdList, analysisIdList);
                if (dataList != null){
                    vo.setDataList(dataList);
                }else{
                    vo.setDataList(new ArrayList<>());
                }

                if (StringUtils.hasText(task.getTableInfo())) {
                    FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
                    if (snapshot != null && snapshot.getMapRelation() != null) {
                        vo.setMapRelation(snapshot.getMapRelation());
                    }
                }
                List<Map<String, Object>> dataList1 = vo.getDataList();
                for (Map<String, Object> stringObjectMap : dataList1) {
                    for (String key : stringObjectMap.keySet()) {
                        if(key.equals("Abs")){
                            Object value = stringObjectMap.get(key);
                            if (value != null) {
                                Object o = convertSciToDecimal(value);
                                String strValue = o.toString();
                                if (strValue.length() >= 6) {
                                    stringObjectMap.put(key, strValue.substring(0, 6));
                                }
                            }
                        }
                    }
                }
                return Result.ok(vo);
            }
            case "时间扫描":
            {
                if (!StringUtils.hasText(sampleId)){
                    return Result.fail("请选择样品");
                }
                // 样品根据逗号进行分割然后存入List中
                List<String> sampleIdList = Arrays.asList(sampleId.split(","));
                FTimeSweepReportDataVo vo = new FTimeSweepReportDataVo();
                Result<List<FHeadVO>> result = timeSweepConciseService.tableHead(taskId, task.getMethodKey(), sampleIdList);
                if (result.getCode() == 200) {
                    List<FHeadVO> vos = result.getData();
                    vo.setHeadList(vos);
                }
                // 获取数据
                List<Map<String, Object>> dataList = timeSweepConciseService.queryTableData(taskId, sampleIdList);
                if (dataList != null){
                    vo.setDataList(dataList);
                }

                if (StringUtils.hasText(task.getTableInfo())) {
                    FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
                    if (snapshot != null && snapshot.getMapRelation() != null) {
                        vo.setMapRelation(snapshot.getMapRelation());
                    }
                }
                List<Map<String, Object>> dataList1 = vo.getDataList();
                for (Map<String, Object> stringObjectMap : dataList1) {
                    for (String key : stringObjectMap.keySet()) {
                        if(key.equals("Abs")){
                            Object value = stringObjectMap.get(key);
                            if (value != null) {
                                Object o = convertSciToDecimal(value);
                                String strValue = o.toString();
                                if (strValue.length() >= 6) {
                                    stringObjectMap.put(key, strValue.substring(0, 6));
                                }
                            }
                        }
                    }
                }

                return Result.ok(vo);
            }
            default:
                return Result.fail("类型错误,请重新选择");
        }
    }

    /**
     * 是否添加样本
     * @param taskId
     * @return
     */
    @Override
    public Result isAddSample(String taskId, String deviceSerialNumber) {
        if (StringUtils.isEmpty(taskId)) {
            return Result.fail("任务id不能为空");
        }
        if (StringUtils.isEmpty(deviceSerialNumber)) {
            return Result.fail("设备编号不能为空");
        }

        // 根据任务id查询任务信息
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        List<FTaskSpecimen> sampleData = com.alibaba.fastjson2.JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        if (sampleData == null) {
            // 如果没有样品，直接创建第一个样品
            return createSample(task, deviceSerialNumber, 1);
        }

        // 1. 查询任务中的所有样品编号，查找最大编号并递增
        boolean status = true;
        for (FTaskSpecimen fTaskSpecimen : sampleData) {
            if (fTaskSpecimen.getIsTested().equals("0") && fTaskSpecimen.getDeleteType().equals("0")) {
                status = false;
                break;
            }
        }

        if (status) {
            // 2. 查询现有的样品编号
            List<String> sampleIds = sampleData.stream()
                    .map(FTaskSpecimen::getSpecimenKey)
                    .collect(Collectors.toList());

            // 3. 查询样品表获取当前样品
            List<FMeasureSample> samples = fMeasureSampleService.list(new QueryWrapper<FMeasureSample>().in("id", sampleIds));

            // 4. 找到当前所有“默认样品”编号，并递增查找下一个可用的编号
            int newSampleNumber = findNextSampleNumber(samples);

            // 5. 创建新样品
            return createSample(task, deviceSerialNumber, newSampleNumber);
        }

        return Result.ok(false);
    }

    /**
     * 根据现有样品查找下一个可用的样品编号
     */
    private int findNextSampleNumber(List<FMeasureSample> samples) {
        // 获取所有样品编号（假设编号的格式为：默认样品1、默认样品2、默认样品N）
        List<Integer> sampleNumbers = samples.stream()
                .filter(sample -> sample.getSampleName().startsWith("默认样品"))
                .map(sample -> Integer.parseInt(sample.getSampleName().replace("默认样品", "")))  // 获取样品编号部分
                .collect(Collectors.toList());

        // 获取最大编号并加1作为新编号
        int maxSampleNumber = sampleNumbers.stream()
                .max(Integer::compareTo)
                .orElse(0);

        return maxSampleNumber + 1;  // 返回下一个可用编号
    }

    /**
     * 创建新的样品
     */
    private Result createSample(FMeasureTask task, String deviceSerialNumber, int newSampleNumber) {
        // 创建新的样品
        FMeasureSample sample = new FMeasureSample();
        Random random = new Random();
        int collect = 100 + random.nextInt(900);
        sample.setSampleName("默认样品" + newSampleNumber);  // 使用新样品编号创建样品名称
        sample.setSampleNumber(String.valueOf(newSampleNumber));  // 设置新样品编号

        // 设置样品池位置
        LambdaQueryWrapper<FSamplePoolConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FSamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        FSamplePoolConfigPO configPO = fSamplePoolConfigService.getOne(wrapper);
        if (configPO == null) {
            return Result.fail("未找到样品池信息");
        }

        if (configPO.getIsAutoChangePool()) {
            sample.setSampleLocation(configPO.getCurrentPoolNum().toString());
        }

        // 保存新样品
        boolean save = fMeasureSampleService.save(sample);
        if (!save) {
            return Result.fail("创建样品失败");
        }

        // 将新样品加入任务样品列表
        FTaskSpecimen fTaskSpecimen = new FTaskSpecimen();
        fTaskSpecimen.setSpecimenKey(sample.getId());
        fTaskSpecimen.setDeleteType("0");
        fTaskSpecimen.setIsTested("0");

        List<FTaskSpecimen> sampleData = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);

        if (sampleData == null){
            sampleData = new ArrayList<>();
            // 添加新样品到任务
            sampleData.add(fTaskSpecimen);
        }else{
            // 添加新样品到任务
            sampleData.add(fTaskSpecimen);
        }
        task.setSampleKey(JSON.toJSONString(sampleData));

        // 更新任务
        boolean update = fMeasureTaskService.updateById(task);
        if (!update) {
            return Result.fail("更新任务失败");
        }

        return Result.ok(true);
    }

    @Override
    public Result<DeviceMeasureVO> queryDeviceTypeInfo(String deviceSerialNumber) {

        R<DeviceMeasureVO> re = deviceManageService.queryDeviceTypeInfo(deviceSerialNumber);
        if (!re.getSuccess()) {
            return Result.fail(re.getMessage());
        }

        return Result.ok(re.getData());

    }

    @Override
    public Result delMethod(String methodId) {
        if (!StringUtils.hasText(methodId)) {
            return Result.fail("参数不能为空");
        }
        QueryWrapper<FMeasureTask> wrapper = new QueryWrapper<>();
        wrapper.eq("method_key", methodId);
        wrapper.eq("is_delete", "0");
        wrapper.last("LIMIT 1");
        FMeasureTask task = fMeasureTaskService.getOne(wrapper,false);
        if (!ObjectUtils.isEmpty(task)){
            return Result.fail("存在与该历史方法相关联的历史文件，请删除关联历史文件之后进行方法删除！！！");
        }
        boolean b = fMeasureMethodService.removeById(methodId);
        if (!b) {
            return Result.fail("更新任务失败");
        }
        return Result.ok(true);
    }

    @Override
    public Result delTask(String  taskId) {
        boolean b=fMeasureTaskService.delTask(taskId);
        if (b){
            return Result.ok(true);
        }
        return Result.fail();
    }

    @Override
    public Result<String> copy(CopyParam copyParam) {
        String taskName = copyParam.getTaskName();
        String taskId = copyParam.getTaskId();
        if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isBlank(taskId)){
            return Result.fail("拷贝失败,传入taskId不能为空！！！");
        }
        try {
            /*
             * 复制任务
             */
            FMeasureTask task = fMeasureTaskService.getById(taskId);
            if (ObjectUtils.isEmpty(task)){
                return Result.fail("未找到任务！！！");
            }
            //自定义任务的ID
            String uuid = UUID.randomUUID().toString().replace("-", "");
            task.setId(uuid);
            task.setEndSaveTime(LocalDateTime.now());
            /*
             * 复制样品
             */
            String sampleKey = task.getSampleKey();
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(sampleKey, TaskSpecimen.class);
            if (!CollectionUtils.isEmpty(taskSpecimen)) {
                taskSpecimen.forEach(sample -> {
                            String specimenKey = sample.getSpecimenKey();
                            FMeasureSample newSample = fMeasureSampleService.getById(specimenKey);
                            //保存样本数据，生成新的样本ID
                            newSample.setId(null);
                            fMeasureSampleService.save(newSample);

                            //进行数据复制
                            LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                            queryWrapper.eq(FMeasureData::getTaskId, taskId);
                            queryWrapper.eq(FMeasureData::getSampleKey, specimenKey);
                            List<FMeasureData> fMeasureDataList = fMeasureDataService.list(queryWrapper);

                            //构建新的数据list
                            if (!CollectionUtils.isEmpty(fMeasureDataList)) {
                                fMeasureDataList.forEach(m -> {
                                    String uuid1 = UUID.randomUUID().toString().replace("-", "");

                                    m.setId(uuid1);
                                    m.setTaskId(task.getId());
                                    //数据拿到新的样本ID进行关联
                                    m.setSampleKey(newSample.getId());

                                    LambdaQueryWrapper<FMeasureAnalysisData> queryWrapper1 = new LambdaQueryWrapper<>();
                                    queryWrapper1.eq(FMeasureAnalysisData::getTaskId, taskId);
                                    queryWrapper1.eq(FMeasureAnalysisData::getSampleKey, specimenKey);
                                    List<FMeasureAnalysisData> fMeasureAnalysisDataList = fMeasureAnalysisDataService.list(queryWrapper1);
                                    if (!CollectionUtils.isEmpty(fMeasureAnalysisDataList)) {
                                        fMeasureAnalysisDataList.forEach(analysisData -> {
                                            analysisData.setId(null);
                                            analysisData.setMeasureDataId(m.getId());
                                            analysisData.setTaskId(task.getId());
                                            analysisData.setSampleKey(newSample.getId());
                                        });
                                        fMeasureAnalysisDataService.saveBatch(fMeasureAnalysisDataList);
                                    }
                                });
                            }
                            fMeasureDataService.saveBatch(fMeasureDataList);


                            //新的样本ID生成并赋值给json，用于任务表中的样本关联字段
                            sample.setSpecimenKey(newSample.getId());
                        }
                );
                task.setSampleKey(JSON.toJSONString(taskSpecimen));
            }

//            task.setId(null);
            task.setTaskName(taskName);
            //另存为之后的设置为保存
            task.setIsSave("0");
            fMeasureTaskService.save(task);
            return Result.ok("拷贝成功");

        } catch (Exception e) {
            return Result.fail("拷贝失败");
        }

    }


    /**
     * 根据分析ID获取分析结果
     * @param analysisId
     * @return
     */
    private String getAnalysisResultById(String analysisId) {
        // 根据分析ID从数据库获取分析结果
        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getById(analysisId);
        if (analysisData == null) {
            return null;
        }
        if (analysisData.getAnalysisResult()==null|| analysisData.getAnalysisResult().isEmpty()){
            return null;
        }
        return analysisData.getAnalysisResult();
    }

    @Override
    public Result<Boolean> checkApproximateRange(FRoundingVO vo) {
        List<FRoundingParamVO> params = vo.getRoundingParams();

        // 1. 检查每个区间内部的合法性
        for (FRoundingParamVO param : params) {
            Double lowerLimit = param.getLowerLimit();
            Double upperLimit = param.getUpperLimit();

            // 只有当下限和上限都不为空时才进行检查
            if (lowerLimit != null && upperLimit != null) {
                // 下限值不能大于上限值
                if (lowerLimit > upperLimit) {
                    return Result.fail("下限值不能大于上限值");
                }
                // 如果下限值等于上限值且为有限值，必须同时包含两端点
                if (lowerLimit.equals(upperLimit)) {
                    if (!param.getIsLowerLimitIncluded() || !param.getIsUpperLimitIncluded()) {
                        return Result.fail("当上下限值相等且为有限值时，必须同时包含两端点");
                    }
                }
            }
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
                return Result.fail("区间之间不能存在重叠");
            }
        }

        return Result.ok(true);
    }

    /**
     * 判断两个区间是否重叠
     *
     * @param interval1 第一个区间
     * @param interval2 第二个区间
     * @return 如果重叠返回true，否则返回false
     */
    private boolean isOverlapping(FRoundingParamVO interval1, FRoundingParamVO interval2) {
        Double lower1 = interval1.getLowerLimit();
        Double upper1 = interval1.getUpperLimit();
        boolean includeLower1 = Boolean.TRUE.equals(interval1.getIsLowerLimitIncluded());
        boolean includeUpper1 = Boolean.TRUE.equals(interval1.getIsUpperLimitIncluded());

        Double lower2 = interval2.getLowerLimit();
        Double upper2 = interval2.getUpperLimit();
        boolean includeLower2 = Boolean.TRUE.equals(interval2.getIsLowerLimitIncluded());
        boolean includeUpper2 = Boolean.TRUE.equals(interval2.getIsUpperLimitIncluded());

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
