package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.dao.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.AddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.RationParameter;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTableEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.*;
import com.persee.workstation.uv_inexus.po.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.measurementService.*;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricCountDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricOriginDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PreCountResultParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MethodParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.SampleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("rationService")
public class RationServiceImpl implements RationService {

    @Autowired
    private SystemUserManageDAO userManageDAO;
    @Autowired
    private TMeasureProjectService tMeasureProjectService;
    @Autowired
    private TMeasureSampleService tMeasureSampleService;
    @Autowired
    private TMeasureTaskService tMeasureTaskService;
    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    TMeasureDataService tMeasureDataService;

    @Autowired
    TMeasureAnalysisDataService tMeasureAnalysisDataService;


    @Value("1")
    private String deleteType;

    @Value("0")
    private String createType;

    @Value("0")
    private String stand;

    @Value("1")
    private String unknown;

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
    public Result<List<SampleVO>> querySampleInfo(String taskId, String sampleKind) {
        List<SampleVO> sampleVOList = new ArrayList<>();
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
                TMeasureSample measureSample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                if (measureSample.getSampleKind().equals(sampleKind)) {
                    SampleVO vo = new SampleVO();
                    BeanUtils.copyProperties(measureSample, vo);
                    vo.setSampleId(measureSample.getId());
                    sampleVOList.add(vo);
                }
            }
        }
        return Result.ok(sampleVOList);
    }


    @Override
    @Transactional
    public Result addOrDeleteSample(AddOrDeleteSampleDTO dto) {

        if (!StringUtils.hasText(dto.getMethodId())) {
            return Result.fail("方法ID不能为空");
        }

        if (dto.getNewSamples().isEmpty() && dto.getDelSampleIds().isEmpty()) {
            return Result.ok("没有要新增或删除的样品！");
        }

        if (dto.getHeads() != null && dto.getHeads().size() > 10) {
            return Result.fail("表格新增列数不能超过10列");
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
                        .eq(TMeasureData::getTaskId, task.getId());
                TMeasureData one = tMeasureDataService.getOne(queryWrapper);
                if (one != null) {
                    one.setIsDelete(deleteType);
                    tMeasureDataService.updateById(one);
                }
            }
        }

        for (Map<String, String> map : dto.getNewSamples()) {

            String jsonString = JSON.toJSONString(map);
            SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);

            if (StringUtils.hasText(sample.getSampleKind())) {
                return Result.fail("缺少样品种类");
            }

            TMeasureSample newSample = new TMeasureSample();
            BeanUtils.copyProperties(sample, newSample);
            newSample.setId(sample.getSampleId());

            if (dto.getHeads() != null) {
                JSONObject newColData = new JSONObject();

                for (HeadVO head : dto.getHeads()) {
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
            return Result.fail("缺少参数");
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
                        .eq(TMeasureData::getTaskId, task.getId());
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
                    .eq(TMeasureData::getTaskId, task.getId());
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
    public Result<MethodParam> queryMethodParam(String methodId) {
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

        MethodParam param = new MethodParam();
        BeanUtils.copyProperties(rationParameter, param);
        param.setRationParam(rationParameter);
        param.setMethodId(method.getId());
        param.setMethodName(method.getMethodName());
        param.setMeasurementType(method.getMeasurementType());
        return Result.ok(param);
    }

    //标准样品表头
    @Override
    public Result<List<HeadVO>> getStandSampleTableHead(String taskId, String methodId, Boolean allDisplay) {

        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(methodId)) {
            return Result.fail("缺少参数");
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }
        List<HeadVO> ans = new ArrayList<>();
        //根据任务中记录的样品id判断是否存在空白样品
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task != null && StringUtils.hasText(task.getSampleKey())) {
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen specimen : taskSpecimen) {
                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                if (sample!=null && StringUtils.hasText(sample.getSampleType())) {
                    if (sample.getSampleType().equals(SampleTypeEnum.empty.getIdentity())){
                        ans.add(new HeadVO(SampleTableEnum.sumAbs.getName(), SampleTableEnum.sumAbs.getIdentity()));
                    }
                }
            }
        }
        ans.add(new HeadVO(SampleTableEnum.sampleName.getName(), SampleTableEnum.sampleName.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.sampleNumber.getName(), SampleTableEnum.sampleNumber.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.sampleType.getName(), SampleTableEnum.sampleType.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.sampleKind.getName(), SampleTableEnum.sampleKind.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.normalConcentration.getName(), SampleTableEnum.normalConcentration.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.sampleLocation.getName(), SampleTableEnum.sampleLocation.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.Abs.getName(), SampleTableEnum.Abs.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.samplingTime.getName(), SampleTableEnum.samplingTime.getIdentity()));

        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
        List<String> waveLengths = parameter.getWaveLength();

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

        return Result.ok(ans);

    }

    //未知样品表头
    @Override
    public Result<List<HeadVO>> getUnknownSampleTableHead(String taskId, String methodId, Boolean allDisplay) {

        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(methodId)) {
            return Result.fail("缺少参数");
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }
        List<HeadVO> ans = new ArrayList<>();
        //根据任务中记录的样品id判断是否存在空白样品
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task != null && StringUtils.hasText(task.getSampleKey())) {
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen specimen : taskSpecimen) {
                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                if (sample!=null && StringUtils.hasText(sample.getSampleType())) {
                    if (sample.getSampleType().equals(SampleTypeEnum.empty.getIdentity())){
                        ans.add(new HeadVO(SampleTableEnum.sumAbs.getName(), SampleTableEnum.sumAbs.getIdentity()));
                    }
                }
            }
        }
        ans.add(new HeadVO(SampleTableEnum.sampleName.getName(), SampleTableEnum.sampleName.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.sampleNumber.getName(), SampleTableEnum.sampleNumber.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.sampleType.getName(), SampleTableEnum.sampleType.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.dilutionRatio.getName(), SampleTableEnum.dilutionRatio.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.amount.getName(), SampleTableEnum.amount.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.sampleKind.getName(), SampleTableEnum.sampleKind.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.fitConcentration.getName(), SampleTableEnum.fitConcentration.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.sampleLocation.getName(), SampleTableEnum.sampleLocation.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.Abs.getName(), SampleTableEnum.Abs.getIdentity()));
        ans.add(new HeadVO(SampleTableEnum.samplingTime.getName(), SampleTableEnum.samplingTime.getIdentity()));

        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
        List<String> waveLengths = parameter.getWaveLength();

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
        if (parameter.getResultParams() != null && !parameter.getResultParams().isEmpty()) {

            for (PreCountResultParam param : parameter.getResultParams()) {
                StringBuilder s2 = new StringBuilder();
                String s1 = param.getResultName() + "(" + param.getUnit() + ")";
                s2.append(param.getResultIdentity()).append("-").append(param.getFormulaText());
                HeadVO headVO = new HeadVO(s1, s2.toString());
                ans.add(headVO);
            }
        }
        return Result.ok(ans);
    }

    @Override
    public Result<List<Map<String, String>>> queryData(String taskId, Boolean allDisplay, String sampleKind) {

        if (!StringUtils.hasText(taskId) || allDisplay == null || !StringUtils.hasText(sampleKind)) {
            return Result.fail("缺少参数");
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<Map<String, String>> ans = new ArrayList<>();

        if (sampleKind.equals(stand)) {

            for (TaskSpecimen specimen : taskSpecimen) {
                if (!allDisplay && specimen.getDeleteType().equals("1")) {
                    continue;
                }

                Map<String, String> map = new HashMap<>();
                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                if (sample == null || !sample.getSampleKind().equals(sampleKind)) {
                    //不是需要的样品，跳过
                    continue;
                }


                if (sample != null && StringUtils.hasText(sample.getSampleType())) {
                    if (sample.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {
                        map.put(SampleTableEnum.sumAbs.getIdentity(), "va");
                    }
                }

                map.put("sampleId", specimen.getSpecimenKey());
                map.put("dilutionRatio", sample.getDilutionRatio());
                map.put("sampleVolume", sample.getSampleVolume());
                map.put("deleteType", specimen.getDeleteType());

                map.put(SampleTableEnum.sampleName.getIdentity(), sample.getSampleName());
                map.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber());
                map.put( SampleTableEnum.sampleType.getIdentity(), sample.getSampleType());
                map.put(SampleTableEnum.sampleLocation.getIdentity(), sample.getSampleLocation());
                map.put( SampleTableEnum.sampleKind.getIdentity(), sample.getSampleKind());
                map.put(SampleTableEnum.normalConcentration.getIdentity(), sample.getNormalConcentration());



                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getTaskId, taskId)
                        .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey());
                TMeasureData data = tMeasureDataService.getOne(queryWrapper);

                if (data != null) {
                    List<PhotometricOriginDataInfo> wlDataList = JSON.parseArray(data.getOriginalCalculationData(), PhotometricOriginDataInfo.class);

                    for (PhotometricOriginDataInfo wlData : wlDataList) {
                        map.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), wlData.getAbsValue());
                        map.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), wlData.getTPercentValue());
                        map.put("remark", data.getRemark());
                        map.put("samplingTime", data.getSamplingTime());

                    }
                }

                LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                        .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey());
//                        .orderByDesc();

                TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                if (one != null) {
                    List<PhotometricCountDataInfo> countList = JSON.parseArray(one.getAnalysisResult(), PhotometricCountDataInfo.class);

                    for (PhotometricCountDataInfo count : countList) {
                        map.put(count.getResultIdentity() + "-" + count.getFormulaText(), count.getCountValue());

                    }

                }
                ans.add(map);
            }
            return Result.ok(ans);




        } else if (sampleKind.equals(unknown)) {

        }

        return null;

    }


}
