package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.dao.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.AddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricMethodParameterDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricParameter;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.*;
import com.persee.workstation.uv_inexus.po.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.standardPo.TReportStandardDetection;
import com.persee.workstation.uv_inexus.po.standardPo.TReportStandardPretreatment;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.measurementService.*;
import com.persee.workstation.uv_inexus.service.standardService.TReportStandardDetectionService;
import com.persee.workstation.uv_inexus.service.standardService.TReportStandardPretreatmentService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.util.pythonUtil.RestUtil;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricCountDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricOriginDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricSampleAndDataVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.PreCountResultParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
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

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private DevMessageUtil devMessageUtil;

    @Value("1")
    String deleteType;

    @Value("0")
    String createType;

    @Value("0")
    String noTested;

    @Value("1")
    String tested;


    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, PhotometricParameter photometricParameter) {

        if (photometricParameter.getWaveLength() == null || photometricParameter.getWaveLength().isEmpty()) {
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
        if (taskList != null && !taskList.isEmpty()) {
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
        if (list == null || list.isEmpty()) {
            LambdaQueryWrapper<TMeasureProject> projectWrapper = new LambdaQueryWrapper<>();
            projectWrapper.eq(TMeasureProject::getCreatorKey, updateBy)
                    .orderByDesc(TMeasureProject::getCreateTime);
            List<TMeasureProject> projectList = tMeasureProjectService.list(projectWrapper);
            if (projectList == null || projectList.isEmpty()) {
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
        if(dto.getNewSamples() == null) {
            dto.setNewSamples(new ArrayList<>());
        }
        if (dto.getDelSampleIds() == null) {
            dto.setDelSampleIds(new ArrayList<>());
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
            newSample.setId(sample.getSampleId());

            if (StringUtils.hasText(newSample.getId())) {
                tMeasureSampleService.updateById(newSample);
            } else {
                TaskSpecimen specimen = new TaskSpecimen();
                specimen.setDeleteType(createType);
                specimen.setIsTested(noTested);
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
        BeanUtils.copyProperties(photometricParameter, param);
        param.setMethodId(method.getId());
        param.setMethodName(method.getMethodName());
        param.setMeasurementType(method.getMeasurementType());
//        param.setPhotometricParameter(photometricParameter);
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
    public R<PhotometricReportDataVO> getReportCount(String taskId) {

        if (!StringUtils.hasText(taskId)) {
            return R.fail("缺少任务id");
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return R.fail("任务不存在");
        }
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        PhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);

        if (parameter.getResultParams() == null || parameter.getResultParams().isEmpty()) {
            return R.fail("缺少结果参数");
        }

        List<HeadVO> heads = new ArrayList<>();

        List<Map<String, String>> data = new ArrayList<>();
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
                Map<String, String> map = new HashMap<>();
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
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }
        PhotometricParameter Parameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
        List<String> waveLengths = Parameter.getWaveLength();
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
                    .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                    .orderByDesc(TMeasureData::getSerialNumber);
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
    public R<List<PhotometricCountDataInfo>> formula(List<PhotometricOriginDataInfo> vo, String sampleId, String methodId) {
        TMeasureSample sample = tMeasureSampleService.getById(sampleId);
        TMeasureMethod method = tMeasureMethodService.getById(methodId);

        if (!StringUtils.hasText(method.getMeasurementParamInfo())) {
            return R.fail("未查询到该方法参数");
        }

        PhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);

        List<PreCountResultParam> resultParams = parameter.getResultParams();

        StringBuilder values = new StringBuilder();
        //稀释倍数
        String dilutionRatio = sample.getDilutionRatio();
        //取样量
        String sampleVolume = sample.getSampleVolume();

        StringBuilder identity = new StringBuilder();

        for (PhotometricOriginDataInfo info : vo) {
            identity.append(info.getWavelength()).append("abs").append(",").append(info.getWavelength()).append("t").append(",");
            values.append(info.getAbsValue()).append(",").append(info.getTPercentValue()).append(",");
        }
        identity.append("t").append(",").append("m");
        values.append("4").append(",").append("5");

        List<PhotometricCountDataInfo> countList = new ArrayList<>();
        for (PreCountResultParam param : resultParams) {
            String[] split = param.getFormulaText().split("=");
            String text = split[split.length - 1];
            String result = RestUtil.sum(text, identity.toString(), values.toString());
            PhotometricCountDataInfo countDataInfo = new PhotometricCountDataInfo();
            BeanUtils.copyProperties(param, countDataInfo);
            JSONObject jsonObject = JSONObject.parseObject(result);
            countDataInfo.setCountValue(jsonObject.get("result").toString());
            countList.add(countDataInfo);
        }

        return R.ok(countList);
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
                    map.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), wlData.getAbsValue());
                    map.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), wlData.getTPercentValue());
                    map.put("remark", data.getRemark());
                    map.put("samplingTime", data.getSamplingTime());

                }
            }

            LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
            analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                    .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey());

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
    }

    @Override
    public Result savePhotometricMeasureData(PhotometricSampleAndDataVO sampleAndDataVO) {

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
            data.setSamplingTime(sampleAndDataVO.getSampleTime());
            tMeasureDataService.updateById(data);
        } else {
            TMeasureData tMeasureData = new TMeasureData();
            tMeasureData.setTaskId(taskId);
            tMeasureData.setSampleKey(sampleId);
            tMeasureData.setInstrumentData(JSON.toJSONString(sampleAndDataVO.getMachineData()));
            tMeasureData.setOriginalCalculationData(JSON.toJSONString(sampleAndDataVO.getWavelengthData()));
            tMeasureData.setMeasurementStartTime(sampleAndDataVO.getStartTime());
            tMeasureData.setMeasurementEndTime(sampleAndDataVO.getEndTime());
            tMeasureData.setSamplingTime(sampleAndDataVO.getSampleTime());
            tMeasureData.setIsDelete(createType);
            tMeasureDataService.save(tMeasureData);
        }

        LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                .eq(TMeasureAnalysisData::getSampleKey, sampleId);
        TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (one != null) {
            one.setAnalysisResult(JSON.toJSONString(sampleAndDataVO.getCountData()));
            one.setIsDelete(createType);
            tMeasureAnalysisDataService.save(one);
        } else {
            TMeasureAnalysisData analysisData = new TMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setAnalysisResult(JSON.toJSONString(sampleAndDataVO.getCountData()));
            analysisData.setIsDelete(createType);
            tMeasureAnalysisDataService.save(analysisData);
        }
        return Result.ok(true);
    }

    @Override
    public Result saveDataInfo(RemarkChangeSaveVO vo) {
        if (!StringUtils.hasText(vo.getTaskId())) {
            return Result.fail("缺少任务id");
        }

        if (!StringUtils.hasText(vo.getSampleId())) {
            return Result.fail("缺少样品id");
        }

        if (!StringUtils.hasText(vo.getRemark())) {
            return Result.fail("缺少备注");
        }

        String taskId = vo.getTaskId();
        String sampleId = vo.getSampleId();
        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getSampleKey, sampleId)
                .orderByDesc(TMeasureData::getSerialNumber);
        List<TMeasureData> list = tMeasureDataService.list(queryWrapper);
        TMeasureData data = new TMeasureData();
        if (list != null || !list.isEmpty()) {
            data = list.get(0);
            data.setRemark(vo.getRemark());
        } else {
            data.setTaskId(taskId);
            data.setSampleKey(sampleId);
            data.setSerialNumber(1);
            data.setRemark(vo.getRemark());
        }
        tMeasureDataService.updateById(data);

        return Result.ok(true);
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
                    countDataInfo.setCountValue(df.format(i * 4L / 3));
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
            if (analysisData == null) {
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

    @Override
    public Result zero(String serialNumber, String methodId) {

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }
        PhotometricParameter photometricParameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
        if (photometricParameter.getWaveLength() == null || photometricParameter.getWaveLength().isEmpty()) {
            return Result.fail("缺少波长");
        }
        List<String> waveLength = photometricParameter.getWaveLength();
        // 通过序列号获取ip
        String ip = deviceInfoService.getIp(serialNumber);
        List<ChangeParam> list = new ArrayList<>();
        for (String s : waveLength) {
            Double v = Double.valueOf(s);
            Command command = new Command();
            command.setCmdId(SetParamEnum.SetWl.getComId());
            command.setParams(new Object[]{v});
            devMessageUtil.wavelengthLocate(command, ip);
            Command com = new Command();
            com.setCmdId(SetParamEnum.Zero.getComId());
            com.setParams(new Object[]{});
            R<ChangeParam> zero = devMessageUtil.zero(com, ip);
            if (!zero.getSuccess()) {
                return Result.fail(zero.getMessage());
            }
            ChangeParam changeParam = zero.getData();
            changeParam.setWl(s);
            list.add(changeParam);
        }
        photometricParameter.setChanageParams(list);
        method.setMeasurementParamInfo(JSON.toJSONString(photometricParameter));
        boolean update = tMeasureMethodService.updateById(method);
        return update ? Result.ok() : Result.fail();
    }

    @Override
    public Result<Boolean> saveButton(String taskId, String updateBy) {
        if (!StringUtils.hasText(taskId)) {
            return Result.fail("未创建任务，无法保存");
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        SystemUserManagePO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SystemUserManagePO.class).eq(SystemUserManagePO::getUserName, updateBy));
        String id = po.getId();
        task.setUserId(id);
        task.setIsDelete("1");
        tMeasureTaskService.updateById(task);
        return Result.ok(true);
    }

    @Override
    public Result<MeasurementCurrentInfoVO> getInfoByTaskId(String taskId) {
        if (!StringUtils.hasText(taskId)){
            return Result.fail("缺少任务id");
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        MeasurementCurrentInfoVO measurementCurrentInfoVO = new MeasurementCurrentInfoVO();
        measurementCurrentInfoVO.setTaskId(taskId);
        measurementCurrentInfoVO.setTaskName(task.getTaskName());
        measurementCurrentInfoVO.setMethodId(task.getMethodKey());
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        measurementCurrentInfoVO.setMethodName(method.getMethodName());
        measurementCurrentInfoVO.setProjectId(task.getProjectItemKey());
        TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
        measurementCurrentInfoVO.setProjectName(project.getProjectName());

        return Result.ok(measurementCurrentInfoVO);
    }

    @Override
    public R<File> downloadWord(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            return R.fail("缺少任务id");
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return R.fail("任务不存在");
        }

        String fileName = task.getTaskName() + ".docx";

        String methodId = task.getMethodKey();
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        List<HeadVO> heads = new ArrayList<>();

        heads.add(new HeadVO("样品编号", "sampleNumber"));
        heads.add(new HeadVO("样品名称", "sampleName"));
        heads.add(new HeadVO("样品类型", "sampleType"));
        heads.add(new HeadVO("稀释比", "dilutionRatio"));
        heads.add(new HeadVO("样品体积", "sampleVolume"));
        heads.add(new HeadVO("样品位置", "sampleLocation"));

        List<HeadVO> vos = tableHead(taskId, methodId, false).getData();
        heads.addAll(vos);
        heads.add(new HeadVO("备注", "remark"));
        heads.add(new HeadVO("采样时间", "samplingTime"));

        List<Map<String, String>> data = queryData(taskId, false).getData();


        XWPFDocument doc = new XWPFDocument();

        XWPFParagraph paragraph = doc.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText("任务：" + task.getTaskName() + "方法" + method.getMethodName() + "测试数据");
        XWPFTable table = doc.createTable();

        table.setWidth("100%");
        int i = 0;
        for (Map<String, String> map : data) {
            XWPFTableRow headerRow = table.getRow(0);
            if(headerRow != null) {
                XWPFTableCell cell = headerRow.getCell(0);
                if(cell != null) {
                    cell.setText("序号");
                }

                XWPFTableRow row = table.createRow();
                int m = 1;
                for (HeadVO head : heads) {

                    String cellData = map.get(head.getIdentity());

                    if (head.getIdentity().equals("sampleType")) {
                        cellData = SampleTypeEnum.getName(cellData);
                    }
                    if (head.getIdentity().equals("sampleLocation")) {
                        cellData = "样品"+cellData;
                    }
                    if (i == 0) {
                        headerRow.addNewTableCell().setText(head.getName());
                        row.addNewTableCell().setText(cellData);
                        row.getCell(0).setText(String.valueOf(1));
                    } else {
                        row.getCell(m).setText(cellData);
                        row.getCell(0).setText(String.valueOf(i+1));
                        m++;
                    }
                }
                i++;
            }
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    XWPFParagraph paragraph1=cell.getParagraphArray(0);
                    paragraph1.setAlignment(ParagraphAlignment.CENTER);
                    cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                }
            }
        }

        try {
            FileOutputStream out = new FileOutputStream(fileName);
            doc.write(out);
            out.close();
            return R.ok(new File(fileName));
        } catch (Exception e) {
            return R.fail(e.getMessage());

        }
    }


}
