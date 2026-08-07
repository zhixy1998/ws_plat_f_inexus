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
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.dto.measurementDto.config.SampleNameConfig;
import com.persee.workstation.uv_inexus.dto.measurementDto.config.SampleNumConfig;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.MappingParamEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.GuidePublicUtilService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureMethodService;
import com.persee.workstation.uv_inexus.service.guide.parseReport.LaTeXConverter;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemBusinessParamService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingReceiveInfo;
import com.persee.workstation.uv_inexus.vo.concise.common.*;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricReportDataVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FSaveDataVO;
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
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemBusinessParamVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT;
import static com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum.QUANTITATIVE_MEASUREMENT;

@Slf4j
@Service("ConciseCommonService")
public class ConciseCommonServiceImpl implements ConciseCommonService {

    @Autowired
    private SpectrumConciseService spectrumConciseService;
    @Autowired
    private TimeSweepConciseService timeSweepConciseService;
    @Autowired
    private FMeasureMethodService fMeasureMethodService;
    @Autowired
    private SystemUserManageDAO userMapper;
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
    private RationConciseService rationConciseService;

    @Autowired
    private SamplePoolConfigService fSamplePoolConfigService;

    @Autowired
    private FMeasureTaskMapper fMeasureTaskMapper;

    @Autowired
    ConcisePublicUtilService concisePublicUtilService;

    @Autowired
    GuidePublicUtilService guidePublicUtilService;

    @Autowired
    LaTeXConverter latexConverter;

    @Autowired
    DeviceInfoService deviceInfoService;

    @Autowired
    SystemBusinessParamService systemBusinessParamService;

    @Autowired
    SystemBusinessParamService businessParamService;

    public DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("1")
    String deleteType;

    @Value("0")
    String createType;

    @Value("1")
    String noSave;

    @Value("0")
    String saved;

    @Value("0")
    String noTested;

    /**
     * 查询默认参数
     *
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
            //measureType+没有默认方法
            return Result.fail("00101001", measureType);
        }
        return Result.ok(one);

    }


    @Override
    public Result<FMethodListWithPageVO> searchMethodButton(FQueryMethodWithPageDto dto) {
        // 对传入的参数进行校验
        if (!StringUtils.hasText(dto.getUpdateBy())) {
            //用户不能为空
            return Result.fail("00101002");
        }
        if (!StringUtils.hasText(dto.getMeasureType())) {
            //方法类型不能为空
            return Result.fail("00100006");
        }

        if (dto.getPageNum() == null || dto.getPageNum() < 1) {
            dto.setPageNum(1);
        }

        if (dto.getPageSize() == null || dto.getPageSize() < 1) {
            dto.setPageSize(10);
        }
        // 设置查询条件
        QueryWrapper<TMeasureMethod> wrapper = new QueryWrapper<>();
//        wrapper.eq("method_creator", dto.getUpdateBy());
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
            //该类型暂未开发
            return Result.fail("00101003");
        }
        return Result.ok(ans);
    }


    /**
     * 查询保存状态
     *
     * @param taskId
     * @return
     */
    @Override
    public Result querySaveState(String taskId) {
        if (taskId == null) {
            //任务id不能为空
            return Result.fail("00000003");
        }
        SaveStateVo vo = new SaveStateVo();
        // 根据任务id查询任务
        QueryWrapper<FMeasureTask> wrapper = new QueryWrapper<>();
        wrapper.eq("id", taskId)
                .eq("is_delete", "0");
        FMeasureTask task = fMeasureTaskService.getOne(wrapper);
        if (task == null) {
            //草稿不存在
            return Result.fail("00101004");
        }
        // 根据任务中的方法key查询方法
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            //没有找到该方法
            return Result.fail("00000009");
        }
        vo.setTaskName(task.getTaskName());
        vo.setMethodName(method.getMethodName());
        // 根据任务id查询测量数据和分析数据
        List<FMeasureData> measureDataList = fMeasureDataService.list(new QueryWrapper<FMeasureData>().eq("task_id", taskId).eq("is_save", "1"));
        List<FMeasureAnalysisData> analysisDataList = fMeasureAnalysisDataService.list(new QueryWrapper<FMeasureAnalysisData>().eq("task_id", taskId).eq("is_save", "1"));
        if (!measureDataList.isEmpty() || !analysisDataList.isEmpty()) {
            vo.setSaveState(false);
            return Result.ok(vo);
        }
        vo.setSaveState(true);
        return Result.ok(vo);
    }

    /**
     * 保存草稿按钮
     *
     * @param taskId
     * @return
     */
    @Override
    public Result saveDraftButton(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            //请先创建任务
            return Result.fail("00101006");
        }
        // 根据任务id查询任务
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        // 根据任务id和is_save字段为1的条件查询
        QueryWrapper<FMeasureData> wrapper = new QueryWrapper<>();
//        wrapper.eq("task_id", taskId).eq("task_status",Constants.PARAM_TWO);
        wrapper.eq("task_id", taskId).eq("is_save", Constants.PARAM_ONE);
        List<FMeasureData> measureDataList = fMeasureDataService.list(wrapper);
        if (measureDataList != null) {
            for (FMeasureData measureData : measureDataList) {
                measureData.setIsSave("0");
                boolean update1 = fMeasureDataService.updateById(measureData);
                if (!(update1)) {
                    //保存草稿失败
                    return Result.fail("00100005");
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
                if (!(update)) {
                    //保存草稿失败
                    return Result.fail("00100005");
                }
            }
        }
        task.setEndSaveTime(LocalDateTime.now());
        boolean update = fMeasureTaskService.updateById(task);
        if (update) {
            return Result.ok();
        } else {
            //保存草稿失败
            return Result.fail("00100005");
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
            //没有找到该方法
            return Result.fail("00000009");
        }
        if (method.getMeasurementType().isEmpty()) {
            //方法类型不存在
            return Result.fail("00101005");
        }
        MeasureTypeEnum measureTypeEnum = MeasureTypeEnum.fromDescription(method.getMeasurementType());
        if (measureTypeEnum == null) {

        }
        switch (measureTypeEnum) {
            case PHOTOMETRIC_MEASUREMENT:
                return Result.ok();
            case QUANTITATIVE_MEASUREMENT:
                return Result.ok();
            case SPECTRAL_SCAN: {
                FSpectrumParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
                return Result.ok(parameter);
            }
            case TIME_SCAN: {
                FTimeSweepParamter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FTimeSweepParamter.class);
                return Result.ok(parameter);
            }
            default:
                //暂不支持
                return Result.fail("0000002B");
        }

    }


    //    @Override
//    public Result<Page<FMeasureTaskDTO>> queryHistoryTask(SelectTaskDto selectTaskDto) {
//
//        String userName = selectTaskDto.getUpdateBy();
//        SysUserPO sysUserPO = userMapper.selectByUsername(userName);
//        if (sysUserPO == null) {
//            //用户不存在
//            return Result.fail("00000001");
//        }
//        String userId = sysUserPO.getId();
//        selectTaskDto.setUpdateBy(userId);
//        return fMeasureTaskService.querybyName(selectTaskDto);
//    }
    @Override
    public Result<Page<FMeasureTaskDTO>> queryHistoryTask(SelectTaskDto selectTaskDto) {

        String userName = selectTaskDto.getUpdateBy();
        SysUserPO sysUserPO = userMapper.selectByUsername(userName);
        if (userName == null) {
            return Result.fail("00000001");
        }
        selectTaskDto.setUpdateBy(sysUserPO.getId());
        return fMeasureTaskService.queryByName(selectTaskDto);
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
            //参数不能为空
            return Result.fail("00000045");
        }
        FMeasureMethod method1 = fMeasureMethodService.getById(updateMethodDto.getMethodId());
        if (method1 == null) {
            //没有找到该方法
            return Result.fail("00000009");
        }
        LambdaQueryWrapper<FMeasureMethod> wrapper = new LambdaQueryWrapper<>();
        wrapper.notIn(FMeasureMethod::getId, updateMethodDto.getMethodId())
                .eq(FMeasureMethod::getMethodName, updateMethodDto.getMethodName())
                .like(FMeasureMethod::getMeasurementType, method1.getMeasurementType());
        long count = fMeasureMethodService.count(wrapper);
        if (count > 0) {
            //方法名称重复
            return Result.fail("00301002");
        }
        String methodId = updateMethodDto.getMethodId();
        LambdaQueryWrapper<FMeasureMethod> fMeasureMethodLambdaQueryWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<FMeasureMethod> eq = fMeasureMethodLambdaQueryWrapper.eq(FMeasureMethod::getId, methodId);
        FMeasureMethod method = fMeasureMethodService.getOne(eq);
        BeanUtils.copyProperties(updateMethodDto, method);
        fMeasureMethodService.updateById(method);
        //修改成功
        return Result.okCode("00101010");
    }

    @Override
    public Result updateTask(UpdateTaskDto updateTaskDto) {
        if (updateTaskDto == null) {
            //参数不能为空
            return Result.fail("00B06003");
        }
        SysUserPO po = userMapper.selectByUsername(updateTaskDto.getUpdateBy());
        LambdaQueryWrapper<FMeasureTask> wrapper = new LambdaQueryWrapper<FMeasureTask>()
                .eq(FMeasureTask::getTaskName, updateTaskDto.getTaskName())
                .notIn(FMeasureTask::getId, updateTaskDto.getTaskId())
                .eq(FMeasureTask::getIsDelete, "0");
        if (po != null) {
            wrapper.eq(FMeasureTask::getUserId, po.getId());
        }
        long count = fMeasureTaskService.count(wrapper);
        if (count > 0) {
            //任务名称重复
            return Result.fail("00301002");
        }
        String taskId = updateTaskDto.getTaskId();
        FMeasureTask byId = fMeasureTaskService.getById(taskId);
        if (byId == null) {
            //没有找到该任务
            return Result.fail("00000004");
        }
        byId.setTaskName(updateTaskDto.getTaskName());
        boolean update = fMeasureTaskService.updateById(byId);
        if (update) {
            //修改成功
            return Result.ok("00101010");
        } else {
            //修改失败
            return Result.fail("00101008");
        }

    }

    @Override
    public Result<Boolean> checkSampleInputParam(FCheckSampleInputParamDto dto) {

        FInputParam newInputParam = dto.getNewInputParam();
        if (newInputParam == null || !StringUtils.hasText(newInputParam.getResultIdentity())) {
            //请输入参数！
            return Result.fail("00101009");
        }

        if (newInputParam.getParamName().length() >= 256) {
            //名称格式错误
            return Result.fail("00000020");
        }

        String methodMark = newInputParam.getResultIdentity();
        if (!Constants.PARAM_PATTERN.matcher(methodMark).matches()) {
            //标识格式错误：必须以大写字母开头，后跟1-10位数字
            return Result.fail("0010100A");
        }

        if (!dto.getType()) {
            if (dto.getInputParamList() != null && !dto.getInputParamList().isEmpty()) {

                for (FInputParam param : dto.getInputParamList()) {
                    if (param.getResultIdentity().equals(newInputParam.getResultIdentity())) {
                        //标识已存在
                        return Result.fail("0010100B");
                    }
                }
            }
        }

        return Result.ok(true);
    }


    @Override
    public Result<Boolean> checkFormula(FCheckFormulaDto checkFormulaDto) {

        FPreCountResultParam newFormula = checkFormulaDto.getNewFormula();

        if (newFormula == null || !StringUtils.hasText(newFormula.getFormulaText())) {
            //请输入公式！
            return Result.fail("0010100C");
        }

        if (!StringUtils.hasText(newFormula.getResultName())) {
            //请输入显示标题!
            return Result.fail("00101017");
        }
        if (!StringUtils.hasText(newFormula.getResultIdentity())) {
            //请输入结果标识！
            return Result.fail("00101018");
        }
        if (!StringUtils.hasText(newFormula.getUnit())) {
            //请输入结果单位！
            return Result.fail("00101019");
        }
        List<FInputParam> inputParamList = checkFormulaDto.getInputParamList();
        if (inputParamList == null || inputParamList.isEmpty()) {
            //请先设置输入参数！
            return Result.fail("0010100D");
        }
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(checkFormulaDto.getInputParamList())) {
            if (checkFormulaDto.getNewFormula() != null) {
                for (FInputParam fInputParam : checkFormulaDto.getInputParamList()) {
                    if (fInputParam.getResultIdentity().equals(checkFormulaDto.getNewFormula().getResultIdentity())) {
                        //标识已存在
                        return Result.fail("0010100B");
                    }
                }

            }
        }

        List<String> paramList = new ArrayList<>();
        for (FInputParam param : inputParamList) {
            paramList.add(param.getResultIdentity());
        }
        // 定量测量时，系统配置的曲线参数也会参与公式计算，需要加入参数列表
        if (QUANTITATIVE_MEASUREMENT.name().equals(checkFormulaDto.getMeasureType())
                || QUANTITATIVE_MEASUREMENT.getDescription().equals(checkFormulaDto.getMeasureType())) {
            for (RationCurveParamEnums curveEnum : RationCurveParamEnums.values()) {
                paramList.add(curveEnum.getIdentity());
            }
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
     *
     * @param taskId
     * @param analysisId
     * @return
     */
    @Override
    public Result obtainReportParameters(String taskId, String analysisId) {

        if ((taskId == null)) {
            //任务id不能为空
            return Result.fail("00000003");
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail("00000004");
        }

        if (task.getMeasureType().equals(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription())) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            SpectralReportVo vo = new SpectralReportVo();
//            TMeasureProject project =fMeasureProjectService.getById(task.getProjectItemKey());
            SysUserPO user = userMapper.selectById(task.getUserId());
            FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
            FPhotometricParameter methodparam = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);
            vo.setTaskName(task.getTaskName());

            if (StringUtils.hasText(task.getSampleKey())) {
                List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
                if (!taskSpecimen.isEmpty()) {
                    FTaskSpecimen specimen = taskSpecimen.get(0);
                    FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());
                    vo.setSampleName(sample.getSampleName());
                    vo.setSampleNumber(sample.getSampleNumber());
                    vo.setSamplingTime(task.getCreateTime().format(formatter));

                    LambdaQueryWrapper<FMeasureData> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(FMeasureData::getTaskId, taskId)
                            .eq(FMeasureData::getSampleKey, sample.getId())
                            .orderByDesc(FMeasureData::getSerialNumber)
                            .last("LIMIT 1");
                    FMeasureData one = fMeasureDataService.getOne(wrapper);
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
                    wave.append(s).append("nm").append(",");
                }
            }
            if (wave.length() > 0) {
                wave.deleteCharAt(wave.length() - 1);
            }
            vo.setInstrumentWavelength(wave.toString());

            vo.setReferenceMethod(method.getMethodName());
            vo.setReferenceStandard(methodparam.getReferenceStandardName());

            if (StringUtils.hasText(task.getDeviceSerialNumber())) {
                vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            }
            String name = deviceInfoService.queryDeviceName(task.getDeviceSerialNumber());
            String modelName = deviceInfoService.queryDeviceModelName(task.getDeviceSerialNumber());

            vo.setInstrumentName("紫外可见分光光度计");
            vo.setInstrumentModel("T600");

            if (StringUtils.hasText(name)) {
                vo.setInstrumentName(name);
            }
            if (StringUtils.hasText(modelName)) {
                vo.setInstrumentModel(modelName);
            }


            vo.setLuminosityMode(WaveLengthEnum.getName(methodparam.getLuminosity()));
            vo.setSpectrumBandwidth(methodparam.getSpectralBandwidth()+"nm");


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
            if (task.getEndSaveTime() != null) {
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
            FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
            FRationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FRationParameter.class);

            vo.setRepetitionPattern(parameter.getRepeatMode());
            vo.setEquationType(RationCurveParamEnums.getName(parameter.getEquationType()));
            if (parameter.getCoefficientMode().equals(RationCurveParamEnums.standard_curve.getIdentity())) {
                vo.setForceZeroPoint("否");
                vo.setInsertZeroPoint("否");
                if (parameter.getZeroMethod() != null && !parameter.getZeroMethod().isEmpty()) {
                    for (String s : parameter.getZeroMethod()) {
                        if (s.equals(RationCurveParamEnums.force_zero.getIdentity())) {
                            vo.setForceZeroPoint("是");
                        }
                        if (s.equals(RationCurveParamEnums.insert_zero.getIdentity())) {
                            vo.setInsertZeroPoint("是");
                        }
                    }
                }
            }

            vo.setAutoChangeSamplePool("否");

            R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(task.getDeviceSerialNumber());
            if (poolInfo.getSuccess()) {
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

                    if (specimen.getIsTested().equals("0")) {
                        continue;
                    }
                    FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());
                    if (!specimen.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                        vo.setStandardConcentration(sample.getNormalConcentration());
                    }

                    vo.setSampleName(sample.getSampleName());
                    vo.setSampleNumber(sample.getSampleNumber());
                    LambdaQueryWrapper<FMeasureData> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(FMeasureData::getTaskId, taskId)
                            .eq(FMeasureData::getSampleKey, sample.getId())
                            .orderByDesc(FMeasureData::getSerialNumber)
                            .last("LIMIT 1");
                    FMeasureData measureData = fMeasureDataService.getOne(wrapper);
                    vo.setSamplingTime(measureData.getSamplingTime());
                    if (measureData.getMeasurementStartTime() != null) {
                        vo.setDetectionStartTime(measureData.getMeasurementStartTime().format(formatter));
                    }
                    if (measureData.getMeasurementEndTime() != null) {
                        vo.setDetectionEndTime(measureData.getMeasurementEndTime().format(formatter));
                    }
                }
            }

            if (StringUtils.hasText(task.getFittingParam())) {
                FittingReceiveInfo fittingReceiveInfo = JSON.parseObject(task.getFittingParam(), FittingReceiveInfo.class);
                StringBuilder param = new StringBuilder();
                if (fittingReceiveInfo != null) {

                    if (fittingReceiveInfo.getRoundCoefficient() != null) {
                        for (Double coefficient : fittingReceiveInfo.getRoundCoefficient()) {
                            param.append(coefficient).append(",");
                        }
                        param.deleteCharAt(param.length() - 1);
                        vo.setCurveParams(param.toString());
                    }

                    SystemBusinessParamVO systemBusinessParamVO = systemBusinessParamService.queryBusinessParam();
                    String curveConf = systemBusinessParamVO.getStandardCurveConf();
                    String[] split = curveConf.split(",");
                    for (int i = 0; i < split.length; i++) {
                        if (split[i].equals("1")) {
                            vo.setR(fittingReceiveInfo.getRoundR() == null ? "" : fittingReceiveInfo.getRoundR());
                        }
                        if (split[i].equals("2")) {
                            vo.setR2(fittingReceiveInfo.getRoundR2() == null ? "" : fittingReceiveInfo.getRoundR2());
                        }
                    }
                    vo.setCurveFormula(fittingReceiveInfo.getEquation() == null ? "" : fittingReceiveInfo.getEquation());
                    vo.setCurveType(String.valueOf(fittingReceiveInfo.getDegree() == null ? "" : fittingReceiveInfo.getDegree()));

                }
            }

            StringBuilder wave = new StringBuilder();
            if (parameter.getWaveLength() != null && !parameter.getWaveLength().isEmpty()) {
                for (String s : parameter.getWaveLength()) {
                    wave.append(s).append("nm").append(",");
                }
            }
            if (wave.length() > 0) {
                wave.deleteCharAt(wave.length() - 1);
            }
            vo.setInstrumentWavelength(wave.toString());

            vo.setReferenceMethod(method.getMethodName());
            vo.setReferenceStandard(parameter.getReferenceStandardName());

            vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            vo.setSpectrumBandwidth(parameter.getSpectralBandwidth()+"nm");

            if (StringUtils.hasText(task.getDeviceSerialNumber())) {
                vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            }
            String name = deviceInfoService.queryDeviceName(task.getDeviceSerialNumber());
            String modelName = deviceInfoService.queryDeviceModelName(task.getDeviceSerialNumber());

            vo.setInstrumentName("紫外可见分光光度计");
            vo.setInstrumentModel("T600");

            if (StringUtils.hasText(name)) {
                vo.setInstrumentName(name);
            }
            if (StringUtils.hasText(modelName)) {
                vo.setInstrumentModel(modelName);
            }


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
            if (task.getEndSaveTime() != null) {
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
            FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
            if (method == null) {
                //没有找到该方法
                return Result.fail("00000009");
            }
            List<String> analysisIdList = new ArrayList<>(Arrays.asList(analysisId.split(",")));
            FMeasureData data = null;
            if (analysisIdList.size() == 1) {
                // 根据分析iD查询分析数据
                FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getById(analysisIdList.get(0));
                if (analysisData == null) {
                    //没有找到该分析数据
                    return Result.fail("0000003D");
                }
                if (analysisData.getMeasureDataId() != null)
                    data = fMeasureDataService.getById(analysisData.getMeasureDataId());
            }
            SpectralReportVo vo = new SpectralReportVo();
            // 将方法参数提取出来拷贝到report中
            SpectrumParameter param = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
            vo.setStartWaveLength(param.getStartWaveLength() + "nm");
            vo.setEndWaveLength(param.getEndWaveLength() + "nm");
            DeviceConfParam scanSpeedByValue = MappingParamEnum.SCAN_SPEED.getScanSpeedByValue(param.getScanSpeed());
            if (scanSpeedByValue != null) {
                vo.setScanSpeed(scanSpeedByValue.getParamName());
            }
            if (param.getRepetitionPattern().equals("单池重复")) {
                vo.setTimeInterval(param.getTimeInterval());
                vo.setIsRepeat(true);
                vo.setRepeatCount(param.getRepetitionTime());
            } else {
                vo.setTimeInterval(null);
                vo.setIsRepeat(false);
                vo.setRepeatCount(null);
            }
            vo.setStairCorrection(param.getLadderCorrection() == true ? "启用" : "不启用");
            vo.setRepetitionPattern(param.getRepetitionPattern());
            if (data != null) {
                vo.setDetectionStartTime(data.getMeasurementStartTime().format(formatter));
                vo.setDetectionEndTime(data.getMeasurementEndTime().format(formatter));
            }
            if (param.getRepetitionPattern().equals("1")) {
                vo.setRepetitionTime(param.getRepetitionTime());
            }
            vo.setWaveLengthInterval(param.getWaveLengthInterval()+"nm");
            ///  仪器
            vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            // 将光度模式转换为(1.abs 2.T% 3.Es 4.Er)用equals判断,首字母大写
            vo.setLuminosityMode(param.getLuminosity().equals("1") ? "Abs" : param.getLuminosity().equals("2") ? "T%" : param.getLuminosity().equals("3") ? "Es" : param.getLuminosity().equals("4") ? "Er" : "");
            vo.setResponseTime(param.getResponseTime());
            vo.setSpectrumBandwidth(param.getSpectralBandwidth()+"nm");
            vo.setChangeLampWaveLength(param.getChangeLampWaveLength()+"nm");
            // 能量灯(0.钨灯 1.氘灯 2.汞灯)
            if (param.getLuminosity().equals("3") || param.getLuminosity().equals("4")) {
                vo.setEnergyLight(param.getEnergyLamp().equals("0") ? "钨灯" : param.getEnergyLamp().equals("1") ? "氘灯" : "汞灯");
                vo.setGain(param.getGain());
            }
            vo.setDetector(method.getMethodCreator());
            vo.setMethodName(method.getMethodName());
            vo.setTaskName(task.getTaskName());

            if (StringUtils.hasText(task.getDeviceSerialNumber())) {
                vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            }
            String name = deviceInfoService.queryDeviceName(task.getDeviceSerialNumber());
            String modelName = deviceInfoService.queryDeviceModelName(task.getDeviceSerialNumber());

            vo.setInstrumentName("紫外可见分光光度计");
            vo.setInstrumentModel("T600");

            if (StringUtils.hasText(name)) {
                vo.setInstrumentName(name);
            }
            if (StringUtils.hasText(modelName)) {
                vo.setInstrumentModel(modelName);
            }

            log.info("报表参数数据输出完毕,内容: {} ", vo);
            return Result.ok(vo);
        } else if (task.getMeasureType().equals(MeasureTypeEnum.TIME_SCAN.getDescription())) {
            // 根据任务中的方法key查询方法
            FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
            if (method == null) {
                //没有找到该方法
                return Result.fail("0000004D");
            }
            List<String> analysisIdList = new ArrayList<>(Arrays.asList(analysisId.split(",")));
            FMeasureData data = null;
            if (analysisIdList.size() == 1) {
                // 根据分析iD查询分析数据
                FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getById(analysisIdList.get(0));
                if (analysisData == null) {
                    //没有找到该分析数据
                    return Result.fail("0000003D");
                }
                if (analysisData.getMeasureDataId() != null)
                    data = fMeasureDataService.getById(analysisData.getMeasureDataId());
            }

            SpectralReportVo vo = new SpectralReportVo();
            // 将方法参数提取出来拷贝到report中
            FTimeSweepParamter param = JSON.parseObject(method.getMeasurementParamInfo(), FTimeSweepParamter.class);
            vo.setInstrumentWavelength(param.getWaveLength() + "nm");
            vo.setScanTime(param.getScanTime());
            vo.setTimeUnit(param.getScanUnit().equals("0") ? "分" : "秒");
            vo.setTimeInterval(param.getIntervalTime());
            vo.setTotalNumber(param.getTotalNumber());
            vo.setMultiPoolMode(param.getMultiPoolMode().equals("0") ? "顺序单池连续采样" : "循环多池交替采样");
            ///  仪器
            vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            if (data != null) {
                vo.setDetectionStartTime(data.getMeasurementStartTime().format(formatter));
                vo.setDetectionEndTime(data.getMeasurementEndTime().format(formatter));
            }
            // 将光度模式转换为(1.abs 2.T% 3.Es 4.Er)用equals判断,首字母大写
            vo.setLuminosityMode(param.getLuminosity().equals("1") ? "Abs" : param.getLuminosity().equals("2") ? "T%" : param.getLuminosity().equals("3") ? "Es" : param.getLuminosity().equals("4") ? "Er" : "");
            vo.setResponseTime(param.getResponseTime());
            vo.setSpectrumBandwidth(param.getSpectralBandwidth()+"nm");
            vo.setChangeLampWaveLength(param.getChangeLampWaveLength()+"nm");
            // 能量灯(0.钨灯 1.氘灯 2.汞灯)
            if (param.getLuminosity().equals("3") || param.getLuminosity().equals("4")) {
                vo.setEnergyLight(param.getEnergyLamp().equals("0") ? "钨灯" : param.getEnergyLamp().equals("1") ? "氘灯" : "汞灯");
                vo.setGain(param.getGain());
            }
//            vo.setProject(project.getProjectName());
            vo.setDetector(method.getMethodCreator());
            vo.setMethodName(method.getMethodName());
            vo.setTaskName(task.getTaskName());

            // 仪器型号
            if (StringUtils.hasText(task.getDeviceSerialNumber())) {
                vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            }
            String name = deviceInfoService.queryDeviceName(task.getDeviceSerialNumber());
            String modelName = deviceInfoService.queryDeviceModelName(task.getDeviceSerialNumber());

            vo.setInstrumentName("紫外可见分光光度计");
            vo.setInstrumentModel("T600");

            if (StringUtils.hasText(name)) {
                vo.setInstrumentName(name);
            }
            if (StringUtils.hasText(modelName)) {
                vo.setInstrumentModel(modelName);
            }


            return Result.ok(vo);
        } else {
            return Result.ok();
        }
    }

    /**
     * 查询报告参数
     *
     * @param taskId
     * @param sampleId
     * @param serialNumber
     * @return
     */
    @Override
    public Result queryReportAllParameters(String taskId, String sampleId, String serialNumber) {
        // 校验任务id
        if (StringUtils.isEmpty(taskId)) {
            //请选择任务
            return Result.fail("00000006");
        }
        // 校验样品id
        if (StringUtils.isEmpty(sampleId)) {
            //请选择样品
            return Result.fail("0000000A");
        }
        // 根据任务id查询任务
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail("00000004");
        }
        // 根据任务中的所属项目的Key查询项目
//        FMeasureProject project =fMeasureProjectService.getById(task.getProjectItemKey());
//        if (project == null) {
//            return Result.fail("项目不存在");
//        }
        // 根据任务中的方法key查询方法
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            //没有找到该方法
            return Result.fail("00000009");
        }
        // 根据样品id查询样品数据
        FMeasureSample sample = fMeasureSampleService.getById(sampleId);
        if (sample == null) {
            //样品不存在
            return Result.fail("0000000B");
        }
        // 编写查询测量的条件
        QueryWrapper<FMeasureData> wrapper = new QueryWrapper<>();
        wrapper.eq("task_key", taskId);
        wrapper.eq("sample_key", sampleId);
        // 如果存在编号，则查询该设备编号的测量数据
        if (StringUtils.hasText(serialNumber)) {
            wrapper.eq("serial_number", serialNumber);
        }
        wrapper.last("limit 1");
        FMeasureData measureData = fMeasureDataService.getOne(wrapper);
        if (measureData == null) {
            //未找到该测量数据
            return Result.fail("00301013");
        }
        // 编写分析的查询条件
        QueryWrapper<FMeasureAnalysisData> analysisWrapper = new QueryWrapper<>();
        analysisWrapper.eq("task_id", taskId);
        analysisWrapper.eq("sample_key", sampleId);
        if (StringUtils.hasText(serialNumber)) {
            analysisWrapper.eq("analysis_number", serialNumber);
        }
        analysisWrapper.last("limit 1");
        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(analysisWrapper);
        if (analysisData == null) {
            //没有找到该分析数据
            return Result.fail("00301013");
        }
        // 根据分析id查询分析图片数据
        QueryWrapper<FMeasureAnalysisImage> attachmentWrapper = new QueryWrapper<>();
        attachmentWrapper.eq("analysis_id", analysisData.getId());
        attachmentWrapper.last("limit 1");
        FMeasureAnalysisImage attachment = fMeasureAnalysisImageService.getOne(attachmentWrapper);
        if (attachment == null) {
            //没有找到对应的图片数据
            return Result.fail("0000000C");
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
                report.setScanSpeed(param.getScanSpeed().equals("0") ? "慢速" : param.getScanSpeed().equals("1") ? "中速" : param.getScanSpeed().equals("2") ? "快速" : "极快");
                // 将阶梯矫正的参数转换为(true.启用 false.不启用)用equals判断
                report.setLadderCorrection(param.getLadderCorrection().equals(true) ? "启用" : "不启用");
                // 将重复模式转换为(0.不重复 1.单池重复)用equals判断
                report.setRepetitionPattern(param.getRepetitionPattern().equals("0") ? "不重复" : "单池重复");
                report.setDetectionStartTime(measureData.getMeasurementStartTime().format(formatter));
                report.setDetectionEndTime(measureData.getMeasurementEndTime().format(formatter));
                ///  仪器
                report.setDeviceSerialNumber(task.getDeviceSerialNumber());
                // 将光度模式转换为(1.abs 2.T% 3.Es 4.Er)用equals判断,首字母大写
                report.setLuminosityMode(param.getLuminosity().equals("1") ? "Abs" : param.getLuminosity().equals("2") ? "T%" : param.getLuminosity().equals("3") ? "Es" : "Er");
                // 将能量灯转换为(0.钨灯 1.氘灯 2.汞灯)
                report.setEnergyLamp(param.getEnergyLamp().equals("0") ? "钨灯" : param.getEnergyLamp().equals("1") ? "氘灯" : "汞灯");

                /// 其他
//                report.setProjectName(project.getProjectName());
                report.setUserBy(method.getMethodCreator());
                if (serialNumber != null) {
                    report.setSampleName(sample.getSampleName() + "-" + serialNumber);
                } else {
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
                report.setLuminosityMode(param.getLuminosity().equals("1") ? "Abs" : param.getLuminosity().equals("2") ? "T%" : param.getLuminosity().equals("3") ? "Es" : "Er");
                // 将能量灯转换为(0.钨灯 1.氘灯 2.汞灯)
                report.setEnergyLamp(param.getEnergyLamp().equals("0") ? "钨灯" : param.getEnergyLamp().equals("1") ? "氘灯" : "汞灯");

                /// 其他
//                report.setProjectName(project.getProjectName());
                report.setUserBy(method.getMethodCreator());
                if (serialNumber != null) {
                    report.setSampleName(sample.getSampleName() + "-" + serialNumber);
                } else {
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
                //未知的测量类型
                return Result.fail("0000000D");
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
        }
    }


    /**
     * 新建报告表数据查询
     *
     * @param taskId
     * @param analysisId
     * @param allDisplay
     * @param decimalDigits
     * @return
     */
    @Override
    public Result newReportTableDataQuery(String taskId, String analysisId, Boolean allDisplay, Integer decimalDigits) {

        if (!StringUtils.hasText(taskId)) {
            //请选择任务
            return Result.fail("00000006");
        }
        if (decimalDigits == null) {
            decimalDigits = Constants.DEFAULT_DECIMAL_NUM;
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail("00000004");
        }
        // 根据任务id查询方法,再从json转换为对象方法参数
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            //没有找到该方法
            return Result.fail("00000009");
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
                    vos.removeIf(head -> head.getIsHidden() != null && head.getIsHidden());
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
                        if (key.equals("Abs")) {
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
            case "定量测定": {
                FRationReportDataVO vo = new FRationReportDataVO();
                Result<List<FHeadVO>> standHeads = rationConciseService.getStandSampleTableHead(taskId, task.getMethodKey(), allDisplay, null);
                if (standHeads.getCode() == 200) {
                    List<FHeadVO> standData = standHeads.getData();
                    standData.removeIf(head -> head.getIsHidden() != null && head.getIsHidden());
                    vo.setStandHeadList(standData);
                }
                Result<List<FHeadVO>> unknowHeads = rationConciseService.getUnknownSampleTableHead(taskId, task.getMethodKey(), allDisplay, null);
                if (unknowHeads.getCode() == 200) {
                    List<FHeadVO> unknowData = unknowHeads.getData();
                    unknowData.removeIf(head -> head.getIsHidden() != null && head.getIsHidden());
                    vo.setUnknowHeadList(unknowData);
                }
                if (dataDTO == null) {
                    dataDTO = new FQueryDataDTO();
                    dataDTO.setTaskId(taskId);
                    dataDTO.setAllDisplay(false);
                    dataDTO.setDigit(digit);
                    dataDTO.setOption(new ArrayList<>());
                }
                Result<FRationSampleDataVO> data = rationConciseService.querySampleData(dataDTO);
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
                Result<FRationFitCurveVO> curveInfo = rationConciseService.queryCurveInfo(taskId, task.getMethodKey());
                if (curveInfo.getCode() == 200) {
                    vo.setCurveInfo(curveInfo.getData());
                }
                List<Map<String, Object>> dataList1 = vo.getStandDataList();
                for (Map<String, Object> stringObjectMap : dataList1) {
                    for (String key : stringObjectMap.keySet()) {
                        if (key.equals("Abs")) {
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
            case "光谱扫描": {

//                if (analysisId.isEmpty()){
//                    return Result.ok(new FSpectrumReportDataVO());
//                }

                // 判断方法参数中的重复模式为true的话就分割分析id
                List<String> analysisIdList = Arrays.asList(analysisId.split(","));
                // 过滤掉分析结果为null的分析id
                analysisIdList = analysisIdList.stream()
                        .filter(id -> {
                            // 根据分析ID查询分析数据结果
                            String analysisResult = getAnalysisResultById(id);
                            return analysisResult != null; // 只保留分析结果不为null的分析ID
                        })
                        .collect(Collectors.toList());

                // 获取数据
                FSpectrumReportDataVO vo = new FSpectrumReportDataVO();
                Result<List<FHeadVO>> result = spectrumConciseService.tableHead(taskId, task.getMethodKey(), analysisIdList);
                if (result.getCode() == 200) {
                    List<FHeadVO> vos = result.getData();
                    vo.setHeadList(vos);
                }

                // 获取数据
                List<Map<String, Object>> dataList = spectrumConciseService.queryTableData(taskId, analysisIdList, decimalDigits);
                if (dataList != null) {
                    vo.setDataList(dataList);
                } else {
                    vo.setDataList(new ArrayList<>());
                }

                if (StringUtils.hasText(task.getTableInfo())) {
                    FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
                    if (snapshot != null && snapshot.getMapRelation() != null) {
                        vo.setMapRelation(snapshot.getMapRelation());
                    }
                }
                if (vo.getDataList() != null) {
                    List<Map<String, Object>> dataList1 = vo.getDataList();
                    for (Map<String, Object> stringObjectMap : dataList1) {
                        for (String key : stringObjectMap.keySet()) {
                            if (key.equals("Abs")) {
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
                }
                log.info("报表表格数据输出完毕,内容: {} ", vo);
                return Result.ok(vo);
            }
            case "时间扫描": {


                FTimeSweepReportDataVo vo = new FTimeSweepReportDataVo();
                // 判断方法参数中的重复模式为true的话就分割分析id
                List<String> analysisIdList = Arrays.asList(analysisId.split(","));
                // 过滤掉分析结果为null的分析id
                analysisIdList = analysisIdList.stream()
                        .filter(id -> {
                            // 根据分析ID查询分析数据结果
                            String analysisResult = getAnalysisResultById(id);
                            return analysisResult != null; // 只保留分析结果不为null的分析ID
                        })
                        .collect(Collectors.toList());

                // 获取表头
                Result<List<FHeadVO>> result = timeSweepConciseService.tableHead(taskId, task.getMethodKey(), analysisIdList);
                if (result.getCode() == 200) {
                    List<FHeadVO> vos = result.getData();
                    vo.setHeadList(vos);
                }

                // 获取数据
                List<Map<String, Object>> dataList = timeSweepConciseService.queryTableData(taskId, analysisIdList, decimalDigits);
                if (dataList != null) {
                    vo.setDataList(dataList);
                } else {
                    vo.setDataList(new ArrayList<>());
                }

                // 获取额外的映射关系
                if (StringUtils.hasText(task.getTableInfo())) {
                    FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
                    if (snapshot != null && snapshot.getMapRelation() != null) {
                        vo.setMapRelation(snapshot.getMapRelation());
                    }
                }

                if (vo.getDataList() != null) {
                    // 格式化数据中的 "Abs" 值
                    List<Map<String, Object>> dataList1 = vo.getDataList();
                    for (Map<String, Object> stringObjectMap : dataList1) {
                        for (String key : stringObjectMap.keySet()) {
                            if (key.equals("Abs")) {
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
                }

                return Result.ok(vo);
            }

            default:
                //类型错误,请重新选择
                return Result.fail("00000005");
        }
    }

    /**
     * 是否添加样本
     *
     * @param dto
     * @return
     */
    @Override
    public Result isAddSample(AddSampleDTO dto) {
        String taskId = dto.getTaskId();
        String deviceSerialNumber = dto.getDeviceSerialNumber();
        String sampleKind = dto.getSampleKind();
        String normalConcentration = dto.getNormalConcentration();
        String measureType = dto.getMeasureType();

        if (deviceSerialNumber==null||deviceSerialNumber.equals("")){
            return Result.fail("00305007");
        }
        if (taskId == null || taskId.equals("")) {
            return Result.fail("00000006");
        }

        // 1. 获取任务信息
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("00000004");
        }

        // 2. 如果未传测量类型，使用任务中的测量类型
        if (measureType == null) {
            measureType = task.getMeasureType();
        }

        // 3. 判断是否是定量测定
        boolean isQuantitative = QUANTITATIVE_MEASUREMENT.equals(measureType);

        // 4. 如果是定量测定标准样品，校验浓度
        if (isQuantitative && SampleTypeEnum.kind_stand.getIdentity().equals(sampleKind)) {
            if (!StringUtils.hasText(normalConcentration)) {
                return Result.fail("00102024");
            }
            try {
                double v = Double.parseDouble(normalConcentration);
                if (v < 0) {
                    return Result.fail("0010202D");
                }
            } catch (Exception e) {
                return Result.fail("0010202D");
            }
        }

        // 5. 获取测量类型描述
        String description = getMeasureTypeDescription(measureType);

        // 6. 准备配置
        SampleNumConfig numberConfig = prepareNumberConfig(dto.getSampleNumConfig(), isQuantitative);
        SampleNameConfig nameConfig = prepareNameConfig(dto.getSampleNameConfig(), isQuantitative);

        // 7. 根据测量类型选择不同的处理逻辑
        if (isQuantitative) {
            return handleQuantitativeSample(dto, task, numberConfig, nameConfig);
        } else {
            return handleFourMeasurementSample(dto, task, description, numberConfig, nameConfig);
        }
    }

    /**
     * 查询样品表头
     *
     * @param taskId
     * @param measureType
     * @return
     */
    public Result<List<FHeadVO>> querySampleHeads(String taskId, String measureType) {

        if (!StringUtils.hasText(taskId)) {
            // 如果任务ID为空，直接返回预定义的表头
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(measureType, null);
            return Result.ok(preHead);
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail("00000004");
        }

        if (!StringUtils.hasText(task.getTableInfo())) {
            // 如果任务的表头信息为空，返回预定义的表头
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(measureType, null);
            return Result.ok(preHead);
        } else {
            // 如果任务的表头信息不为空，解析任务中的表头数据
            FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
            if (snapshot.getSampleHeads() != null && !snapshot.getSampleHeads().isEmpty()) {
                return Result.ok(snapshot.getSampleHeads());
            } else {
                // 如果解析的样品表头为空，返回预定义的表头
                List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(measureType, null);
                return Result.ok(preHead);
            }
        }
    }

    /**
     * 根据现有样品查找下一个可用的样品编号
     */
    private int findNextSampleNumber(List<FMeasureSample> samples) {
        // 获取所有样品编号（假设编号的格式为：默认样品1、默认样品2、默认样品N）
        List<Integer> sampleNumbers = samples.stream()
                .filter(sample -> sample.getSampleName().startsWith("样品"))
                .map(sample -> Integer.parseInt(sample.getSampleName().replace("样品", "")))  // 获取样品编号部分
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
        sample.setSampleName("样品" + newSampleNumber);  // 使用新样品编号创建样品名称
        sample.setSampleNumber(String.valueOf(newSampleNumber));  // 设置新样品编号

        // 设置样品池位置
        LambdaQueryWrapper<SamplePoolConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        SamplePoolConfigPO configPO = fSamplePoolConfigService.getOne(wrapper);
        if (configPO == null) {
            //未找到样品池信息
            return Result.fail("00101011");
        }

        if (configPO.getIsAutoChangePool()) {
            sample.setSampleLocation(configPO.getCurrentPoolNum().toString());
        }

        // 保存新样品
        boolean save = fMeasureSampleService.save(sample);
        if (!save) {
            //创建样品失败
            return Result.fail("00101012");
        }

        // 将新样品加入任务样品列表
        FTaskSpecimen fTaskSpecimen = new FTaskSpecimen();
        fTaskSpecimen.setSpecimenKey(sample.getId());
        fTaskSpecimen.setDeleteType("0");
        fTaskSpecimen.setIsTested("0");

        List<FTaskSpecimen> sampleData = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);

        if (sampleData == null) {
            sampleData = new ArrayList<>();
            // 添加新样品到任务
            sampleData.add(fTaskSpecimen);
        } else {
            // 添加新样品到任务
            sampleData.add(fTaskSpecimen);
        }
        task.setSampleKey(JSON.toJSONString(sampleData));

        // 更新任务
        boolean update = fMeasureTaskService.updateById(task);
        if (!update) {
            //更新任务失败
            return Result.fail("0010100F");
        }

        return Result.ok(true);
    }

    @Override
    public Result<DeviceMeasureVO> queryDeviceTypeInfo(String deviceSerialNumber) {

        R<DeviceMeasureVO> re = deviceInfoService.queryDeviceTypeInfo(deviceSerialNumber);
        if (!re.getSuccess()) {
            return Result.fail(re.getMessage());
        }

        DeviceMeasureVO vo = re.getData();

        // 排序
        if (vo != null && vo.getScanSpeed() != null) {
            vo.getScanSpeed().sort((o1, o2) -> {
                int v1 = Integer.parseInt(o1.getParamValue());
                int v2 = Integer.parseInt(o2.getParamValue());
                return Integer.compare(v1, v2);
            });
        }

        return Result.ok(re.getData());

    }



    @Override
    public Result delMethod(String methodId) {
        if (!StringUtils.hasText(methodId)) {
            //参数不能为空
            return Result.fail("00000045");
        }
        QueryWrapper<FMeasureTask> wrapper = new QueryWrapper<>();
        wrapper.eq("method_key", methodId);
        wrapper.eq("is_delete", "0");
        wrapper.last("LIMIT 1");
        FMeasureTask task = fMeasureTaskService.getOne(wrapper, false);
        if (!ObjectUtils.isEmpty(task)) {
            //存在与该历史方法相关联的历史文件，请删除关联历史文件之后进行方法删除！！！
            return Result.fail("0010100E" , "任务名称：" + task.getTaskName());
        }
        boolean b = fMeasureMethodService.removeById(methodId);
        if (!b) {
            //更新任务失败
            return Result.fail("0010100F");
        }
        return Result.ok(true);
    }

    @Override
    public Result<String> copy(CopyParam copyParam) {

        String taskName = copyParam.getTaskName();
        String taskId = copyParam.getTaskId();
        if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isBlank(taskId)) {
            //拷贝失败,传入taskId不能为空！！！
            return Result.fail("00101013");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task != null && task.getIsDelete().equals("1")) {
            //任务已删除
            return Result.fail("00300012");
        }
        rationConciseService.saveButton(copyParam.getTaskId(), copyParam.getUpdateBy());
        try {
            /*
             * 复制任务
             */

            LambdaQueryWrapper<FMeasureTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FMeasureTask::getTaskName, taskName)
                    .eq(FMeasureTask::getMeasureType, task.getMeasureType())
                    .eq(FMeasureTask::getIsDelete, "0")
                    .eq(FMeasureTask::getUserId, task.getUserId());
            if (fMeasureTaskService.count(wrapper) > 0) {
                //任务名称重复
                return Result.fail("0000004D");
            }
            if (ObjectUtils.isEmpty(task)) {
                //没有找到该任务
                return Result.fail("00000004");
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
                                    m.setIsSave("0");
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
                                            analysisData.setIsSave("0");
                                        });
                                        fMeasureAnalysisDataService.saveBatch(fMeasureAnalysisDataList);
                                    }
                                });
                                LambdaQueryWrapper<FMeasureAnalysisData> queryWrapper3 = new LambdaQueryWrapper<>();
                                queryWrapper3.eq(FMeasureAnalysisData::getTaskId, taskId)
                                        .notIn(FMeasureAnalysisData::getAnalysisType, "5");
                                List<FMeasureAnalysisData> list = fMeasureAnalysisDataService.list(queryWrapper3);
                                if (!CollectionUtils.isEmpty(list)) {
                                    list.forEach(analysisData -> {
                                        analysisData.setId(null);
                                        analysisData.setTaskId(task.getId());
                                        analysisData.setIsSave("0");
                                    });
                                    fMeasureAnalysisDataService.saveBatch(list);
                                }

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
            task.setCreateTime(LocalDateTime.now());
            fMeasureTaskService.save(task);
            //拷贝成功
            return Result.ok("00101014");

        } catch (Exception e) {
            //拷贝失败
            return Result.fail("00101015");
        }

    }

    @Override
    public Result saveDataInfo(FSaveDataVO vo) {
        if (!StringUtils.hasText(vo.getTaskId())) {
            return Result.fail("0030001A");
        }

        if (!StringUtils.hasText(vo.getSampleId())) {
            return Result.fail("0030001B");
        }

        if (vo.getData() == null || vo.getData().isEmpty()) {
            return Result.ok();
        }
        String taskId = vo.getTaskId();
        String sampleId = vo.getSampleId();

        LambdaQueryWrapper<FMeasureData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FMeasureData::getTaskId, taskId)
                .eq(FMeasureData::getSampleKey, sampleId)
                .orderByDesc(FMeasureData::getSerialNumber).last("limit 1");
        FMeasureData data = fMeasureDataService.getOne(wrapper);


        LambdaQueryWrapper<FMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                .eq(FMeasureAnalysisData::getSampleKey, sampleId);

        if (data != null) {
            queryWrapper.eq(FMeasureAnalysisData::getMeasureDataId, data.getId());
        }
        queryWrapper.orderByDesc(FMeasureAnalysisData::getDataVersion).last("limit 1");

        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(queryWrapper);
        if (analysisData != null) {
            Map<String, String> jsonObject;
            if (StringUtils.hasText(analysisData.getAnalysisParam())) {
                jsonObject = JSON.parseObject(analysisData.getAnalysisParam(), new TypeReference<Map<String, String>>() {
                });
                jsonObject.putAll(vo.getData());
            } else {
                jsonObject = vo.getData();
            }
            analysisData.setAnalysisParam(JSON.toJSONString(jsonObject));
            fMeasureAnalysisDataService.updateById(analysisData);
        } else {

            queryWrapper.clear();
            queryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                    .eq(FMeasureAnalysisData::getSampleKey, sampleId)
                    .orderByDesc(FMeasureAnalysisData::getDataVersion).last("limit 1");
            analysisData = fMeasureAnalysisDataService.getOne(queryWrapper);
            if (analysisData == null) {
                analysisData = new FMeasureAnalysisData();
                analysisData.setDataVersion(0);
            }
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setAnalysisParam(JSON.toJSONString(vo.getData()));
            analysisData.setIsDelete(createType);
            fMeasureAnalysisDataService.saveOrUpdate(analysisData);
        }

        return Result.ok(true);
    }


    /**
     * 根据分析ID获取分析结果
     *
     * @param analysisId
     * @return
     */
    private String getAnalysisResultById(String analysisId) {
        // 根据分析ID从数据库获取分析结果
        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getById(analysisId);
        if (analysisData == null) {
            return null;
        }
        if (analysisData.getAnalysisResult() == null || analysisData.getAnalysisResult().isEmpty()) {
            return null;
        }
        return analysisData.getAnalysisResult();
    }

    @Override
    public Result<Boolean> checkApproximateRange(FRoundingVO vo) {
        R<Boolean> r = concisePublicUtilService.checkApproximateRange(vo);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @Override
    public Result setUpParametersPort(String taskId, String deviceSerialNumber, String measureType) {
        // 校验参数
        if (!StringUtils.hasText(taskId)) {
            return Result.fail("0030001C");
        }
        if (!StringUtils.hasText(deviceSerialNumber)) {
            return Result.fail("0030001D");
        }
        if (!StringUtils.hasText(measureType)) {
            return Result.fail("0030001E");
        }
        // 根据任务id查询任务信息
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("00B08014");
        }
        String serialNumber = task.getDeviceSerialNumber();
        if (serialNumber!=null&&!task.getDeviceSerialNumber().equals(deviceSerialNumber)){
            return Result.fail("00B00004");
        }
        String methodId = task.getMethodKey();
        // 根据测量类型获取对应的测量服务
        MeasureTypeEnum measureTypeEnum = MeasureTypeEnum.fromDescription(measureType);
        switch (measureTypeEnum) {
            case PHOTOMETRIC_MEASUREMENT:
                return photometricService.setUpParametersPort(methodId, deviceSerialNumber);
            case QUANTITATIVE_MEASUREMENT:
                return rationConciseService.setUpParametersPort(methodId, deviceSerialNumber);
            case SPECTRAL_SCAN:
                return spectrumConciseService.setUpParametersPort(methodId, deviceSerialNumber);
            case TIME_SCAN:
                return timeSweepConciseService.setUpParametersPort(methodId, deviceSerialNumber);
            case TWO_COMPONENT_QUANTITATIVE_ANALYSIS:
                return Result.fail("00300020");
            default:
                return Result.fail("00300021");
        }
    }

    /**
     * 查询多个任务样品信息
     * @param multipTaskSample
     * @return
     */
    @Override
    public Result queryMultipleTaskSample(MultipTaskSample multipTaskSample) {
        // 校验参数
        if(multipTaskSample==null){
            return Result.fail("00300022");
        }
        if(multipTaskSample.getTaskIds()==null|| multipTaskSample.getTaskIds().isEmpty()){
            return Result.fail("00300023");
        }
        if(multipTaskSample.getMeasureType()==null){
            return Result.fail("0030001E");
        }
        if (multipTaskSample.getDigit()==null){
            multipTaskSample.setDigit(4);
        }
        List<Map<String,Object>> resultList = new ArrayList<>();
        // 根据测量类型获取对应的测量服务
        MeasureTypeEnum measureTypeEnum = MeasureTypeEnum.fromDescription(multipTaskSample.getMeasureType());
        for (String taskId : multipTaskSample.getTaskIds()) {
            FMeasureTask byId = fMeasureTaskService.getById(taskId);
            if (byId == null) {
                return Result.fail("00300015");
            }
            Map<String,Object> result = new HashMap<>();
            Result resultData = null;
            switch (measureTypeEnum) {
                case SPECTRAL_SCAN:
                    resultData = spectrumConciseService.queryMeasuredSampleDataList(taskId, null, multipTaskSample.getDigit(), multipTaskSample.getDeviceSerialNumber());
                    break;
                case TIME_SCAN:
                    resultData = timeSweepConciseService.queryMeasuredSampleDataList(taskId, null, multipTaskSample.getDigit(), multipTaskSample.getDeviceSerialNumber());
                    break;
                default:
                    return Result.fail("00300016");
            }
            result.put("title",byId.getTaskName());
            result.put("children",resultData.getData());
            resultList.add(result);
        }
        return Result.ok(resultList);
    }

    @PreDestroy //
    public void cleanDeletedData() {
        // 清理30天前被逻辑删除的数据
        LocalDateTime threshold = LocalDateTime.now().minusDays(2);
        fMeasureTaskService.remove(new QueryWrapper<FMeasureTask>()
                .eq("is_delete", "1")
                .lt("end_save_time", threshold));
    }

    /**
     * 获取测量类型描述
     */
    private String getMeasureTypeDescription(String measureType) {
        if ("光谱扫描".equals(measureType)) {
            return MeasureTypeEnum.SPECTRAL_SCAN.getDescription();
        } else if ("时间扫描".equals(measureType)) {
            return MeasureTypeEnum.TIME_SCAN.getDescription();
        } else if ("定量测定".equals(measureType)) {
            return MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription();
        } else if ("光度测量".equals(measureType)) {
            return PHOTOMETRIC_MEASUREMENT.getDescription();
        }
        return null;
    }

    /**
     * 准备样品编号配置
     */
    private SampleNumConfig prepareNumberConfig(SampleNumConfig config, boolean isQuantitative) {
        if (config != null) {
            // 确保step不为null
            if (config.getStep() == null) {
                config.setStep(1);
            }
            // 确保startNum不为null
            if (config.getStartNum() == null) {
                config.setStartNum(1);
            }
            // 确保preName不为空
            if (!StringUtils.hasText(config.getPreName())) {
                if (isQuantitative) {
                    config.setPreName("default-");
                } else {
                    config.setPreName("default");
                }
            }
            return config;
        }

        // 默认配置
        SampleNumConfig defaultConfig = new SampleNumConfig();
        if (isQuantitative) {
            defaultConfig.setPreName("default-");
        } else {
            defaultConfig.setPreName("default");
        }
        defaultConfig.setStartNum(1);
        defaultConfig.setStep(1);
        return defaultConfig;
    }


    /**
     * 准备样品名称配置
     */
    private SampleNameConfig prepareNameConfig(SampleNameConfig config, boolean isQuantitative) {
        if (config != null) {
            // 确保step不为null
            if (config.getStep() == null) {
                config.setStep(1);
            }
            // 确保startNum不为null
            if (config.getStartNum() == null) {
                config.setStartNum(1);
            }
            // 确保preName不为空
            if (!StringUtils.hasText(config.getPreName())) {
                config.setPreName("样品");
            }
            return config;
        }

        // 默认配置
        SampleNameConfig defaultConfig = new SampleNameConfig();
        defaultConfig.setPreName("样品");
        defaultConfig.setStartNum(1);
        defaultConfig.setStep(1);
        return defaultConfig;
    }

    /**
     * 处理定量测定样品创建
     */
    private Result handleQuantitativeSample(AddSampleDTO dto, FMeasureTask task,
                                            SampleNumConfig numberConfig, SampleNameConfig nameConfig) {
        String taskId = dto.getTaskId();
        String deviceSerialNumber = dto.getDeviceSerialNumber();
        String sampleKind = dto.getSampleKind();
        String normalConcentration = dto.getNormalConcentration();

        // 获取任务样品列表
        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        if (taskSpecimen == null) {
            taskSpecimen = new ArrayList<>();
        }

        // 获取表头信息
        Result<List<FHeadVO>> listResult = querySampleHeads(taskId, sampleKind);
        if (!listResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode())) {
            return Result.fail(listResult.getMessage());
        }

        Integer sampleAutoNum = task.getSampleAutoNum();
        if (sampleAutoNum == null) {
            sampleAutoNum = 0;
        }

        // 获取业务参数
        SystemBusinessParamVO businessParamVO = businessParamService.queryBusinessParam();
        Integer concentrationDigit = businessParamVO.getConcentrationDigits();

        // 获取动态表头
        List<FHeadVO> heads = listResult.getData();
        List<FHeadVO> dynamicHeads = new ArrayList<>();
        heads.forEach(head -> {
            if (head.getLocate().equals(TableHeadILocateEnums.sample.getIdentity())) {
                dynamicHeads.add(head);
            }
        });

        // 获取自动换池配置
        boolean isAutoChangePool = getAutoChangePoolConfig(dto, deviceSerialNumber);

        // 处理样品创建逻辑
        if (taskSpecimen.isEmpty()) {
            // 首次创建样品
            return createFirstQuantitativeSample(task, taskSpecimen, sampleKind, normalConcentration,
                    concentrationDigit, dynamicHeads, numberConfig, nameConfig,isAutoChangePool,deviceSerialNumber);
        } else {
            // 后续添加样品
            return createSubsequentQuantitativeSamples(dto, task, taskSpecimen, sampleKind, normalConcentration,
                    concentrationDigit, dynamicHeads, isAutoChangePool,
                    deviceSerialNumber, numberConfig, nameConfig, sampleAutoNum);
        }
    }

    /**
     * 处理四个测量样品创建
     */
    private Result handleFourMeasurementSample(AddSampleDTO dto, FMeasureTask task, String description,
                                               SampleNumConfig numberConfig, SampleNameConfig nameConfig) {
        String taskId = dto.getTaskId();
        String deviceSerialNumber = dto.getDeviceSerialNumber();

        Integer sampleAutoNum = task.getSampleAutoNum();
        if (sampleAutoNum == null) {
            sampleAutoNum = 0;
        }

        // 获取任务样品列表
        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        if (taskSpecimen == null) {
            taskSpecimen = new ArrayList<>();
        }

        // 获取表头信息
        Result<List<FHeadVO>> listResult = querySampleHeads(taskId, description);
        if (!listResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode())) {
            return Result.fail(listResult.getMessage());
        }

        List<FHeadVO> heads = listResult.getData();
        List<FHeadVO> dynamicHeads = new ArrayList<>();
        heads.forEach(head -> {
            if (head.getLocate().equals(TableHeadILocateEnums.sample.getIdentity())) {
                dynamicHeads.add(head);
            }
        });

        // 获取自动切换样品池配置
        boolean isAutoChangePool = getAutoChangePoolConfig(dto, deviceSerialNumber);

        // 获取当前样品池信息（用于分配池号）
        Integer currentPoolNum = null;
        if (isAutoChangePool) {
            R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
            if (poolInfo.getSuccess() && poolInfo.getData() != null) {
                currentPoolNum = poolInfo.getData().getCurrentPoolNum();
            }
        }

        if (taskSpecimen.isEmpty()) {
            // ===== 第一次创建：始终只创建一个样品 =====
            FMeasureSample newSample = new FMeasureSample();

            // 使用配置的开始号
            sampleAutoNum = numberConfig.getStartNum();

            // 生成样品编号：前缀-开始号
            newSample.setSampleNumber(numberConfig.getPreName() + "-" + sampleAutoNum);

            // 生成样品名称：使用配置的起始编号
            newSample.setSampleName(nameConfig.getPreName() + nameConfig.getStartNum());
            newSample.setSampleType(SampleTypeEnum.unKnown.getIdentity());

            // 如果开启了自动切换样品池，分配当前池号
            if (isAutoChangePool && currentPoolNum != null) {
                newSample.setSampleLocation(String.valueOf(currentPoolNum));
            }

            if (!dynamicHeads.isEmpty()) {
                newSample.setHeads(JSON.toJSONString(dynamicHeads));
            }

            boolean save = fMeasureSampleService.save(newSample);
            if (!save) {
                return Result.fail("00300008");
            }

            taskSpecimen.add(new FTaskSpecimen(newSample.getId(), createType, noTested,
                    newSample.getSampleType(), null));

            log.info("首次创建样品: {}, 池号: {}", newSample.getSampleNumber(), newSample.getSampleLocation());

        } else {
            // ===== 后续创建样品 =====

            // 检查是否有未测试的样品
            boolean hasNoTested = false;
            List<String> sampleNumList = new ArrayList<>();
            List<String> sampleNameList = new ArrayList<>();

            for (FTaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(deleteType)) {
                    continue;
                }

                if (specimen.getIsTested().equals(noTested)) {
                    hasNoTested = true;
                    break;
                }

                FMeasureSample measureSample = fMeasureSampleService.getById(specimen.getSpecimenKey());
                if (measureSample != null) {
                    if (StringUtils.hasText(measureSample.getSampleNumber())) {
                        sampleNumList.add(measureSample.getSampleNumber());
                    }
                    if (StringUtils.hasText(measureSample.getSampleName())) {
                        sampleNameList.add(measureSample.getSampleName());
                    }
                }
            }

            if (hasNoTested) {
                return Result.ok(true);
            }

            // ===== 判断要创建的样品数量 =====
            int samplesToCreate = 1;  // 默认创建1个

            List<Integer> poolNumbers = new ArrayList<>();  // 存储要分配的池号

            if (isAutoChangePool) {
                // 开启自动切换：获取所有可用池号
                List<Integer> usefulLocation = fSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
                if (!usefulLocation.isEmpty()) {
                    samplesToCreate = usefulLocation.size();  // 按可用池号数量创建
                    poolNumbers = usefulLocation;
                    log.info("自动切换样品池开启，将创建 {} 个样品，分配池号: {}", samplesToCreate, poolNumbers);
                }
            } else {
                // 未开启自动切换：只创建1个
                samplesToCreate = 1;
                log.info("未开启自动切换，创建1个样品");
            }

            // 计算当前的样品编号的最大值
            int currentMaxNumber = findMaxNumberFromSamples(sampleNumList, numberConfig.getPreName() + "-");
            if (currentMaxNumber < numberConfig.getStartNum()) {
                currentMaxNumber = numberConfig.getStartNum();
            }

            // 计算当前名称的最大序号
            int currentMaxNameNumber = findMaxNumberFromSamples(sampleNameList, nameConfig.getPreName());
            if (currentMaxNameNumber < nameConfig.getStartNum()) {
                currentMaxNameNumber = nameConfig.getStartNum();
            }

            // 批量创建样品
            List<FMeasureSample> newSamples = new ArrayList<>();
            int nextNumber = currentMaxNumber;
            int nextNameNumber = currentMaxNameNumber;

            for (int i = 0; i < samplesToCreate; i++) {
                // 计算下一个编号
                if (i == 0) {
                    nextNumber = currentMaxNumber + numberConfig.getStep();
                } else {
                    nextNumber += numberConfig.getStep();
                }

                // 检查编号是否已存在
                while (sampleNumList.contains(numberConfig.getPreName() + "-" + nextNumber)) {
                    nextNumber += numberConfig.getStep();
                }

                // 计算下一个名称序号
                if (i == 0) {
                    nextNameNumber = currentMaxNameNumber + nameConfig.getStep();
                } else {
                    nextNameNumber += nameConfig.getStep();
                }

                // 检查名称是否已存在
                while (sampleNameList.contains(nameConfig.getPreName() + nextNameNumber)) {
                    nextNameNumber += nameConfig.getStep();
                }

                FMeasureSample newSample = new FMeasureSample();
                newSample.setSampleNumber(numberConfig.getPreName() + "-" + nextNumber);
                newSample.setSampleName(nameConfig.getPreName() + nextNameNumber);
                newSample.setSampleType(SampleTypeEnum.unKnown.getIdentity());

                // 分配池号（如果开启了自动切换）
                if (isAutoChangePool && !poolNumbers.isEmpty()) {
                    newSample.setSampleLocation(String.valueOf(poolNumbers.get(i)));
                }

                if (!dynamicHeads.isEmpty()) {
                    newSample.setHeads(JSON.toJSONString(dynamicHeads));
                }

                newSamples.add(newSample);
                sampleNumList.add(newSample.getSampleNumber());
                sampleNameList.add(newSample.getSampleName());
            }

            // 批量保存样品
            boolean saveAll = fMeasureSampleService.saveBatch(newSamples);
            if (!saveAll) {
                return Result.fail("00300008");
            }

            // 添加到任务标本列表
            for (FMeasureSample sample : newSamples) {
                taskSpecimen.add(new FTaskSpecimen(sample.getId(), createType, noTested,
                        sample.getSampleType(), null));
            }

            // 更新自动编号（取最后一个样品的编号）
            sampleAutoNum = nextNumber;

            log.info("后续创建 {} 个样品成功", samplesToCreate);
        }

        // 更新任务
        task.setSampleKey(JSON.toJSONString(taskSpecimen));
        task.setSampleAutoNum(sampleAutoNum);
        boolean update = fMeasureTaskService.updateById(task);
        if (!update) {
            return Result.fail("00000048");
        }

        return Result.ok(true);
    }


    /**
     * 获取自动换池配置
     */
    private boolean getAutoChangePoolConfig(AddSampleDTO dto, String deviceSerialNumber) {
        // 如果请求中指定了自动换池，使用请求值
        if (dto.getAutoChangePool() != null) {
            return dto.getAutoChangePool();
        }

        // 否则从设备配置获取
        R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
        if (poolInfo.getSuccess()) {
            return poolInfo.getData().getIsAutoChangePool();
        }
        return false;
    }

    /**
     * 创建首个定量测定样品
     */
    private Result createFirstQuantitativeSample(FMeasureTask task, List<FTaskSpecimen> taskSpecimen,
                                                 String sampleKind, String normalConcentration,
                                                 Integer concentrationDigit, List<FHeadVO> dynamicHeads,
                                                 SampleNumConfig numberConfig, SampleNameConfig nameConfig,
                                                 Boolean isAutoChangePool,String deviceSerialNumber) {

        FMeasureSample newSample = new FMeasureSample();

        // 设置样品名称和编号（使用配置的起始值）
        newSample.setSampleName(nameConfig.getPreName() + nameConfig.getStartNum());
        newSample.setSampleNumber(numberConfig.getPreName() + numberConfig.getStartNum());

        // 设置样品类型
        if (SampleTypeEnum.kind_unknown.getIdentity().equals(sampleKind)) {
            newSample.setSampleType(SampleTypeEnum.unKnown.getIdentity());
        } else {
            newSample.setSampleType(SampleTypeEnum.stand.getIdentity());
            String newNormalConcentration = DataCountUtil.addZero(Double.parseDouble(normalConcentration), concentrationDigit);
            newSample.setNormalConcentration(newNormalConcentration);
        }

        newSample.setSampleKind(sampleKind);
        if (!dynamicHeads.isEmpty()) {
            newSample.setHeads(JSON.toJSONString(dynamicHeads));
        }

        // 如果开启自动切换样品池的话将当前池号设置给样品
        if (isAutoChangePool){
            R<FSamplePoolInfoVO> fSamplePoolInfoVOR = fSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
            newSample.setSampleLocation(String.valueOf(fSamplePoolInfoVOR.getData().getCurrentPoolNum()));
        }

        boolean save = fMeasureSampleService.save(newSample);
        if (!save) {
            return Result.fail("00300008");
        }

        taskSpecimen.add(new FTaskSpecimen(newSample.getId(), createType, noTested,
                newSample.getSampleType(), sampleKind));

        // 更新任务（使用配置的起始值）
        task.setSampleAutoNum(numberConfig.getStartNum());
        task.setSampleKey(JSON.toJSONString(taskSpecimen));
        boolean update = fMeasureTaskService.updateById(task);
        if (!update) {
            return Result.fail("00000048");
        }

        return Result.ok(true);
    }

    /**
     * 创建后续定量测定样品（支持批量创建）
     */
    private Result createSubsequentQuantitativeSamples(AddSampleDTO dto, FMeasureTask task,
                                                       List<FTaskSpecimen> taskSpecimen, String sampleKind,
                                                       String normalConcentration, Integer concentrationDigit,
                                                       List<FHeadVO> dynamicHeads, boolean isAutoChangePool,
                                                       String deviceSerialNumber, SampleNumConfig numberConfig,
                                                       SampleNameConfig nameConfig, Integer sampleAutoNum) {

        // 检查是否有未测试的样品
        boolean hasNoTested = false;
        List<String> sampleNumList = new ArrayList<>();
        List<String> sampleNameList = new ArrayList<>();
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
            if (measureSample != null && StringUtils.hasText(measureSample.getSampleName())) {
                sampleNumList.add(measureSample.getSampleNumber());
                sampleNameList.add(measureSample.getSampleName());
                if (StringUtils.hasText(measureSample.getNormalConcentration())) {
                    cou = Double.parseDouble(measureSample.getNormalConcentration()) - last;
                    last = Double.parseDouble(measureSample.getNormalConcentration());
                }
            }
        }

        if (hasNoTested) {
            return Result.ok(true);
        }

        if (cou == 0) {
            cou = 1;
        }

        // 计算要创建的样品数量
        int samplesToCreate = 1;
        if (isAutoChangePool) {
            List<Integer> usefulLocation = fSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
            samplesToCreate = usefulLocation.size();
        }

        // 计算当前的样品编号和名称的最大值
        int currentMaxNumber = findMaxNumberFromSamples(sampleNumList, numberConfig.getPreName());
        int currentMaxNameNumber = findMaxNumberFromSamples(sampleNameList, nameConfig.getPreName());

        // 批量创建样品
        for (int i = 0; i < samplesToCreate; i++) {
            FMeasureSample newSample = new FMeasureSample();

            // 生成唯一的样品编号
            int nextNumber;
            if (currentMaxNumber < numberConfig.getStartNum()) {
                // 如果当前最大值小于起始值，从起始值开始
                nextNumber = numberConfig.getStartNum();
            } else {
                // 否则按递增规则计算下一个编号
                nextNumber = currentMaxNumber + numberConfig.getStep();
            }

            // 检查编号是否已存在，如果存在则继续递增
            while (sampleNumList.contains(numberConfig.getPreName() + nextNumber)) {
                nextNumber += numberConfig.getStep();
            }

            // 生成唯一的样品名称
            int nextNameNumber;
            if (currentMaxNameNumber < nameConfig.getStartNum()) {
                // 如果当前最大值小于起始值，从起始值开始
                nextNameNumber = nameConfig.getStartNum();
            } else {
                // 否则按递增规则计算下一个名称编号
                nextNameNumber = currentMaxNameNumber + nameConfig.getStep();
            }

            // 检查名称是否已存在，如果存在则继续递增
            while (sampleNameList.contains(nameConfig.getPreName() + nextNameNumber)) {
                nextNameNumber += nameConfig.getStep();
            }

            newSample.setSampleNumber(numberConfig.getPreName() + nextNumber);
            newSample.setSampleName(nameConfig.getPreName() + nextNameNumber);

            // 设置样品类型
            if (SampleTypeEnum.kind_unknown.getIdentity().equals(sampleKind)) {
                newSample.setSampleType(SampleTypeEnum.unKnown.getIdentity());
            } else {
                newSample.setSampleType(SampleTypeEnum.stand.getIdentity());
                String newNormalConcentration = DataCountUtil.addZero(Double.parseDouble(normalConcentration), concentrationDigit);
                newSample.setNormalConcentration(newNormalConcentration);
                // 更新下一个样品的浓度
                normalConcentration = DataCountUtil.addZero((Double.parseDouble(normalConcentration) + cou), concentrationDigit);
            }

            newSample.setSampleKind(sampleKind);
            if (!dynamicHeads.isEmpty()) {
                newSample.setHeads(JSON.toJSONString(dynamicHeads));
            }

            boolean save = fMeasureSampleService.save(newSample);
            if (!save) {
                return Result.fail("00300008");
            }

            taskSpecimen.add(new FTaskSpecimen(newSample.getId(), createType, noTested,
                    newSample.getSampleType(), sampleKind));

            // 更新列表
            sampleNumList.add(newSample.getSampleNumber());
            sampleNameList.add(newSample.getSampleName());

            // 更新最大值
            currentMaxNumber = nextNumber;
            currentMaxNameNumber = nextNameNumber;

            // 更新自动编号
            sampleAutoNum = nextNumber;
        }

        // 更新任务
        task.setSampleAutoNum(sampleAutoNum);
        task.setSampleKey(JSON.toJSONString(taskSpecimen));
        boolean update = fMeasureTaskService.updateById(task);
        if (!update) {
            return Result.fail("00000048");
        }

        // 处理自动换池
        if (isAutoChangePool && samplesToCreate > 1) {
            List<Integer> usefulLocation = fSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
            guidePublicUtilService.changeSampleLocation(task.getId(), usefulLocation);
        }

        return Result.ok(true);
    }

    /**
     * 处理自动换池
     */
    private void handleAutoChangePool(AddSampleDTO dto, FMeasureTask task, String deviceSerialNumber, boolean isQuantitative) {
        Boolean autoChangePool = dto.getAutoChangePool();

        if (autoChangePool != null && autoChangePool) {
            List<Integer> usefulLocation = fSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
            if (isQuantitative) {
                guidePublicUtilService.changeSampleLocation(task.getId(), usefulLocation);
            } else {
                concisePublicUtilService.changeSampleLocation(task.getId(), usefulLocation);
            }
        } else {
            // 如果没有指定，检查设备配置
            R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
            if (poolInfo.getSuccess()) {
                FSamplePoolInfoVO poolInfoVO = poolInfo.getData();
                if (poolInfoVO.getIsAutoChangePool()) {
                    List<Integer> usefulLocation = fSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
                    if (isQuantitative) {
                        guidePublicUtilService.changeSampleLocation(task.getId(), usefulLocation);
                    } else {
                        concisePublicUtilService.changeSampleLocation(task.getId(), usefulLocation);
                    }
                }
            }
        }
    }


    /**
     * 从样品列表中找到最大的数字编号
     */
    private int findMaxNumberFromSamples(List<String> sampleList, String prefix) {
        int maxNumber = 0;

        for (String sample : sampleList) {
            if (sample.startsWith(prefix)) {
                try {
                    String numberStr = sample.substring(prefix.length());
                    int number = Integer.parseInt(numberStr);
                    if (number > maxNumber) {
                        maxNumber = number;
                    }
                } catch (NumberFormatException e) {
                    // 忽略非数字后缀
                }
            }
        }

        return maxNumber;
    }


}
