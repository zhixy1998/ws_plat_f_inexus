package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.dao.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.AddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricMethodParameterDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricParameter;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.*;
import com.persee.workstation.uv_inexus.po.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.standardPo.TReportStandardDetection;
import com.persee.workstation.uv_inexus.po.standardPo.TReportStandardPretreatment;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.measurementService.*;
import com.persee.workstation.uv_inexus.service.standardService.TReportStandardDetectionService;
import com.persee.workstation.uv_inexus.service.standardService.TReportStandardPretreatmentService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricCountDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricOriginDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricSampleAndDataVO;
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

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("photometricService")
public class PhotometricServiceImpl implements PhotometricService {
    @Autowired
    private TMeasureProjectService tMeasureProjectService;
    @Autowired
    private TMeasureTaskService tMeasureTaskService;
    @Autowired
    private TMeasureSampleService tMeasureSampleService;
    @Autowired
    private SystemUserManageDAO userManageDAO;
    @Autowired
    private TMeasureMethodService tMeasureMethodService;
    @Autowired
    private TMeasureDataService tMeasureDataService;

    @Autowired
    private TMeasureAnalysisDataService tMeasureAnalysisDataService;

    @Autowired
    private TReportStandardDetectionService tReportStandardDetectionService;

    @Autowired
    private TReportStandardPretreatmentService tReportStandardPretreatmentService;

    @Value("1")
    String deleteType;

    @Value("0")
    String createType;


    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, PhotometricParameter photometricParameter) {

        if (photometricParameter.getWaveLengths() == null || photometricParameter.getWaveLengths().isEmpty()) {
            return Result.fail("请填写波长值");
        }

        // 创建测量方法表
        TMeasureMethod tMeasureMethod = new TMeasureMethod();
        tMeasureMethod.setProjectItemKey(methodDto.getProjectId());
        tMeasureMethod.setMethodName(methodDto.getMethodName());
        tMeasureMethod.setMeasurementType(methodDto.getMethodType());
        tMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(photometricParameter));
        tMeasureMethod.setMethodCreator(updateBy);
        tMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        boolean save = tMeasureMethodService.save(tMeasureMethod);
        if (save) {
            return Result.ok("创建成功");
        } else {
            return Result.fail("创建失败");
        }
    }

    @Override
    public Result updateMethodParameter(PhotometricMethodParameterDto methodDto) {
        // 根据方法id查询当前方法
        if (!StringUtils.hasText(methodDto.getMethodId())) {
            return Result.fail("缺少方法id");
        }
        TMeasureMethod method = tMeasureMethodService.getById(methodDto.getMethodId());

    // 将ExperimentMethodDto中的值转入SpectrumParameter对象中
        PhotometricParameter param = new PhotometricParameter();
        BeanUtils.copyProperties(methodDto, param);

        String s = JSON.toJSONString(param);
//            String s = new ObjectMapper().writeValueAsString(param);
        method.setMeasurementParamInfo(s);

        if (!methodDto.getMethodName().equals(method.getMethodName())) {
            method.setMethodName(methodDto.getMethodName());
        }
        boolean update = tMeasureMethodService.updateById(method);

        return Result.ok(update);
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
    public Result addOrDeleteSample(AddOrDeleteSampleDTO dto) {

        if (!StringUtils.hasText(dto.getMethodId())) {
            return Result.fail("方法ID不能为空");
        }
        if (dto.getNewSamples().isEmpty() && dto.getDelSampleIds().isEmpty()) {
            return Result.ok("没有要新增或删除的样品！");
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
        if (task == null) {
            return Result.fail("任务不存在");
        }

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

        for (SampleVO sample : dto.getNewSamples()) {
            TMeasureSample newSample = new TMeasureSample();
            BeanUtils.copyProperties(sample, newSample);

            if (StringUtils.hasText(newSample.getId())) {
                tMeasureSampleService.updateById(newSample);
            } else {
                TaskSpecimen specimen = new TaskSpecimen();
                specimen.setDeleteType(createType);
                taskSpecimen.add(specimen);
                log.info("新增的样品：{}", newSample);
                tMeasureSampleService.save(newSample);
                specimen.setSpecimenKey(newSample.getId());
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
    public Result<List<SampleVO>> querySampleInfo(String taskId) {
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
                SampleVO vo = new SampleVO();
                BeanUtils.copyProperties(measureSample, vo);
                vo.setSampleId(measureSample.getId());
                sampleVOList.add(vo);
            }
        }

        return Result.ok(sampleVOList);
    }

    @Override
    public Result<MethodParam> queryMethodParam(String methodId) {

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }

        if (!StringUtils.hasText(method.getMeasurementParamInfo())) {
            return Result.fail("未查询到该方法参数");
        }
        PhotometricParameter photometricParameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);

        MethodParam param = new MethodParam();
        param.setMethodId(method.getId());
        param.setMethodName(method.getMethodName());
        param.setMeasurementType(method.getMeasurementType());
        param.setPhotometricParameter(photometricParameter);
        return Result.ok(param);
    }

    @Override
    public Result queryStandardInfo(String standardName) {

        LambdaQueryWrapper<TReportStandardDetection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(TReportStandardDetection::getStandardName, standardName);
        List<TReportStandardDetection> list = tReportStandardDetectionService.list(queryWrapper);
        for (TReportStandardDetection detection : list) {
            LambdaQueryWrapper<TReportStandardPretreatment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TReportStandardPretreatment::getDetectionId, detection.getId());
            List<TReportStandardPretreatment> pretreatmentList = tReportStandardPretreatmentService.list(wrapper);
            detection.setPretreatments(pretreatmentList);
        }

        return Result.ok(list);
    }

    @Override
    public Result<List<HeadVO>> tableHead(String taskId, String methodId, Boolean allDisplay) {
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }
        PhotometricParameter Parameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
        List<String> waveLengths = Parameter.getWaveLengths();
        List<HeadVO> ans = new ArrayList<>();

        if (waveLengths != null && !waveLengths.isEmpty()) {

            for (String waveLength : waveLengths) {
                StringBuilder s1 = new StringBuilder();
                StringBuilder s2 = new StringBuilder();
                if (!allDisplay) {
                    String luminosity = Parameter.getLuminosity();
                    String name = WaveLengthEnum.getName(luminosity);

                    s1.append(waveLength).append(name);
                    s2.append(waveLength).append("-").append(luminosity);
                    ans.add(new HeadVO(s1.toString(), s2.toString()));
                    continue;
                }

                s1.append(waveLength).append(WaveLengthEnum.abs.getName());
                s2.append(waveLength).append("-").append(WaveLengthEnum.abs.getCode());
                HeadVO headVO1 = new HeadVO(s1.toString(), s2.toString());
                ans.add(headVO1);

                s1.setLength(0);
                s2.setLength(0);
                s1.append(waveLength).append(WaveLengthEnum.tPercent.getName());
                s2.append(waveLength).append("-").append(WaveLengthEnum.tPercent.getCode());
                HeadVO headVO2 = new HeadVO(s1.toString(), s2.toString());
                ans.add(headVO2);
            }
        }
        if (Parameter.getResultParams() != null && !Parameter.getResultParams().isEmpty()) {

            for (PreCountResultParam param : Parameter.getResultParams()) {
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
    public Result<List<PhotometricSampleAndDataVO>> queryDataInfo(String taskId) {
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<PhotometricSampleAndDataVO> list = new ArrayList<>();

        for (TaskSpecimen specimen : taskSpecimen) {
            PhotometricSampleAndDataVO sampleAndDataVO = new PhotometricSampleAndDataVO();
            sampleAndDataVO.setTaskId(taskId);
            sampleAndDataVO.setSampleId(specimen.getSpecimenKey());

            TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
            sampleAndDataVO.setSample(sample);

            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getTaskId, taskId)
                    .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey());
            TMeasureData data = tMeasureDataService.getOne(queryWrapper);

            if (data != null) {
                List<PhotometricOriginDataInfo> wlDataList = JSON.parseArray(data.getOriginalCalculationData(), PhotometricOriginDataInfo.class);
                sampleAndDataVO.setWavelengthData(wlDataList);
            }

            LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
            analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                    .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey());

            TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);

            if (one != null) {
                List<PhotometricCountDataInfo> countList = JSON.parseArray(one.getAnalysisResult(), PhotometricCountDataInfo.class);
                sampleAndDataVO.setCountData(countList);
            }

            list.add(sampleAndDataVO);

        }
        return Result.ok(list);
    }


    @Override
    public Result<List<Map<String, String>>> queryData(String taskId, Boolean allDisplay) {
        if (!StringUtils.hasText(taskId) || allDisplay == null) {
            return Result.fail("缺少参数");
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<Map<String, String>> ans = new ArrayList<>();

        for (TaskSpecimen specimen : taskSpecimen) {
            if (!allDisplay && specimen.getDeleteType().equals("1")) {
                continue;
            }

            Map<String, String> map = new HashMap<>();
            TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

            map.put("sampleId", specimen.getSpecimenKey());
            map.put("sampleName", sample.getSampleName());
            map.put("sampleNumber", sample.getSampleNumber());
            map.put("sampleType", sample.getSampleType());
            map.put("dilutionRatio", sample.getDilutionRatio());
            map.put("sampleVolume", sample.getSampleVolume());
            map.put("deleteType", specimen.getDeleteType());
            map.put("sampleLocation", sample.getSampleLocation());

            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getTaskId, taskId)
                    .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey());
            TMeasureData data = tMeasureDataService.getOne(queryWrapper);

            if (data != null) {
                List<PhotometricOriginDataInfo> wlDataList = JSON.parseArray(data.getOriginalCalculationData(), PhotometricOriginDataInfo.class);

                for (PhotometricOriginDataInfo wlData : wlDataList) {
                    map.put(wlData.getWavelength()+ "-" + WaveLengthEnum.abs.getCode(), wlData.getAbsValue());
                    map.put(wlData.getWavelength()+ "-" + WaveLengthEnum.tPercent.getCode(), wlData.getTPercentValue());
                    map.put("remark",data.getRemark() );
                    map.put("samplingTime",data.getSamplingTime() );

                }
            }

            LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
            analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                    .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey());

            TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
            if (one != null) {
                List<PhotometricCountDataInfo> countList = JSON.parseArray(one.getAnalysisResult(), PhotometricCountDataInfo.class);

                for (PhotometricCountDataInfo count : countList) {
                    map.put(count.getResultIdentity()+ "-" + count.getFormulaText(), count.getCountValue());

                }

            }
            ans.add(map);
        }
        return Result.ok(ans);
    }

    @Override
    public Result savePhotometricMeasureData(LocalDateTime startTime, LocalDateTime endTime, List<PhotometricSampleAndDataVO> vo) {
        for (PhotometricSampleAndDataVO sampleAndDataVO : vo) {
            String taskId = sampleAndDataVO.getTaskId();
            String sampleId = sampleAndDataVO.getSampleId();
            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getTaskId, taskId)
                    .eq(TMeasureData::getSampleKey, sampleId);
            TMeasureData data = tMeasureDataService.getOne(queryWrapper);
            if (data != null) {
                //认为是重测，覆盖数据
                data.setInstrumentData(JSON.toJSONString(sampleAndDataVO.getMachineData()));
                data.setOriginalCalculationData(JSON.toJSONString(sampleAndDataVO.getWavelengthData()));
                tMeasureDataService.updateById(data);
            } else {
                TMeasureData tMeasureData = new TMeasureData();
                tMeasureData.setTaskId(taskId);
                tMeasureData.setSampleKey(sampleId);
                tMeasureData.setInstrumentData(JSON.toJSONString(sampleAndDataVO.getMachineData()));
                tMeasureData.setOriginalCalculationData(JSON.toJSONString(sampleAndDataVO.getWavelengthData()));
                tMeasureData.setMeasurementStartTime(startTime);
                tMeasureData.setMeasurementEndTime(endTime);
                tMeasureData.setIsDelete(createType);
                tMeasureDataService.save(tMeasureData);
            }
        }

        return Result.ok(true);
    }

    @Override
    public Result saveDataInfo(PhotometricSampleAndDataVO vo) {

        String taskId = vo.getTaskId();
        String sampleId = vo.getSampleId();
        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getSampleKey, sampleId);
        TMeasureData data = tMeasureDataService.getOne(queryWrapper);
        if (data == null || data.getIsDelete().equals(deleteType)) {
            log.info("数据不存在或者已删除");
            return Result.fail("数据不存在或已删除");
        }

        data.setRemark(vo.getRemark());
        data.setSamplingTime(vo.getSampleTime());
        tMeasureDataService.updateById(data);

        return Result.ok( true);
    }

    @Override
    public void test(String taskId, String methodId) {
        List<String> waveLengths = new ArrayList<>();
        waveLengths.add("400");
        waveLengths.add("500");
        waveLengths.add("600");

        TMeasureData data = new TMeasureData();
        data.setTaskId(taskId);
        data.setRemark("test");
        data.setIsDelete("0");

        TMeasureMethod byId = tMeasureMethodService.getById(methodId);
        PhotometricParameter parameter = JSON.parseObject(byId.getMeasurementParamInfo(), PhotometricParameter.class);
//        parameter.setWaveLengths(waveLengths);
//        String jsonString = JSON.toJSONString(parameter);
//        byId.setMeasurementParamInfo(jsonString);
//       tMeasureMethodService.updateById(byId);
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen specimen : taskSpecimen) {
            LambdaQueryWrapper<TMeasureData> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TMeasureData::getTaskId, taskId)
                    .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey());
            TMeasureData one = tMeasureDataService.getOne(wrapper);

            List<PhotometricOriginDataInfo> orData = new ArrayList<>();
            List<PhotometricCountDataInfo> list = new ArrayList<>();
            for (String length : waveLengths) {
                PhotometricOriginDataInfo originDataInfo = new PhotometricOriginDataInfo();

                originDataInfo.setWavelength(length);

                Random random = new Random();
                // 生成[0,1)之间的随机数
                double randomValue = random.nextDouble();
                // 格式化为三位小数
                DecimalFormat df = new DecimalFormat("0.000");
                String formattedValue = df.format(randomValue);

                String s = DataCountUtil.changeTPercentOrAbs(formattedValue, null);

                originDataInfo.setAbsValue(formattedValue);
                originDataInfo.setTPercentValue(s);
                originDataInfo.setMode("1");
                orData.add(originDataInfo);

                List<PreCountResultParam> resultParams = parameter.getResultParams();

                for (PreCountResultParam resultParam : resultParams) {
                    PhotometricCountDataInfo countDataInfo = new PhotometricCountDataInfo();
                    BeanUtils.copyProperties(resultParam, countDataInfo);
                    double i = Double.parseDouble(s);
                    countDataInfo.setCountValue(df.format(i * 4L /3));
                    list.add(countDataInfo);

                }
            }

            if (one == null) {
                one = new TMeasureData();
                one.setSampleKey(specimen.getSpecimenKey());
                one.setIsDelete("0");
                one.setTaskId(taskId);
                one.setOriginalCalculationData(JSON.toJSONString(orData));
                tMeasureDataService.save(one);
            } else {
                one.setOriginalCalculationData(JSON.toJSONString(orData));
                tMeasureDataService.updateById(one);
            }

            LambdaQueryWrapper<TMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId).eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey());

            TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(queryWrapper);
            if (analysisData == null){
                analysisData = new TMeasureAnalysisData();
                analysisData.setTaskId(taskId);
                analysisData.setSampleKey(specimen.getSpecimenKey());
                analysisData.setAnalysisParam(JSON.toJSONString(list));
                tMeasureAnalysisDataService.save(analysisData);
            } else {
                analysisData.setAnalysisParam(JSON.toJSONString(list));
                tMeasureAnalysisDataService.updateById(analysisData);
            }
        }



    }


}
