package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.concise.common.FMethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FRationAddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FTaskHeadSnapshotDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.*;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
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
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FSampleVO;
import com.persee.workstation.uv_inexus.vo.concise.ration.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("fRationService")
public class FRationServiceImpl implements FRationService {

    @Autowired
    SystemUserManageDAO userManageDAO;
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
    private String stand;

    @Value("1")
    private String unknown;

    @Value("0")
    String noTested;

    @Value("1")
    String tested;

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
        if ( save){
            return Result.ok("创建成功");
        }else {
            return Result.fail("创建失败");
        }
    }

    @Override
    public Result<FMeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType) {

        SystemUserManagePO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SystemUserManagePO.class).eq(SystemUserManagePO::getUserName, updateBy));
        String id = po.getId();

        LambdaQueryWrapper<FMeasureTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FMeasureTask::getUserId, id)
                .eq(FMeasureTask::getMeasureType, measureType)
                .orderByDesc(FMeasureTask::getCreateTime);
        List<FMeasureTask> taskList = fMeasureTaskService.list(wrapper);
        if (taskList != null && !taskList.isEmpty()){
            FMeasureTask task = taskList.get(0);
            String projectItemKey = task.getProjectItemKey();
            String methodKey = task.getMethodKey();

            FMeasureProject project = fMeasureProjectService.getById(projectItemKey);
            FMeasureMethod method = fMeasureMethodService.getById(methodKey);
            FMeasurementCurrentInfoVO vo = new FMeasurementCurrentInfoVO();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getTaskName());
            vo.setMethodId(method.getId());
            vo.setMethodName(method.getMethodName());
            vo.setProjectId(project.getId());
            vo.setProjectName(project.getProjectName());
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
            vo.setProjectName(projectList.get(0).getProjectName());
            vo.setProjectId(projectList.get(0).getId());

        } else {
            FMeasureMethod fMeasureMethod = list.get(0);
            FMeasureProject fMeasureProject = fMeasureProjectService.getById(fMeasureMethod.getProjectItemKey());

            vo.setMethodId(fMeasureMethod.getId());
            vo.setMethodName(fMeasureMethod.getMethodName());
            vo.setProjectId(fMeasureProject.getId());
            vo.setProjectName(fMeasureProject.getProjectName());
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
    @Transactional
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
            SystemUserManagePO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SystemUserManagePO.class).eq(SystemUserManagePO::getUserName, dto.getUpdateBy()));
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
        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
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
    public Result<FFRationMethodParam> queryMethodParam(String methodId) {
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

        FFRationMethodParam param = new FFRationMethodParam();
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
                FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());

                if(sample == null || !SampleTypeEnum.kind_stand.getIdentity().equals(sample.getSampleKind())) {
                    log.info("非标准样品，跳过");
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
        List<FHeadVO> result = dealHeads(new ArrayList<>(ans), parameter, SampleTypeEnum.kind_stand.getIdentity(), allDisplay, empty);
        return Result.ok(result);

    }

    private List<FHeadVO> dealHeads(List<FHeadVO> heads, FRationParameter parameter, String sampleKind, Boolean allDisplay, Boolean empty) {
        List<String> waveLength = parameter.getWaveLength();
        Map<String, FHeadVO> map = heads.stream().collect(Collectors.toMap(FHeadVO::getIdentity, head -> head));

        for (String s : waveLength) {
            String identity = s + "-"+ WaveLengthEnum.abs.getCode();
            String tPercentIdentity = s + "-"+ WaveLengthEnum.tPercent.getCode();
            String absName = s + WaveLengthEnum.abs.getName();
            String tPercentName = s + WaveLengthEnum.tPercent.getName();

            if (!map.containsKey(identity)) {
                FHeadVO newHead = new FHeadVO(absName, identity, TableHeadILocateEnums.measure.getIdentity(), absName);
                heads.add(newHead);
            }

            if (allDisplay) {
                if (!map.containsKey(tPercentIdentity)) {
                    FHeadVO newHead = new FHeadVO(tPercentName, tPercentIdentity, TableHeadILocateEnums.measure.getIdentity(), tPercentName);
                    heads.add(newHead);
                }
            } else {
                if (map.containsKey(tPercentIdentity)) {
                    FHeadVO vo = map.get(tPercentIdentity);
                    heads.remove(vo);
                }
            }
        }
        String sumAbsIdentity = SampleTableEnum.sumAbs.getIdentity();
        String absIdentity = SampleTableEnum.Abs.getIdentity();
        if (!map.containsKey(absIdentity)) {
            FHeadVO newHead = new FHeadVO(SampleTableEnum.Abs.getName(), absIdentity, TableHeadILocateEnums.measure.getIdentity(), SampleTableEnum.Abs.getName());
            heads.add(newHead);
        }
        if (empty) {
            if (!map.containsKey(sumAbsIdentity)) {
                FHeadVO newHead = new FHeadVO(SampleTableEnum.sumAbs.getName(), sumAbsIdentity, TableHeadILocateEnums.measure.getIdentity(), SampleTableEnum.sumAbs.getName());
                heads.add(newHead);
            }
        } else {
            if (map.containsKey(sumAbsIdentity)) {
                FHeadVO vo = map.get(sumAbsIdentity);
                heads.remove(vo);
            }
        }

        if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
            List<FPreCountResultParam> resultParams = parameter.getResultParams();
            for (FPreCountResultParam results : resultParams) {
                //TODO 方法参数标识待定，后续再改动
                if (!StringUtils.hasText(results.getParam())) {
                    results.setParam(results.getResultIdentity() + "-" + results.getFormulaText());
                }
                if (!map.containsKey(results.getParam())) {
                    String name = results.getResultName() + "(" + results.getUnit() + ")";
                    FHeadVO newHead = new FHeadVO(name, results.getParam(), TableHeadILocateEnums.analyze.getIdentity(), name);
                    heads.add(newHead);
                }
            }
        }

        return heads;
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
            map.put(SampleTableEnum.sampleId.getIdentity(), sample.getId());
            map.put(SampleTableEnum.sampleName.getIdentity(), sample.getSampleName());
            map.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber());
            map.put(SampleTableEnum.sampleType.getIdentity(), SampleTypeEnum.getName(sample.getSampleType()));
            map.put(SampleTableEnum.sampleKind.getIdentity(), sample.getSampleKind());
            map.put(SampleTableEnum.normalConcentration.getIdentity(), sample.getNormalConcentration());
            map.put(SampleTableEnum.sampleLocation.getIdentity(), "样品池"+ sample.getSampleLocation());
            map.put(SampleTableEnum.dilutionRatio.getIdentity(), sample.getDilutionRatio());
            map.put(SampleTableEnum.amount.getIdentity(), sample.getSampleVolume());

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
                    map.put(SampleTableEnum.samplingTime.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.remark.getIdentity(), data.getRemark());
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
                    map.put(SampleTableEnum.samplingTime.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.remark.getIdentity(), data.getRemark());
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
                    children.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.avg.getName());

                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), wlData.getAbsValue());
                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(),wlData.getTPercentValue());
                    ans.putIfAbsent(sample.getSampleNumber() + "-" + DataIdentityEnums.avg.getIdentity(), children);
                }
                if (DataIdentityEnums.sd.getIdentity().equals(s)) {
                    children.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.sd.getName());

                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), absData.getSd());
                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tPercentData.getSd());
                    ans.putIfAbsent(sample.getSampleNumber() + "-" + DataIdentityEnums.sd.getIdentity(), children);
                }
                if (DataIdentityEnums.rsd.getIdentity().equals(s)) {
                    children.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getName());
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
        map.put(SampleTableEnum.sampleId.getIdentity(), sample.getId());
        map.put(SampleTableEnum.sampleName.getIdentity(), sample.getSampleName());
        map.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber());
        map.put(SampleTableEnum.sampleType.getIdentity(), SampleTypeEnum.getName(sample.getSampleType()));
        map.put(SampleTableEnum.sampleKind.getIdentity(), sample.getSampleKind());
        map.put(SampleTableEnum.sampleLocation.getIdentity(), "样品池"+sample.getSampleLocation());

        if (sample.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
            map.put(SampleTableEnum.normalConcentration.getIdentity(), sample.getNormalConcentration());
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

                map.put(SampleTableEnum.samplingTime.getIdentity(), data.getSamplingTime());
                map.put(SampleTableEnum.remark.getIdentity(), data.getRemark());

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
                map.put(SampleTableEnum.samplingTime.getIdentity(), data.getSamplingTime());
                map.put(SampleTableEnum.remark.getIdentity(), data.getRemark());
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
                            children.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber() + "-" + split[split.length - 1]);
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
            map.put(SampleTableEnum.sampleId.getIdentity(), sample.getId());
            map.put(SampleTableEnum.sampleName.getIdentity(), sample.getSampleName());
            map.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber());
            map.put(SampleTableEnum.dilutionRatio.getIdentity(), sample.getDilutionRatio());
            map.put(SampleTableEnum.amount.getIdentity(), sample.getSampleVolume());
            map.put(SampleTableEnum.sampleType.getIdentity(), SampleTypeEnum.getName(sample.getSampleType()));
            map.put(SampleTableEnum.sampleKind.getIdentity(), sample.getSampleKind());
            map.put(SampleTableEnum.normalConcentration.getIdentity(), sample.getNormalConcentration());
            map.put(SampleTableEnum.sampleLocation.getIdentity(), "样品池"+sample.getSampleLocation());

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

                    map.put(SampleTableEnum.samplingTime.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.remark.getIdentity(), data.getRemark());

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
                    map.put(SampleTableEnum.samplingTime.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.remark.getIdentity(), data.getRemark());
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
                if (dataInfo.getIdentity().equals(SampleTableEnum.sumAbs.getIdentity())) {
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
    public R<List<FRationCountDataInfo>> formula(List<FRationWavelengthDataInfo> vo, FMeasureSample sample, FMeasureTask task, FRationParameter parameter, Integer digit) {
        if (!StringUtils.hasText(task.getTableInfo())) {
            return R.fail("缺少分析参数，无法计算分析");
        }
        FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
        if (snapshot.getMapRelation() == null || snapshot.getMapRelation().isEmpty()) {
            return R.fail("缺少映射关系，无法计算分析");
        }
        String jsonString = JSON.toJSONString(sample);
        JSONObject allData = JSON.parseObject(jsonString);
        if (StringUtils.hasText(sample.getNewColData())) {
            JSONObject object = JSONObject.parseObject(sample.getNewColData());
            allData.putAll(object);
        }
        LambdaQueryWrapper<FMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureAnalysisData::getTaskId, task.getId())
                .eq(FMeasureAnalysisData::getSampleKey, sample.getId())
                .orderByDesc(FMeasureAnalysisData::getDataVersion)
                .last("limit 1");
        FMeasureAnalysisData one = fMeasureAnalysisDataService.getOne(queryWrapper);
        if (one != null && StringUtils.hasText(one.getAnalysisParam())) {
            String analysisParam = one.getAnalysisParam();
            JSONObject object = JSON.parseObject(analysisParam);
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
                yAbs.setHeadName(SampleTableEnum.sumAbs.getName());
                yAbs.setIdentity(SampleTableEnum.sumAbs.getIdentity());
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
                    sumAbsData.setHeadName(SampleTableEnum.sumAbs.getName());
                    sumAbsData.setIdentity(SampleTableEnum.sumAbs.getIdentity());
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
                yAbs.setHeadName(SampleTableEnum.sumAbs.getName());
                yAbs.setIdentity(SampleTableEnum.sumAbs.getIdentity());
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
                    sumAbsData.setHeadName(SampleTableEnum.sumAbs.getName());
                    sumAbsData.setIdentity(SampleTableEnum.sumAbs.getIdentity());
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
                valueData.setHeadName(SampleTableEnum.fitConcentration.getName());
                valueData.setIdentity(SampleTableEnum.fitConcentration.getIdentity());
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
                            if (dataInfo.getIdentity().equals(SampleTableEnum.sumAbs.getIdentity())) {
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

            SystemUserManagePO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SystemUserManagePO.class).eq(SystemUserManagePO::getUserName, updateBy));
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

            SystemUserManagePO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SystemUserManagePO.class).eq(SystemUserManagePO::getUserName, updateBy));
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

}
