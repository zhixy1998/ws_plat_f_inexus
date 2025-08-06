package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.dao.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.measurementPo.*;
import com.persee.workstation.uv_inexus.po.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.measurementService.*;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingParam;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingReceiveInfo;
import com.persee.workstation.uv_inexus.util.pythonUtil.RestUtil;
import com.persee.workstation.uv_inexus.vo.measurementVo.PreCountResultParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.*;
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
@Service("rationService")
public class RationServiceImpl implements RationService {

    @Autowired
    SystemUserManageDAO userManageDAO;
    @Autowired
    TMeasureProjectService tMeasureProjectService;
    @Autowired
    TMeasureSampleService tMeasureSampleService;
    @Autowired
    TMeasureTaskService tMeasureTaskService;
    @Autowired
    TMeasureMethodService tMeasureMethodService;

    @Autowired
    TMeasureDataService tMeasureDataService;

    @Autowired
    TMeasureAnalysisDataService tMeasureAnalysisDataService;

    @Autowired
    DeviceInfoService deviceInfoService;

    @Autowired
    DevMessageUtil devMessageUtil;


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
        TMeasureMethod method = tMeasureMethodService.getById(methodDto.getMethodId());
        if (method == null) {
            return Result.fail("方法不存在");
        }
        RationParameter parameter = new RationParameter();
        BeanUtils.copyProperties(methodDto, parameter);

        String param = JSON.toJSONString(parameter);
        // 更新方法参数
        method.setMeasurementParamInfo(param);
        // 更新方法
        boolean update = tMeasureMethodService.updateById(method);
        return Result.ok(update);
    }

    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, RationParameter rationParameter) {
        if (rationParameter.getWaveLength() == null || rationParameter.getWaveLength().isEmpty()) {
            return Result.fail("请填写波长参数");
        }

        // 创建测量方法表
        TMeasureMethod tMeasureMethod = new TMeasureMethod();
        tMeasureMethod.setProjectItemKey(methodDto.getProjectId());
        tMeasureMethod.setMethodName(methodDto.getMethodName());
        tMeasureMethod.setMeasurementType(methodDto.getMethodType());
        tMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(rationParameter));
        tMeasureMethod.setMethodCreator(updateBy);
        tMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        boolean save = tMeasureMethodService.save(tMeasureMethod);
        if ( save){
            return Result.ok("创建成功");
        }else {
            return Result.fail("创建失败");
        }
    }

    @Override
    public Result<MeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType) {

        SystemUserManagePO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SystemUserManagePO.class).eq(SystemUserManagePO::getUserName, updateBy));
        String id = po.getId();

        LambdaQueryWrapper<TMeasureTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TMeasureTask::getUserId, id)
                .eq(TMeasureTask::getMeasureType, measureType)
                .orderByDesc(TMeasureTask::getCreateTime);
        List<TMeasureTask> taskList = tMeasureTaskService.list(wrapper);
        if (taskList != null && !taskList.isEmpty()){
            TMeasureTask task = taskList.get(0);
            String projectItemKey = task.getProjectItemKey();
            String methodKey = task.getMethodKey();

            TMeasureProject project = tMeasureProjectService.getById(projectItemKey);
            TMeasureMethod method = tMeasureMethodService.getById(methodKey);
            MeasurementCurrentInfoVO vo = new MeasurementCurrentInfoVO();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getTaskName());
            vo.setMethodId(method.getId());
            vo.setMethodName(method.getMethodName());
            vo.setProjectId(project.getId());
            vo.setProjectName(project.getProjectName());
            return Result.ok(vo);
        }

        MeasurementCurrentInfoVO vo = new MeasurementCurrentInfoVO();

        LambdaQueryWrapper<TMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureMethod::getMethodCreator, updateBy)
                .eq(TMeasureMethod::getMeasurementType, measureType)
                .orderByDesc(TMeasureMethod::getMethodCreateTime);
        List<TMeasureMethod> list = tMeasureMethodService.list(queryWrapper);
        if (list == null|| list.isEmpty()){
            LambdaQueryWrapper<TMeasureProject> projectWrapper = new LambdaQueryWrapper<>();
            projectWrapper.eq(TMeasureProject::getCreatorKey, updateBy)
                    .orderByDesc(TMeasureProject::getCreateTime);
            List<TMeasureProject> projectList = tMeasureProjectService.list(projectWrapper);
            if (projectList == null || projectList.isEmpty()){
                return Result.ok(vo);
            }
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
        return Result.ok(vo);
    }


    @Override
    public Result<List<JSONObject>> querySampleList(String taskId, String sampleKind) {
        List<JSONObject> sampleVOList = new ArrayList<>();
        if (!StringUtils.hasText(taskId)) {
            return Result.ok(sampleVOList);
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        if (StringUtils.hasText(task.getSampleKey())) {
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(deleteType)) {
                    continue;
                }
                TMeasureSample measureSample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                if (!StringUtils.hasText(measureSample.getSampleKind()) || !measureSample.getSampleKind().equals(sampleKind)) {
                    continue;
                }
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
    public Result<List<HeadVO>> querySampleHeads(String taskId, String sampleKind) {
        List<HeadVO> ans = new ArrayList<>();
        LinkedHashSet<HeadVO> ansSet = new LinkedHashSet<>();
        if (!StringUtils.hasText(taskId)) {
            return Result.ok(ans);
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        if (StringUtils.hasText(task.getSampleKey())) {
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen specimen : taskSpecimen) {
                TMeasureSample measureSample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                if (!StringUtils.hasText(measureSample.getSampleKind()) || !measureSample.getSampleKind().equals(sampleKind)) {
                    continue;
                }
                List<HeadVO> heads = JSON.parseArray(measureSample.getHeads(), HeadVO.class);

                ansSet.addAll(heads);
            }
        }
        ans.addAll(ansSet);
        return Result.ok(ans);
    }


    @Override
    @Transactional
    public Result addOrDeleteSample(RationAddOrDeleteSampleDTO dto) {

        if (!StringUtils.hasText(dto.getMethodId())) {
            return Result.fail("没有方法！");
        }

        if (dto.getNewStandSamples().isEmpty() && dto.getDelSampleIds().isEmpty() && dto.getNewUnknowSamples().isEmpty()) {
            return Result.ok("没有要新增或删除的样品！");
        }

        if (dto.getStandHeads() != null && dto.getStandHeads().size() > 10) {
            return Result.fail("标准样品表格新增列数不能超过10列");
        }

        if (dto.getUnknowHeads() != null && dto.getUnknowHeads().size() > 10) {
            return Result.fail("未知样品表格新增列数不能超过10列");
        }

        if (!StringUtils.hasText(dto.getTaskId())) {
            //创建任务
            TMeasureTask newTask = new TMeasureTask();

            TMeasureMethod method = tMeasureMethodService.getById(dto.getMethodId());
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
                if (tMeasureTaskService.save(newTask)) {
                    save = true;
                    break;
                }
            }
            if (!save) {
                return Result.fail("系统出错");
            }
            dto.setTaskId(newTask.getId());
        }

        TMeasureTask task = tMeasureTaskService.getById(dto.getTaskId());
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen specimen : taskSpecimen) {
            if (dto.getDelSampleIds().contains(specimen.getSpecimenKey())) {
                specimen.setDeleteType(deleteType);
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(TMeasureData::getTaskId, task.getId()).orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
                TMeasureData one = tMeasureDataService.getOne(queryWrapper);
                if (one != null) {
                    one.setIsDelete(deleteType);
                    tMeasureDataService.updateById(one);
                }
            }
        }

        for (Map<String, String> map : dto.getNewStandSamples()) {

            String jsonString = JSON.toJSONString(map);
            SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);

            TMeasureSample newSample = new TMeasureSample();
            BeanUtils.copyProperties(sample, newSample);
            newSample.setId(sample.getSampleId());
            newSample.setSampleKind(stand);
            if (dto.getStandHeads() != null && !dto.getStandHeads().isEmpty()) {
                JSONObject newColData = new JSONObject();

                for (HeadVO head : dto.getStandHeads()) {
                    String s = map.get(head.getIdentity());
                    newColData.put(head.getIdentity(), s);
                }

                newSample.setHeads(JSON.toJSONString(dto.getStandHeads()));
                newSample.setNewColData(JSON.toJSONString(newColData));
            }

            if (StringUtils.hasText(newSample.getId())) {
                tMeasureSampleService.updateById(newSample);
            } else {
                TaskSpecimen specimen = new TaskSpecimen();
                specimen.setDeleteType(createType);
                specimen.setIsTested(noTested);
                log.info("新增的标准样品：{}", newSample);
                tMeasureSampleService.save(newSample);
                specimen.setSpecimenKey(newSample.getId());
                taskSpecimen.add(specimen);
            }
        }


        for (Map<String, String> map : dto.getNewUnknowSamples()) {

            String jsonString = JSON.toJSONString(map);
            SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);

            TMeasureSample newSample = new TMeasureSample();
            BeanUtils.copyProperties(sample, newSample);
            newSample.setId(sample.getSampleId());
            newSample.setSampleKind(unknown);

            if (dto.getUnknowHeads() != null && !dto.getUnknowHeads().isEmpty()) {
                JSONObject newColData = new JSONObject();

                for (HeadVO head : dto.getUnknowHeads()) {
                    String s = map.get(head.getIdentity());
                    newColData.put(head.getIdentity(), s);
                }

                newSample.setHeads(JSON.toJSONString(dto.getUnknowHeads()));
                newSample.setNewColData(JSON.toJSONString(newColData));
            }

            if (StringUtils.hasText(newSample.getId())) {
                tMeasureSampleService.updateById(newSample);
            } else {
                TaskSpecimen specimen = new TaskSpecimen();
                specimen.setDeleteType(createType);
                specimen.setIsTested(noTested);
                log.info("新增的未知样品：{}", newSample);
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
            return Result.fail("请选择需要删除的任务和样品");
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            return Result.fail("任务无样品");
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen specimen : taskSpecimen) {
            if (sampleId.equals(specimen.getSpecimenKey())) {
                specimen.setDeleteType(deleteType);
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(TMeasureData::getTaskId, task.getId())
                        .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
                TMeasureData one = tMeasureDataService.getOne(queryWrapper);
                if (one != null) {
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
                    .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
            TMeasureData one = tMeasureDataService.getOne(queryWrapper);
            if (one != null) {
                one.setIsDelete(createType);
                tMeasureDataService.updateById(one);
            }
        }
        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);

        return Result.ok(tMeasureTaskService.updateById(task));
    }

    @Override
    public Result<RationMethodParam> queryMethodParam(String methodId) {
        if (!StringUtils.hasText(methodId)) {
            return Result.fail("方法ID不能为空");
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }

        if (!StringUtils.hasText(method.getMeasurementParamInfo())) {
            return Result.fail("未查询到该方法参数");
        }
        RationParameter rationParameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);

        RationMethodParam param = new RationMethodParam();
        BeanUtils.copyProperties(rationParameter, param);
        param.setMethodId(method.getId());
        param.setMethodName(method.getMethodName());
        param.setMeasurementType(method.getMeasurementType());
        return Result.ok(param);
    }

    //标准样品表头
    @Override
    public Result<List<HeadVO>> getStandSampleTableHead(String taskId, String methodId, Boolean allDisplay) {

        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(methodId)) {
            return Result.fail("请选择方法或任务");
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }
        LinkedHashSet<HeadVO> ans = new LinkedHashSet<>();

        ans.add(new HeadVO(SampleTableEnum.sampleName.getName(), SampleTableEnum.sampleName.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.sampleNumber.getName(), SampleTableEnum.sampleNumber.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.sampleType.getName(), SampleTableEnum.sampleType.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.normalConcentration.getName(), SampleTableEnum.normalConcentration.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.sampleLocation.getName(), SampleTableEnum.sampleLocation.getIdentity()));

        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
        List<String> waveLengths = parameter.getWaveLength();

        //根据任务中记录的样品id判断是否存在空白样品
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        boolean empty = false;
        if (task != null && StringUtils.hasText(task.getSampleKey())) {
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen specimen : taskSpecimen) {
                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

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
                    List<HeadVO> heads = JSON.parseArray(sample.getHeads(), HeadVO.class);
                    ans.addAll(heads);
                }
            }
        }

        if (waveLengths != null && !waveLengths.isEmpty()) {

            for (String waveLength : waveLengths) {
                StringBuilder s1 = new StringBuilder();
                StringBuilder s2 = new StringBuilder();

                s1.append(waveLength).append(WaveLengthEnum.abs.getName());
                s2.append(waveLength).append("-").append(WaveLengthEnum.abs.getCode());
                HeadVO headVO1 = new HeadVO(s1.toString(), s2.toString());
                ans.add(headVO1);

                if (allDisplay) {
                    s1.setLength(0);
                    s2.setLength(0);
                    s1.append(waveLength).append(WaveLengthEnum.tPercent.getName());
                    s2.append(waveLength).append("-").append(WaveLengthEnum.tPercent.getCode());
                    HeadVO headVO2 = new HeadVO(s1.toString(), s2.toString());
                    ans.add(headVO2);
                }
            }
        }

        ans.add(new HeadVO(SampleTableEnum.Abs.getName(), SampleTableEnum.Abs.getIdentity()));
        if (empty) {
            ans.add(new HeadVO(SampleTableEnum.sumAbs.getName(), SampleTableEnum.sumAbs.getIdentity()));
        }
        ans.add(new HeadVO(SampleTableEnum.samplingTime.getName(), SampleTableEnum.samplingTime.getIdentity()));


        List<HeadVO> ansList = new ArrayList<>(ans);
        return Result.ok(ansList);

    }

    //未知样品表头
    @Override
    public Result<List<HeadVO>> getUnknownSampleTableHead(String taskId, String methodId, Boolean allDisplay) {

        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(methodId)) {
            return Result.fail("请选择任务或方法");
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }
        LinkedHashSet<HeadVO> ans = new LinkedHashSet<>();
        //根据任务中记录的样品id判断是否存在空白样品

        ans.add(new HeadVO(SampleTableEnum.sampleName.getName(), SampleTableEnum.sampleName.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.sampleNumber.getName(), SampleTableEnum.sampleNumber.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.sampleType.getName(), SampleTableEnum.sampleType.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.dilutionRatio.getName(), SampleTableEnum.dilutionRatio.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.amount.getName(), SampleTableEnum.amount.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.sampleLocation.getName(), SampleTableEnum.sampleLocation.getIdentity()));

        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
        List<String> waveLengths = parameter.getWaveLength();

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        boolean empty = false;
        if (task != null && StringUtils.hasText(task.getSampleKey())) {
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen specimen : taskSpecimen) {
                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

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
                    List<HeadVO> heads = JSON.parseArray(sample.getHeads(), HeadVO.class);
                    ans.addAll(heads);
                }
            }
        }

        if (waveLengths != null && !waveLengths.isEmpty()) {

            for (String waveLength : waveLengths) {
                StringBuilder s1 = new StringBuilder();
                StringBuilder s2 = new StringBuilder();

                s1.append(waveLength).append(WaveLengthEnum.abs.getName());
                s2.append(waveLength).append("-").append(WaveLengthEnum.abs.getCode());
                HeadVO headVO1 = new HeadVO(s1.toString(), s2.toString());
                ans.add(headVO1);

                if (allDisplay) {
                    s1.setLength(0);
                    s2.setLength(0);
                    s1.append(waveLength).append(WaveLengthEnum.tPercent.getName());
                    s2.append(waveLength).append("-").append(WaveLengthEnum.tPercent.getCode());
                    HeadVO headVO2 = new HeadVO(s1.toString(), s2.toString());
                    ans.add(headVO2);
                }
            }
        }
        ans.add(new HeadVO(SampleTableEnum.Abs.getName(), SampleTableEnum.Abs.getIdentity()));
        if (empty) {
            ans.add(new HeadVO(SampleTableEnum.sumAbs.getName(), SampleTableEnum.sumAbs.getIdentity()));
        }

        ans.add(new HeadVO(SampleTableEnum.fitConcentration.getName(), SampleTableEnum.fitConcentration.getIdentity()));

        if (parameter.getResultParams() != null && !parameter.getResultParams().isEmpty()) {

            for (PreCountResultParam param : parameter.getResultParams()) {
                StringBuilder s2 = new StringBuilder();
                String s1 = param.getResultName() + "(" + param.getUnit() + ")";
                s2.append(param.getResultIdentity()).append("-").append(param.getFormulaText());
                HeadVO headVO = new HeadVO(s1, s2.toString());
                ans.add(headVO);
            }
        }
        ans.add(new HeadVO(SampleTableEnum.samplingTime.getName(), SampleTableEnum.samplingTime.getIdentity()));
        List<HeadVO> ansList = new ArrayList<>(ans);
        return Result.ok(ansList);
    }

    @Override
    public Result saveStandSampleData(RationDataVO dataInfo) {

        String sampleId = dataInfo.getSampleId();
        String taskId = dataInfo.getTaskId();
        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getSampleKey, sampleId)
                .orderByDesc(TMeasureData::getSerialNumber);
        List<TMeasureData> list = tMeasureDataService.list(queryWrapper);

        TMeasureData tMeasureData = new TMeasureData();
        tMeasureData.setTaskId(taskId);
        tMeasureData.setSampleKey(sampleId);
        tMeasureData.setInstrumentData(JSON.toJSONString(dataInfo.getMachineData()));
        tMeasureData.setOriginalCalculationData(JSON.toJSONString(dataInfo.getWavelengthData()));
        tMeasureData.setMeasurementStartTime(dataInfo.getStartTime());
        tMeasureData.setMeasurementEndTime(dataInfo.getEndTime());
        tMeasureData.setSamplingTime(dataInfo.getSampleTime());

        if (list != null && !list.isEmpty()) {
            TMeasureData data = list.get(0);
            tMeasureData.setSerialNumber(data.getSerialNumber() + 1);
        } else {
            tMeasureData.setSerialNumber(0);
        }
        tMeasureData.setIsDelete(createType);
        tMeasureDataService.save(tMeasureData);


        LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                .eq(TMeasureAnalysisData::getAnalysisName, tMeasureData.getId() + "-"+tMeasureData.getSerialNumber());
        TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (one != null) {
            one.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            one.setIsDelete(createType);
            tMeasureAnalysisDataService.save(one);
        } else {
            TMeasureAnalysisData analysisData = new TMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            analysisData.setAnalysisName(tMeasureData.getId() + "-"+tMeasureData.getSerialNumber());
            analysisData.setIsDelete(createType);
            tMeasureAnalysisDataService.save(analysisData);
        }
        return Result.ok(true);

    }

    @Override
    public Result saveUnknowSampleData(RationDataVO dataInfo) {

        String sampleId = dataInfo.getSampleId();
        String taskId = dataInfo.getTaskId();
        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getSampleKey, sampleId)
                .orderByDesc(TMeasureData::getSerialNumber);
        List<TMeasureData> list = tMeasureDataService.list(queryWrapper);

        TMeasureData tMeasureData = new TMeasureData();
        tMeasureData.setTaskId(taskId);
        tMeasureData.setSampleKey(sampleId);
        tMeasureData.setInstrumentData(JSON.toJSONString(dataInfo.getMachineData()));
        tMeasureData.setOriginalCalculationData(JSON.toJSONString(dataInfo.getWavelengthData()));
        tMeasureData.setMeasurementStartTime(dataInfo.getStartTime());
        tMeasureData.setMeasurementEndTime(dataInfo.getEndTime());
        tMeasureData.setSamplingTime(dataInfo.getSampleTime());

        if (list != null && !list.isEmpty()) {
            TMeasureData data = list.get(0);
            tMeasureData.setSerialNumber(data.getSerialNumber() + 1);
        } else {
            tMeasureData.setSerialNumber(0);
        }
        tMeasureData.setIsDelete(createType);
        tMeasureDataService.save(tMeasureData);


        LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                .eq(TMeasureAnalysisData::getAnalysisName, tMeasureData.getId() + "-"+tMeasureData.getSerialNumber());
        TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (one != null) {
            one.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            one.setIsDelete(createType);
            tMeasureAnalysisDataService.save(one);
        } else {
            TMeasureAnalysisData analysisData = new TMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            analysisData.setAnalysisName(tMeasureData.getId() + "-"+tMeasureData.getSerialNumber());
            analysisData.setIsDelete(createType);
            tMeasureAnalysisDataService.save(analysisData);
        }
        return Result.ok(true);

    }

    @Override
    public R<RationCountDataInfo> countAbsValue(List<RationWavelengthDataInfo> originData, String methodId) {

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null || !StringUtils.hasText(method.getMeasurementParamInfo())) {
            return R.fail("方法参数缺少");
        }
        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);

        List<String> waveLength = parameter.getWaveLength();
        List<String> lengthParams = parameter.getWaveLengthParams();
        double ans = 0;

        for (int i = 0; i < waveLength.size(); i++) {
            double value = 0;
            for (RationWavelengthDataInfo dataInfo : originData) {
                if (waveLength.get(i).equals(dataInfo.getWavelength())) {
                    value = Double.parseDouble(dataInfo.getAbsValue());
                }
            }
            if (value == 0) {
                return R.fail("未找到对应波长数据");
            }
            if (i < lengthParams.size()) {
                value = value * Double.parseDouble(lengthParams.get(i));
            } else {
                value = value * 1;
            }
            ans += value;
        }
        RationCountDataInfo countDataInfo = new RationCountDataInfo();
        countDataInfo.setColValue(ans);
        countDataInfo.setIdentity(SampleTableEnum.Abs.getIdentity());
        countDataInfo.setHeadName(SampleTableEnum.Abs.getName());

        return R.ok(countDataInfo);
    }

    @Override
    public Result<List<Map<String, String>>> queryStandSampleData(String taskId, Boolean allDisplay) {
        if (!StringUtils.hasText(taskId) || allDisplay == null) {
            return Result.fail("未选择任务");
        }
        List<Map<String, String>> ans = new ArrayList<>();
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        boolean repeat = false;
        if (StringUtils.hasText(task.getMethodKey())) {
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
                if (StringUtils.hasText(parameter.getRepeatMode()) && parameter.getRepeatMode().equals("重复")) {
                    repeat = true;
                }
            }
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("未选择样品");
            return Result.ok(ans);
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        for (TaskSpecimen specimen : taskSpecimen) {
            if (!allDisplay && specimen.getDeleteType().equals("1")) {
                continue;
            }
            TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
            if (sample == null || !sample.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                log.info("不是标准样品，跳过");
                continue;
            }

            Map<String, String> map = new HashMap<>();
            //样品信息
            map.put(SampleTableEnum.sampleName.getIdentity(), sample.getSampleName());
            map.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber());
            map.put(SampleTableEnum.sampleType.getIdentity(), sample.getSampleType());
            map.put(SampleTableEnum.sampleKind.getIdentity(), sample.getSampleKind());
            map.put(SampleTableEnum.normalConcentration.getIdentity(), sample.getNormalConcentration());
            map.put(SampleTableEnum.sampleLocation.getIdentity(), sample.getSampleLocation());
            map.put(SampleTableEnum.dilutionRatio.getIdentity(), sample.getDilutionRatio());
            map.put(SampleTableEnum.amount.getIdentity(), sample.getSampleVolume());

            //数据信息,repeat：false-不重复，true-重复
            if (!repeat) {
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getTaskId, taskId)
                        .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
                TMeasureData data = tMeasureDataService.getOne(queryWrapper);
                if (data != null) {
                    map.put(SampleTableEnum.samplingTime.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.remark.getIdentity(), data.getRemark());
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                        for (RationWavelengthDataInfo dataInfo : list) {
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                        }
                    }

                    LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                    analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                            .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                            .eq(TMeasureAnalysisData::getAnalysisName, data.getId() + "-" + data.getSerialNumber());

                    TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                    if (one != null && one.getAnalysisResult() != null) {
                        List<RationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), RationCountDataInfo.class);
                        for (RationCountDataInfo countData : countDataList) {
                            map.put(countData.getIdentity(), String.valueOf(countData.getColValue()));
                        }
                    }

                }
            } else {
                //重复模式，获取数据方式待定----暂时先以不重复做回显
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getTaskId, taskId)
                        .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
                TMeasureData data = tMeasureDataService.getOne(queryWrapper);
                if (data != null) {
                    map.put(SampleTableEnum.samplingTime.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.remark.getIdentity(), data.getRemark());
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                        for (RationWavelengthDataInfo dataInfo : list) {
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                        }
                    }

                    LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                    analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                            .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                            .eq(TMeasureAnalysisData::getAnalysisName, data.getId() + "-" + data.getSerialNumber());

                    TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                    if (one != null && one.getAnalysisResult() != null) {
                        List<RationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), RationCountDataInfo.class);
                        for (RationCountDataInfo countData : countDataList) {
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
    public Result<List<Map<String, String>>> queryUnknowSampleData(String taskId, Boolean allDisplay) {
        if (!StringUtils.hasText(taskId) || allDisplay == null) {
            return Result.fail("未选择任务");
        }
        List<Map<String, String>> ans = new ArrayList<>();
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        boolean repeat = false;
        if (StringUtils.hasText(task.getMethodKey())) {
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
                if (StringUtils.hasText(parameter.getRepeatMode()) && parameter.getRepeatMode().equals("重复")) {
                    repeat = true;
                }
            }
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("未选择样品");
            return Result.ok(ans);
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        for (TaskSpecimen specimen : taskSpecimen) {
            if (!allDisplay && specimen.getDeleteType().equals("1")) {
                continue;
            }
            TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
            if (sample == null || !sample.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                log.info("不是未知样品，跳过");
                continue;
            }

            Map<String, String> map = new HashMap<>();
            //样品信息
            map.put(SampleTableEnum.sampleName.getIdentity(), sample.getSampleName());
            map.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber());
            map.put(SampleTableEnum.sampleType.getIdentity(), sample.getSampleType());
            map.put(SampleTableEnum.sampleKind.getIdentity(), sample.getSampleKind());
            map.put(SampleTableEnum.normalConcentration.getIdentity(), sample.getNormalConcentration());
            map.put(SampleTableEnum.sampleLocation.getIdentity(), sample.getSampleLocation());


            //数据信息,repeat：false-不重复，true-重复
            if (!repeat) {
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getTaskId, taskId)
                        .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
                TMeasureData data = tMeasureDataService.getOne(queryWrapper);
                if (data != null) {

                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                        for (RationWavelengthDataInfo dataInfo : list) {
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                        }
                    }

                    map.put(SampleTableEnum.samplingTime.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.remark.getIdentity(), data.getRemark());

                    LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                    analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                            .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                            .eq(TMeasureAnalysisData::getAnalysisName, data.getId() + "-" + data.getSerialNumber());

                    TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                    if (one != null && one.getAnalysisResult() != null) {
                        List<RationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), RationCountDataInfo.class);
                        for (RationCountDataInfo countData : countDataList) {
                            map.put(countData.getIdentity(), String.valueOf(countData.getColValue()));
                        }
                    }

                }
            } else {
                //重复模式，获取数据方式待定----暂时先以不重复做回显
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getTaskId, taskId)
                        .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
                TMeasureData data = tMeasureDataService.getOne(queryWrapper);
                if (data != null) {
                    map.put(SampleTableEnum.samplingTime.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.remark.getIdentity(), data.getRemark());
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                        for (RationWavelengthDataInfo dataInfo : list) {
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                        }
                    }

                    LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                    analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                            .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                            .eq(TMeasureAnalysisData::getAnalysisName, data.getId() + "-" + data.getSerialNumber());

                    TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                    if (one != null && one.getAnalysisResult() != null) {
                        List<RationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), RationCountDataInfo.class);
                        for (RationCountDataInfo countData : countDataList) {
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
    public FittingReceiveInfo fittingCurve(List<RationCountDataInfo> countData, Boolean empty, String methodId, List<String> inputParam) {
        
        List<Double> data = new ArrayList<>();
        FittingParam param = new FittingParam();
        if (empty) {
            for (RationCountDataInfo dataInfo : countData) {
                if (dataInfo.getIdentity().equals(SampleTableEnum.sumAbs.getIdentity())) {
                    data.add(dataInfo.getColValue());

                }
            }
        } else {
            for (RationCountDataInfo dataInfo : countData) {
                if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                    data.add(dataInfo.getColValue());
                }
            }
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);

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
        String s = RestUtil.dataFit(param);
        FittingReceiveInfo info = JSON.parseObject(s, FittingReceiveInfo.class);
        if (info == null || !ResultCodeEnum.SUCCESS.getCode().equals(info.getCode())) {
            log.info("数据拟合失败");
            return null;
        }

        Arrays.stream(param.getX_data()).min().ifPresent(info::setMinX);
        Arrays.stream(param.getX_data()).max().ifPresent(info::setMaxX);

        return info;
    }

    @Override
    public Result<RationFitCurveVO> queryCurveInfo(String taskId, String methodId){

        if (!StringUtils.hasText(taskId)) {
            log.info("缺少任务id");
            return Result.fail("未选择任务");
        }
        if (!StringUtils.hasText(methodId)) {
            log.info("缺少方法id");
            return Result.fail("未选择方法");
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            log.info("任务不存在");
            return Result.fail("任务不存在");
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);

        RationFitCurveVO resultVo = new RationFitCurveVO();

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
    public RationFitCurveVO curveData(FittingReceiveInfo info) {
        List<Double> list = info.getCoefficients().stream()
                .map(d -> BigDecimal.valueOf(d)
                        .setScale(3, RoundingMode.HALF_UP)
                        .doubleValue())
                .collect(Collectors.toList());
        List<Double> x_data = new ArrayList<>();
        List<Double> y_data = new ArrayList<>();

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
                    x_data.add(i);
                    y_data.add(y);
                }
                break;
            case 2:
                for (double i = minX - 1; i < maxX + 1; i += step) {
                    double y = k0 + k1 * i + k2 * i * i;
                    x_data.add(i);
                    y_data.add(y);
                }
                break;
            case 3:
                for (double i = minX - 1; i < maxX + 1; i += step) {
                    double y = k0 + k1 * i + k2 * i * i + k3 * i * i * i;
                    x_data.add(i);
                    y_data.add(y);
                }
                break;

            case 4:
                for (double i = minX - 1; i < maxX + 1; i += step) {
                    double y = k0 + k1 * i + k2 * i * i + k3 * i * i * i + k4 * i * i * i * i;
                    x_data.add(i);
                    y_data.add(y);

                }
                break;
            default:
                break;
        }
        RationFitCurveVO vo = new RationFitCurveVO();
        vo.setCoefficientList(list);
        vo.setXList(x_data.stream()
                .map(d -> BigDecimal.valueOf(d)
                        .setScale(3, RoundingMode.HALF_UP)
                        .doubleValue())
                .collect(Collectors.toList()));
        vo.setYList(y_data.stream()
                .map(d -> BigDecimal.valueOf(d)
                        .setScale(3, RoundingMode.HALF_UP)
                        .doubleValue())
                .collect(Collectors.toList()));
        vo.setCurveType(RationCurveParamEnums.getName(String.valueOf(info.getDegree())));
        return vo;
    }

    @Override
    public String valueCalculate(FittingReceiveInfo info, Double value, String equationType) {

        String s = "";
        String ans = null;

        if (equationType.equals(RationCurveParamEnums.typeAbs.getIdentity())) {
            s = RestUtil.concentrationCalculate(info.getCoefficients(), value, 1);
        } else if (equationType.equals(RationCurveParamEnums.typeCurve.getIdentity())) {
            s = RestUtil.concentrationCalculate(info.getCoefficients(), value, 0);
        }
        if (StringUtils.hasText(s)) {
            JSONObject jsonObject = JSON.parseObject(s);

            ans = jsonObject.get("value").toString();
        }
        return ans;
    }

    @Override
    public Result zero(String deviceSerialNumber, String methodId, String taskId, String updateBy) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            return Result.fail("未连接仪器");
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }

        if (!StringUtils.hasText(taskId)){
            //创建任务

            //创建任务
            TMeasureTask newTask = new TMeasureTask();

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
                if (tMeasureTaskService.save(newTask)) {
                    save = true;
                    break;
                }
            }
            if (!save) {
                return Result.fail("系统出错");
            }
            taskId = newTask.getId();
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
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

        boolean update = tMeasureTaskService.updateById(task);
        return update ? Result.ok() : Result.fail();
    }


}
