package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricParameter;
import com.persee.workstation.uv_inexus.dto.measurementDto.RationParameter;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureAnalysisData;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureData;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParam;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPreCountResultParam;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FSpectrumParameter;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTimeSweepParamter;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureMethodService;
import com.persee.workstation.uv_inexus.service.guide.parseReport.LaTeXConverter;
import com.persee.workstation.uv_inexus.vo.concise.common.FMethodInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FMethodListWithPageVO;
import com.persee.workstation.uv_inexus.vo.concise.common.SaveStateVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private FMeasureDataService fMeasureDataService;
    @Autowired
    private FMeasureAnalysisDataService fMeasureAnalysisDataService;
    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    private PhotometricConciseService photometricService;

    @Autowired
    private RationConciseService rationService;

    @Autowired
    private FSamplePoolConfigService fSamplePoolConfigService;

    @Autowired
    LaTeXConverter latexConverter;

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
        if (!measureDataList.isEmpty() && !analysisDataList.isEmpty()){
            vo.setSaveState(false);
            return Result.ok(vo);
        }
        vo.setSaveState(true);
        return Result.ok(vo);
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
                .eq(FMeasureTask::getUserId, userId).orderByDesc(FMeasureTask::getEndSaveTime);

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


}
