package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskHeadSnapshot;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.standardService.TReportStandardDetectionService;
import com.persee.workstation.uv_inexus.service.guide.standardService.TReportStandardPretreatmentService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.util.pythonUtil.RestUtil;
import com.persee.workstation.uv_inexus.vo.measurementVo.*;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.*;
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

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTableEnum.sampleId;

@Slf4j
@Service("photometricService")
public class PhotometricServiceImpl implements PhotometricService {
    @Autowired
    TMeasureProjectService tMeasureProjectService;
    @Autowired
    TMeasureTaskService tMeasureTaskService;
    @Autowired
    TMeasureSampleService tMeasureSampleService;
    @Autowired
    SystemUserManageDAO userManageDAO;
    @Autowired
    TMeasureMethodService tMeasureMethodService;
    @Autowired
    TMeasureDataService tMeasureDataService;

    @Autowired
    TMeasureAnalysisDataService tMeasureAnalysisDataService;

    @Autowired
    TReportStandardDetectionService tReportStandardDetectionService;

    @Autowired
    TReportStandardPretreatmentService tReportStandardPretreatmentService;

    @Autowired
    TMeasureTaskAttachmentService tMeasureTaskAttachmentService;

    @Autowired
    DeviceInfoService deviceInfoService;

    @Autowired
    DevMessageUtil devMessageUtil;

    @Autowired
    RestUtil restUtil;

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
            return Result.fail("缺少方法");
        }
        // 根据方法id查询方法
        TMeasureMethod measureMethod = tMeasureMethodService.getById(methodDto.getMethodId());
        if (measureMethod==null){
            return Result.fail("方法不存在");
        }
        // 将前端传过来的值复制到SpectrumParameter类中
        PhotometricParameter parameter = new PhotometricParameter();
        BeanUtils.copyProperties(methodDto, parameter);
        measureMethod.setMeasurementParamInfo(JSON.toJSONString(parameter));
        measureMethod.setMethodName(methodDto.getMethodName());
        boolean update = tMeasureMethodService.updateById(measureMethod);
        if (!update){
            return Result.fail("更新失败");
        }
        return Result.ok(measureMethod.getId());
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
            return Result.fail("请先创建方法");
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
        if (task == null) {
            return Result.fail("任务不存在");
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen specimen : taskSpecimen) {
            if (dto.getDelSampleIds().contains(specimen.getSpecimenKey())) {
                specimen.setDeleteType(deleteType);
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(TMeasureData::getTaskId, task.getId())
                        .orderByDesc(TMeasureData::getSerialNumber);
                List<TMeasureData> list = tMeasureDataService.list(queryWrapper);
                if (list != null && !list.isEmpty()) {
                    TMeasureData one = list.get(0);
                    one.setIsDelete(deleteType);
                    tMeasureDataService.updateById(one);
                }
            }
        }

        for (Map<String, String> map : dto.getNewSamples()) {
            String jsonString = JSON.toJSONString(map);
            SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);
            TMeasureSample newSample = new TMeasureSample();
            BeanUtils.copyProperties(sample, newSample);
            newSample.setId(sample.getSampleId());

            if (dto.getHeads() != null) {

                newSample.setHeads(JSON.toJSONString(dto.getHeads()));

                JSONObject newColData = new JSONObject();

                for (HeadVO head : dto.getHeads()) {
                    String s = map.get(head.getIdentity());
                    newColData.put(head.getIdentity(), s);
                }

                newSample.setNewColData(JSON.toJSONString(newColData));
            }

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
                         .orderByDesc(TMeasureData::getSerialNumber);
                List<TMeasureData> list = tMeasureDataService.list(queryWrapper);
                if (list != null && !list.isEmpty()) {
                    TMeasureData one = list.get(0);
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
                     .orderByDesc(TMeasureData::getSerialNumber);
            List<TMeasureData> list = tMeasureDataService.list(queryWrapper);
            if (list != null && !list.isEmpty()) {
                TMeasureData one = list.get(0);
                one.setIsDelete(createType);
                tMeasureDataService.updateById(one);
            }

        }
        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);

        return Result.ok(tMeasureTaskService.updateById(task));
    }

    @Override
    public Result<List<JSONObject>> newQuerySampleInfo(String taskId) {
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
    public Result<List<HeadVO>> querySampleHeads(String taskId) {

        LinkedHashSet<HeadVO> ans = new LinkedHashSet<>();

        if (!StringUtils.hasText(taskId)) {
            return Result.ok(new ArrayList<>(ans));
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        if (StringUtils.hasText(task.getSampleKey())) {
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen specimen : taskSpecimen) {
                if(specimen.getDeleteType().equals(deleteType)) {
                    continue;
                }
                TMeasureSample measureSample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                List<HeadVO> heads = JSON.parseArray(measureSample.getHeads(), HeadVO.class);

                if (heads != null) {
                    ans.addAll(heads);
                }
            }
        }
        return Result.ok(new ArrayList<>(ans));
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
        param.setLuminosityName(WaveLengthEnum.getName(photometricParameter.getLuminosity()));
        param.setSamplePoolWayName(RepeatConfigEnum.getName(photometricParameter.getSamplePoolWay()));
//        param.setPhotometricParameter(photometricParameter);
        return Result.ok(param);
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

        List<Map<String, Object>> data = new ArrayList<>();
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
                Map<String, Object> map = new HashMap<>();
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
        PhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
        List<String> waveLengths = parameter.getWaveLength();
        LinkedHashSet<HeadVO> ans = new LinkedHashSet<>();

        if (StringUtils.hasText(taskId)) {
            TMeasureTask task = tMeasureTaskService.getById(taskId);

            if (task != null && StringUtils.hasText(task.getTableInfo())) {
                TaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
                if (snapshot != null && snapshot.getTaskHeads() != null && !snapshot.getTaskHeads().isEmpty()) {
                    ans.addAll(snapshot.getTaskHeads());
                }
            }

            if (task != null && StringUtils.hasText(task.getSampleKey())) {

                List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
                for (TaskSpecimen specimen : taskSpecimen) {
                    TMeasureSample measureSample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                    if (StringUtils.hasText(measureSample.getHeads())){
                        List<HeadVO> heads = JSON.parseArray(measureSample.getHeads(), HeadVO.class);

                        if (heads != null && !heads.isEmpty()) {
                            ans.addAll(heads);
                        }
                    }
                }

            }
        }

        if (waveLengths != null && !waveLengths.isEmpty()) {

            for (String waveLength : waveLengths) {
                StringBuilder s1 = new StringBuilder();
                StringBuilder s2 = new StringBuilder();

                if (parameter.getLuminosity().equals(WaveLengthEnum.es.getCode()) || parameter.getLuminosity().equals(WaveLengthEnum.er.getCode())) {
                    String luminosity = parameter.getLuminosity();
                    String name = WaveLengthEnum.getName(luminosity);

                    s1.append(waveLength).append(name);
                    s2.append(waveLength).append("-").append(luminosity);
                    ans.add(new HeadVO(s1.toString(), s2.toString(), "measureData"));
                    continue;
                }

                if (!allDisplay) {
                    String luminosity = parameter.getLuminosity();
                    String name = WaveLengthEnum.getName(luminosity);

                    s1.append(waveLength).append(name);
                    s2.append(waveLength).append("-").append(luminosity);
                    ans.add(new HeadVO(s1.toString(), s2.toString(), "measureData"));
                    continue;
                }

                s1.append(waveLength).append(WaveLengthEnum.abs.getName());
                s2.append(waveLength).append("-").append(WaveLengthEnum.abs.getCode());
                HeadVO headVO1 = new HeadVO(s1.toString(), s2.toString(), "measureData");
                ans.add(headVO1);

                s1.setLength(0);
                s2.setLength(0);
                s1.append(waveLength).append(WaveLengthEnum.tPercent.getName());
                s2.append(waveLength).append("-").append(WaveLengthEnum.tPercent.getCode());
                HeadVO headVO2 = new HeadVO(s1.toString(), s2.toString(), "measureData");
                ans.add(headVO2);
            }
        }
        if (parameter.getResultParams() != null && !parameter.getResultParams().isEmpty()) {

            for (PreCountResultParam param : parameter.getResultParams()) {
                StringBuilder s2 = new StringBuilder();
                String s1 = param.getResultName() + "(" + param.getUnit() + ")";
                s2.append(param.getResultIdentity()).append("-").append(param.getFormulaText());
                HeadVO headVO = new HeadVO(s1, s2.toString(), "countData");
                ans.add(headVO);
            }
        }

        return Result.ok(new ArrayList<>(ans));
    }

    @Override
    public R<List<PhotometricCountDataInfo>> formula(List<PhotometricOriginDataInfo> vo, TMeasureSample sample, TMeasureTask task, PhotometricParameter parameter, Integer digit) {

        if (!StringUtils.hasText(task.getTableInfo())) {
            return R.fail("缺少分析参数，无法计算分析");
        }
        TaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
        if (snapshot.getMapRelation() == null || snapshot.getMapRelation().isEmpty()) {
            return R.fail("缺少映射关系，无法计算分析");
        }
        String jsonString = JSON.toJSONString(sample);
        JSONObject allData = JSON.parseObject(jsonString);
        if (StringUtils.hasText(sample.getNewColData())) {
            JSONObject object = JSONObject.parseObject(sample.getNewColData());
            allData.putAll(object);
        }
        LambdaQueryWrapper<TMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureAnalysisData::getTaskId, task.getId())
                .eq(TMeasureAnalysisData::getSampleKey, sample.getId())
                .orderByDesc(TMeasureAnalysisData::getDataVersion)
                .last("limit 1");
        TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(queryWrapper);
        if (one != null && StringUtils.hasText(one.getAnalysisParam())) {
            String analysisParam = one.getAnalysisParam();
            JSONObject object = JSON.parseObject(analysisParam);
            allData.putAll(object);
        }

        for (PhotometricOriginDataInfo dataInfo : vo) {
            if (dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                allData.putAll(dataInfo.getData());
            }
        }

        StringBuilder values = new StringBuilder();

        StringBuilder identity = new StringBuilder();

        for (InputParam inputParam : parameter.getSampleInputParams()) {
            boolean b = snapshot.getMapRelation().stream().anyMatch(
                    mapRelation -> inputParam.getResultIdentity().equals(mapRelation.getParamIdentity()));

            Optional<InputParamMapTableHead> mapTableHead = snapshot.getMapRelation().stream().filter(
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

        List<PhotometricCountDataInfo> countList = new ArrayList<>();
        for (PreCountResultParam param : parameter.getResultParams()) {
            String[] split = param.getFormulaText().split("=");
            String text = split[split.length - 1];
            String result = restUtil.sum(text, identity.toString(), values.toString());
            PhotometricCountDataInfo countDataInfo = new PhotometricCountDataInfo();
            BeanUtils.copyProperties(param, countDataInfo);
            JSONObject resultJson = JSONObject.parseObject(result);

            if (!resultJson.get("code").equals("0") && !resultJson.get("code").equals(0)) {
                return R.fail("公式计算出错！" + resultJson.get("message"));
            }
            countDataInfo.setCountValue(resultJson.get("result").toString());
            countList.add(countDataInfo);
        }

        return R.ok(countList);
    }


    @Override
    public Result<List<Map<String, Object>>> queryData(QueryDataDTO queryDataDTO) {
        String taskId = queryDataDTO.getTaskId();
        Boolean allDisplay = queryDataDTO.getAllDisplay();
        List<String> option = queryDataDTO.getOption();

        if (!StringUtils.hasText(taskId) || allDisplay == null) {
            return Result.fail("未选择任务");
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        List<Map<String, Object>> ans = new ArrayList<>();
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("未找到样品");
            return Result.ok(ans);
        }

        Integer digit = queryDataDTO.getDigit();
        if (digit == null || digit < 0) {
            digit = Constants.DEFAULT_DECIMAL_NUM;
            queryDataDTO.setDigit(digit);
        }

        R<TMeasureTaskAttachment> attachmentResult = tMeasureTaskAttachmentService.queryByTaskId(taskId);
        if (!attachmentResult.getSuccess()) {
            TMeasureTaskAttachment taskAttachment = new TMeasureTaskAttachment();
            taskAttachment.setTaskId(taskId);
            taskAttachment.setDataParam(JSON.toJSONString(queryDataDTO));
            tMeasureTaskAttachmentService.save(taskAttachment);
            attachmentResult = tMeasureTaskAttachmentService.queryByTaskId(taskId);
        }
        TMeasureTaskAttachment attachment = attachmentResult.getData();
        if (!StringUtils.hasText(attachment.getDataParam())) {
            attachment.setDataParam(JSON.toJSONString(queryDataDTO));
            tMeasureTaskAttachmentService.saveOrUpdate(attachment);
        }


        boolean repeat = false;
        PhotometricParameter parameter = null;
        if (StringUtils.hasText(task.getMethodKey())) {
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                parameter= JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
                if (StringUtils.hasText(parameter.getRepetitionPattern()) && parameter.getRepetitionPattern().equals("单池重复")) {
                    repeat = true;
                }
            }
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        for (TaskSpecimen specimen : taskSpecimen) {
            if (!allDisplay && specimen.getDeleteType().equals("1")) {
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

            //样品信息
            map.put(sampleId.getIdentity(), sample.getId());
            map.put(SampleTableEnum.sampleName.getIdentity(), sample.getSampleName());
            map.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber());
            map.put(SampleTableEnum.sampleType.getIdentity(), SampleTypeEnum.getName(sample.getSampleType()));
            map.put(SampleTableEnum.sampleLocation.getIdentity(), "样品池"+ sample.getSampleLocation());


            if (StringUtils.hasText(sample.getNewColData())){
                Map<String, String> object = JSONObject.parseObject(sample.getNewColData(), new TypeReference<Map<String, String>>() {});
                map.putAll(object);
            }

            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getTaskId, taskId)
                    .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                    .orderByDesc(TMeasureData::getSerialNumber)
                    .last("limit 1");
            TMeasureData data = tMeasureDataService.getOne(queryWrapper);
            if (!repeat) {
                if (data != null) {
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        List<PhotometricOriginDataInfo> wlDataList = JSON.parseArray(data.getOriginalCalculationData(), PhotometricOriginDataInfo.class);

                        for (PhotometricOriginDataInfo wlData : wlDataList) {
                            Map<String, String> originData = wlData.getData();
                            // 创建一个新的Map来存储格式化后的数据
                            Map<String, String> formattedData = new HashMap<>();
                            // 使用现有的工具类 DataCountUtil.formatValueWithBigDecimal 进行格式化
                            for (Map.Entry<String, String> entry : originData.entrySet()) {
                                String formattedValue = DataCountUtil.formatValueWithBigDecimal(digit, entry.getValue());
                                formattedData.put(entry.getKey(), formattedValue);
                            }
                            map.putAll(formattedData);
                            map.put("samplingTime", data.getSamplingTime());
                        }
                        Map<String, String> countData = getCountData(task, sample, parameter, digit, wlDataList);
                        map.putAll(countData);
                    }
                }
            } else {
                if (data != null) {
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        Map<String, Map<String, String>> childrenList = new HashMap<>();

                        List<Map<String, String>> middle = new ArrayList<>();
                        List<PhotometricOriginDataInfo> wlDataList = JSON.parseArray(data.getOriginalCalculationData(), PhotometricOriginDataInfo.class);

                        PhotometricOriginDataInfo dataInfo = reCountData(wlDataList, digit);
                        for (PhotometricOriginDataInfo wlData : wlDataList) {

                            if (wlData.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                                wlDataList.remove(wlData);
                                BeanUtils.copyProperties(dataInfo, wlData);
                                wlDataList.add(dataInfo);
                                map.putAll(wlData.getData());
                                Map<String, Map<String, String>> ch = extracted(option, wlData, sample);
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
                                Map<String, String> originData = wlData.getData();
                                // 创建一个新的Map来存储格式化后的数据
                                Map<String, String> formattedData = new HashMap<>();
                                // 使用现有的工具类 DataCountUtil.formatValueWithBigDecimal 进行格式化
                                for (Map.Entry<String, String> entry : originData.entrySet()) {
                                    String formattedValue = DataCountUtil.formatValueWithBigDecimal(digit, entry.getValue());
                                    formattedData.put(entry.getKey(), formattedValue);
                                }

                                Map<String, String> children = new HashMap<>(formattedData);
                                String[] split = wlData.getIdentity().split("-");
                                children.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber() + "-" + split[split.length - 1]);
                                children.put(SampleTableEnum.samplingTime.getIdentity(), wlData.getMeasureTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                                childrenList.putIfAbsent(wlData.getIdentity(), children);
                            }
                        }
                        map.put("samplingTime", data.getSamplingTime());

                        childrenList.forEach((key, value) -> middle.add(value));
                        map.put("children", middle);
                        Map<String, String> countData = getCountData(task, sample, parameter, digit, wlDataList);
                        map.putAll(countData);
                    }
                    map.put("remark", data.getRemark());
                }
            }
            ans.add(map);
        }

        return Result.ok(ans);
    }

    public Map<String,String> getCountData(TMeasureTask task, TMeasureSample sample, PhotometricParameter parameter, Integer digit, List<PhotometricOriginDataInfo> wlDataList) {

        Map<String, String> map = new HashMap<>();

        R<List<PhotometricCountDataInfo>> formula = formula(wlDataList, sample, task, parameter, digit);
        if (formula.getSuccess()) {
            List<PhotometricCountDataInfo> formulaData = formula.getData();
            for (PhotometricCountDataInfo count : formulaData) {
                String s = DataCountUtil.formatValueWithBigDecimal(digit, count.getCountValue());
                map.put(count.getResultIdentity() + "-" + count.getFormulaText(), s);
            }
        }


        LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, task.getId())
                .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(TMeasureAnalysisData::getAnalysisName).last("limit 1");

        TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (one != null) {
            List<PhotometricCountDataInfo> countList = JSON.parseArray(one.getAnalysisResult(), PhotometricCountDataInfo.class);

            if (map.isEmpty()) {
                for (PhotometricCountDataInfo count : countList) {

                    String s = DataCountUtil.formatValueWithBigDecimal(digit, count.getCountValue());
                    map.put(count.getResultIdentity() + "-" + count.getFormulaText(), s);

                }
            }

            if (StringUtils.hasText(one.getAnalysisParam())) {
                Map<String, String> jsonObject = JSON.parseObject(one.getAnalysisParam(), new TypeReference<Map<String, String>>(){});
                map.putAll(jsonObject);
            }
        }
        return map;
    }

    private PhotometricOriginDataInfo reCountData(List<PhotometricOriginDataInfo> wlDataList, Integer digit) {

        Map<String, List<Double>> data = new HashMap<>();
        Map<String, RepeatMeasureData> otherData = new HashMap<>();
        Map<String, String> ans = new HashMap<>();
        PhotometricOriginDataInfo result = new PhotometricOriginDataInfo();

        for (PhotometricOriginDataInfo wlData : wlDataList) {
            if (wlData.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                BeanUtils.copyProperties(wlData, result);
                continue;
            }

            wlData.getData().forEach((key, value) -> {
                data.computeIfAbsent(key, k -> new ArrayList<>()).add(Double.parseDouble(value));
            });
        }

        for (Map.Entry<String, List<Double>> entry : data.entrySet()) {
            List<Double> values = entry.getValue();
            double mean = DataCountUtil.calculateMean(values, digit);
            Double sd = DataCountUtil.calculateSD(values, digit);
            Double rsd = DataCountUtil.calculateRSD(values, digit);

            RepeatMeasureData repeatMeasureData = new RepeatMeasureData();
            repeatMeasureData.setSd(sd == null ? "未检出" :String.valueOf(sd));
            repeatMeasureData.setRsd(rsd == null ? "未检出" :String.valueOf(rsd));
            otherData.put(entry.getKey(), repeatMeasureData);
            ans.put(entry.getKey(), String.valueOf(mean));
        }
        result.setData(ans);
        result.setOtherData(otherData);
        return result;
    }

    private Map<String, Map<String, String>> extracted(List<String> option, PhotometricOriginDataInfo wlData,  TMeasureSample sample) {

        Map<String, String> originData = wlData.getData();
        Map<String, Map<String, String>> ans = new HashMap<>();
        String mapName = wlData.getWavelength() + "-" + wlData.getMode();

        RepeatMeasureData absData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode());
        RepeatMeasureData tPercentData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode());


        if (wlData.getMode().equals(WaveLengthEnum.abs.getCode()) || wlData.getMode().equals(WaveLengthEnum.tPercent.getCode())) {

            if (!option.isEmpty()) {
                for (String s : option) {
                    Map<String, String> children = new HashMap<>();
                    if (DataIdentityEnums.avg.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.avg.getName());

                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), originData.get(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode()));
                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), originData.get(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode()));
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.avg.getIdentity(), children);
                    }
                    if (DataIdentityEnums.sd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.sd.getName());

                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), absData.getSd());
                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tPercentData.getSd());
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.sd.getIdentity(), children);
                    }
                    if (DataIdentityEnums.rsd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getName());
                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), absData.getRsd());
                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tPercentData.getRsd());
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.rsd.getIdentity(), children);
                    }
                }
            }
        } else {
            if (!option.isEmpty()) {
                for (String s : option) {
                    Map<String, String> children = new HashMap<>();
                    if (DataIdentityEnums.avg.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.avg.getName());
                        children.put(mapName, originData.get(mapName));
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.avg.getIdentity(), children);
                    }
                    if (DataIdentityEnums.sd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.sd.getName());
                        RepeatMeasureData measureData = wlData.getOtherData().get(mapName);
                        children.put(mapName, measureData.getSd());
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.sd.getIdentity(), children);
                    }
                    if (DataIdentityEnums.rsd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getName());
                        RepeatMeasureData measureData = wlData.getOtherData().get(mapName);
                        children.put(mapName, measureData.getRsd());
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.rsd.getIdentity(), children);
                    }
                }
            }
        }

        return ans;
    }

    @Override
    public R<String> savePhotometricMeasureData(PhotometricSampleAndDataVO sampleAndDataVO) {

        String taskId = sampleAndDataVO.getTaskId();
        String sampleId = sampleAndDataVO.getSampleId();
        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getSampleKey, sampleId)
                .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
        TMeasureData tMeasureData = tMeasureDataService.getOne(queryWrapper);

        if (tMeasureData == null) {
            tMeasureData = new TMeasureData();
            tMeasureData.setSerialNumber(0);
        }

        if (StringUtils.hasText(tMeasureData.getOriginalCalculationData()) || StringUtils.hasText(tMeasureData.getInstrumentData())) {
            tMeasureData = new TMeasureData();
            tMeasureData.setSerialNumber(tMeasureData.getSerialNumber() + 1);
        }
        tMeasureData.setTaskId(taskId);
        tMeasureData.setSampleKey(sampleId);
        tMeasureData.setInstrumentData(JSON.toJSONString(sampleAndDataVO.getMachineData()));
        tMeasureData.setOriginalCalculationData(JSON.toJSONString(sampleAndDataVO.getWavelengthData()));
        tMeasureData.setMeasurementStartTime(sampleAndDataVO.getStartTime());
        tMeasureData.setMeasurementEndTime(sampleAndDataVO.getEndTime());
        tMeasureData.setSamplingTime(sampleAndDataVO.getSampleTime());

        tMeasureData.setIsDelete(createType);
        tMeasureDataService.saveOrUpdate(tMeasureData);

        return R.ok(tMeasureData.getId());
    }

    @Override
    public R<Boolean> saveCountData(PhotometricSampleAndDataVO countData, String dataId) {

        if (countData.getCountData() == null || countData.getCountData().isEmpty()) {
            return R.fail("没有要保存的数据");
        }
        String taskId = countData.getTaskId();
        String sampleId = countData.getSampleId();

        LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(TMeasureAnalysisData::getDataVersion).last("limit 1");
        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (analysisData != null) {
            if (StringUtils.hasText(analysisData.getMeasureDataId())) {
                analysisData.setId(null);
                analysisData.setDataVersion(analysisData.getDataVersion() + 1);
            }
            analysisData.setMeasureDataId(dataId);

            analysisData.setAnalysisResult(JSON.toJSONString(countData.getCountData()));
        } else {
            analysisData = new TMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setAnalysisResult(JSON.toJSONString(countData.getCountData()));
            analysisData.setDataVersion(0);
            analysisData.setMeasureDataId(dataId);
        }
        analysisData.setIsDelete(createType);
        tMeasureAnalysisDataService.saveOrUpdate(analysisData);
        return R.ok(true);

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
        if (list != null && !list.isEmpty()) {
            data = list.get(0);
            data.setRemark(vo.getRemark());
        } else {
            data.setTaskId(taskId);
            data.setSampleKey(sampleId);
            data.setSerialNumber(1);
            data.setRemark(vo.getRemark());
        }
        tMeasureDataService.saveOrUpdate(data);

        return Result.ok(true);
    }

    /**
     * 零点校零
     * @param deviceSerialNumber
     * @param methodId
     * @return
     */
    @Override
    public Result zero(String deviceSerialNumber, String methodId, String taskId,String updateBy) {

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
        PhotometricParameter photometricParameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
        if (photometricParameter.getWaveLength() == null || photometricParameter.getWaveLength().isEmpty()) {
            return Result.fail("缺少波长");
        }
        List<String> waveLength = photometricParameter.getWaveLength();
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
    public R<File> downloadWord(String taskId, Boolean allDisplay) {
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

        List<HeadVO> vos = tableHead(taskId, methodId, allDisplay).getData();
        heads.addAll(vos);
        heads.add(new HeadVO("备注", "remark"));
        heads.add(new HeadVO("采样时间", "samplingTime"));

        QueryDataDTO queryDataDTO = new QueryDataDTO();
        queryDataDTO.setOption(new ArrayList<>());
        queryDataDTO.setTaskId(taskId);
        queryDataDTO.setAllDisplay(allDisplay);

        List<Map<String, Object>> data = queryData(queryDataDTO).getData();


        XWPFDocument doc = new XWPFDocument();
        CTDocument1 document = doc.getDocument();
        CTBody body = document.getBody();
        if (!body.isSetSectPr()) {
            body.addNewSectPr();
        }
        CTSectPr section = body.getSectPr();
        if(!section.isSetPgSz()) {
            section.addNewPgSz();
        }
        CTPageSz pageSize = section.getPgSz();
        if (vos.size() > 5) {
            //必须要设置下面两个参数，否则整个的代码是无效的
            pageSize.setW(BigInteger.valueOf(15840));
            pageSize.setH(BigInteger.valueOf(12240));
        }

        XWPFParagraph paragraph = doc.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText("任务：" + task.getTaskName() + "\r" );
        run.setText("方法：" + method.getMethodName() + "\r");
        run.setText("测试数据：");
        XWPFTable table = doc.createTable();

        table.setWidth("100%");
        int i = 0;
        for (Map<String, Object> map : data) {
            XWPFTableRow headerRow = table.getRow(0);
            if(headerRow != null) {
                XWPFTableCell cell = headerRow.getCell(0);
                if(cell != null) {
                    cell.setText("序号");
                }

                XWPFTableRow row = table.createRow();
                int m = 1;
                for (HeadVO head : heads) {

                    String cellData = String.valueOf(map.get(head.getIdentity()));

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

    @Override
    public R<List<InputParam>> queryInputParam(String methodId) {
        if (!StringUtils.hasText(methodId)) {
            return R.fail("未选择方法");
        }
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return R.fail("方法不存在");
        }
        PhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
        List<InputParam> inputParams = parameter.getSampleInputParams();

        return R.ok(inputParams);
    }

    @Override
    public R<List<InputParamMapTableHead>> queryInputParamMapTableHead(String taskId) {

        if (!StringUtils.hasText(taskId)) {
            return R.fail("未选择任务");
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return R.fail("任务不存在");
        }
        List<InputParamMapTableHead> ans = new ArrayList<>();
        if (task.getTableInfo() != null) {
            TaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
            ans = snapshot.getMapRelation();
        }
        return R.ok(ans);

    }

    @Override
    public R<Boolean> saveInputParamMapTableHead(TaskHeadSnapshot taskHeadSnapshot) {

        if (!StringUtils.hasText(taskHeadSnapshot.getTaskId())) {
            return R.fail("未选择任务");
        }

        TMeasureTask task = tMeasureTaskService.getById(taskHeadSnapshot.getTaskId());
        if (task == null) {
            return R.fail("任务不存在");
        }
        LinkedHashSet<HeadVO> ans = new LinkedHashSet<>();

        if (StringUtils.hasText(task.getSampleKey())) {
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen specimen : taskSpecimen) {
                TMeasureSample measureSample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                List<HeadVO> heads = JSON.parseArray(measureSample.getHeads(), HeadVO.class);

                if (heads != null && !heads.isEmpty()) {
                    ans.addAll(heads);
                }
            }
        }
        taskHeadSnapshot.setSampleHeads(new ArrayList<>(ans));
        task.setTableInfo(JSON.toJSONString(taskHeadSnapshot));

        return tMeasureTaskService.updateById(task) ? R.ok(true) : R.fail("保存失败");
    }


}
