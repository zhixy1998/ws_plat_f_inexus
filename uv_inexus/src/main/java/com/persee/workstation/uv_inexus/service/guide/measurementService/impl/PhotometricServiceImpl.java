package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;

import static com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTableEnum.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocument1;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.AddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricMethodParameterDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricParameter;
import com.persee.workstation.uv_inexus.dto.measurementDto.QueryDataDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.TaskHeadSnapshotDTO;
import com.persee.workstation.uv_inexus.enums.measurementEnums.DataIdentityEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.RepeatConfigEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTableEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.TableHeadILocateEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureAnalysisData;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureData;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureProject;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureSample;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTaskAttachment;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskHeadSnapshot;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.PhotometricService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureAnalysisDataService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureDataService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureMethodService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureProjectService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureSampleService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureTaskAttachmentService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureTaskService;
import com.persee.workstation.uv_inexus.service.guide.standardService.TReportStandardDetectionService;
import com.persee.workstation.uv_inexus.service.guide.standardService.TReportStandardPretreatmentService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.util.pythonUtil.RestUtil;
import com.persee.workstation.uv_inexus.vo.measurementVo.InputParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricCountDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricOriginDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricSampleAndDataVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.PreCountResultParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MethodParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.PhotometricReportDataVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RepeatMeasureData;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.SampleVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.SaveDataVO;

import lombok.extern.slf4j.Slf4j;

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
    TMeasureTaskMapper tMeasureTaskMapper;
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
    public Result<String> updateMethodParameter(PhotometricMethodParameterDto methodDto) {
        // 根据方法id查询当前方法
        if (!StringUtils.hasText(methodDto.getMethodId())) {
            return Result.fail("缺少方法");
        }
        if(methodDto.getMethodName().length() >= 256){
            return Result.fail("方法名格式错误");
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
        if (dto.getHeads() == null || dto.getHeads().isEmpty()) {
            return Result.fail("至少需要一个表头");
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

        TaskHeadSnapshot taskHeadSnapshot = new TaskHeadSnapshot();

        if (StringUtils.hasText(task.getTableInfo())) {
            taskHeadSnapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
        }

        if (dto.getHeads() != null && !dto.getHeads().isEmpty()) {
            taskHeadSnapshot.setSampleHeads(dto.getHeads());
        }
        taskHeadSnapshot.setTaskId(task.getId());
        task.setTableInfo(JSON.toJSONString(taskHeadSnapshot));


        for (Map<String, String> map : dto.getNewSamples()) {
            String jsonString = JSON.toJSONString(map);
            SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);
            TMeasureSample newSample = new TMeasureSample();
            BeanUtils.copyProperties(sample, newSample);
            newSample.setId(sample.getSampleId());

            if (dto.getHeads() != null) {
                JSONObject newColData = new JSONObject();

                for (HeadVO head : dto.getHeads()) {
                    if (head.getLocate().equals(TableHeadILocateEnums.system.getIdentity())) {
                        continue;
                    }
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
                specimen.setIsTested(noTested);
                specimen.setSampleType(newSample.getSampleType());
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

        if (!StringUtils.hasText(taskId)) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
            return Result.ok(preHead);
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        if (!StringUtils.hasText(task.getTableInfo())) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
            return Result.ok(preHead);
        } else {
            TaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
            if (snapshot.getSampleHeads() != null && !snapshot.getSampleHeads().isEmpty()) {
                return Result.ok(snapshot.getSampleHeads());
            } else {
                List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
                return Result.ok(preHead);
            }
        }
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
        if(methodId.length() != 32){
            return  Result.fail("方法参数格式错误");
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
        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(methodId)) {
            return Result.fail("请选择方法或任务");
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }
        PhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
        LinkedHashSet<HeadVO> ans = new LinkedHashSet<>();
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_stand.getIdentity());
            ans.addAll(preHead);
            return Result.ok(new ArrayList<>(ans));
        }

        if (StringUtils.hasText(task.getTableInfo())) {
            TaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
            if (snapshot != null) {
                if (snapshot.getTaskHeads()!= null && !snapshot.getTaskHeads().isEmpty()) {
                    ans.addAll(snapshot.getTaskHeads());
                }
                if (snapshot.getSampleHeads() != null && !snapshot.getSampleHeads().isEmpty()) {
                    ans.addAll(snapshot.getSampleHeads());
                }
            }
        } else {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(method.getMeasurementType(), null);
            ans.addAll(preHead);
        }

        if (StringUtils.hasText(task.getSampleKey())) {

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

        List<HeadVO> vos = dealHeads(new ArrayList<>(ans), parameter, allDisplay);

        return Result.ok(vos);
    }

    private List<HeadVO> dealHeads(List<HeadVO> heads, PhotometricParameter parameter, Boolean allDisplay) {
        List<String> waveLength = parameter.getWaveLength();
        Map<String, HeadVO> map = heads.stream().collect(Collectors.toMap(HeadVO::getIdentity, head -> head));

        Map<String, List<HeadVO>> headsMap = new HashMap<>();

        Map<String, HeadVO> analyzeMap = new HashMap<>();

        for (HeadVO head : heads) {
            headsMap.computeIfAbsent(head.getLocate(), k -> new ArrayList<>()).add(head);
            if (head.getLocate().equals(TableHeadILocateEnums.measure.getIdentity())) {
                map.put(head.getIdentity(), head);
            }
            if (head.getLocate().equals(TableHeadILocateEnums.analyze.getIdentity())) {
                analyzeMap.put(head.getIdentity(), head);
            }
        }

        List<HeadVO> allMeasureHeads = new ArrayList<>();

        String mode = parameter.getLuminosity();
        if (mode.equals(WaveLengthEnum.es.getCode()) || mode.equals(WaveLengthEnum.er.getCode())) {
            for (String s : waveLength) {
                String identity = s + "-"+ mode;
                String name = s + WaveLengthEnum.getName(mode);
                HeadVO newHead = new HeadVO(name, identity, TableHeadILocateEnums.measure.getIdentity(), name);
                if (map.containsKey(identity)) {
                    newHead = map.get(identity);
                }
                allMeasureHeads.add(newHead);
            }
        } else if (mode.equals(WaveLengthEnum.abs.getCode()) || mode.equals(WaveLengthEnum.tPercent.getCode())) {
            for (String s : waveLength) {
                String absIdentity = s + "-"+ WaveLengthEnum.abs.getCode();
                String tPercentIdentity = s + "-"+ WaveLengthEnum.tPercent.getCode();
                String absName = s + WaveLengthEnum.abs.getName();
                String tPercentName = s + WaveLengthEnum.tPercent.getName();

                HeadVO absHead = new HeadVO(absName, absIdentity, TableHeadILocateEnums.measure.getIdentity(), absName);
                HeadVO tPercentHead = new HeadVO(tPercentName, tPercentIdentity, TableHeadILocateEnums.measure.getIdentity(), tPercentName);

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
        }
        List<HeadVO> ans = new ArrayList<>();
        List<HeadVO> systemHeads = headsMap.get(TableHeadILocateEnums.system.getIdentity());
        if (systemHeads != null) ans.addAll(systemHeads);

        List<HeadVO> sampleHeads = headsMap.get(TableHeadILocateEnums.sample.getIdentity());
        if (sampleHeads != null) ans.addAll(sampleHeads);
        ans.addAll(allMeasureHeads);
        List<HeadVO> taskHeads = headsMap.get(TableHeadILocateEnums.data.getIdentity());
        if (taskHeads != null)ans.addAll(taskHeads);

        List<PreCountResultParam> resultParams = parameter.getResultParams();
        List<HeadVO> headAnalyze = new ArrayList<>();
        for (PreCountResultParam results : resultParams) {
            //TODO 方法参数标识待定，后续再改动
            if (!StringUtils.hasText(results.getParam())) {
                results.setParam(results.getResultIdentity() + "-" + results.getFormulaText());
            }
            String name = results.getResultName() + "(" + results.getUnit() + ")";
            HeadVO analyzeHead = new HeadVO(name, results.getParam(), TableHeadILocateEnums.analyze.getIdentity(), name);

            if (analyzeMap.containsKey(results.getParam())) {
                analyzeHead = analyzeMap.get(results.getParam());
            }
            headAnalyze.add(analyzeHead);
        }
        ans.addAll(headAnalyze);

        List<HeadVO> other = headsMap.get(TableHeadILocateEnums.other.getIdentity());
        if (other == null || other.isEmpty()) {
            other = new ArrayList<>();
            HeadVO samplingTimeHead = new HeadVO(SampleTableEnum.samplingTime.getName(), SampleTableEnum.samplingTime.getIdentity(), TableHeadILocateEnums.other.getIdentity(), SampleTableEnum.samplingTime.getName());
            HeadVO remark = new HeadVO(SampleTableEnum.remark.getName(), SampleTableEnum.remark.getIdentity(), TableHeadILocateEnums.other.getIdentity(), SampleTableEnum.remark.getName());
            other.add(samplingTimeHead);
            other.add(remark);
        }

        ans.addAll(other);

        return ans;
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
    public Result saveDataInfo(SaveDataVO vo) {
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

    /**
     * 多格式文件导出（支持word/txt/xlsx）
     * @param taskId 任务ID
     * @param allDisplay 是否显示全部数据
     * @param fileType 导出格式（word/txt/xlsx）
     * @return 生成的文件对象
     */
    public R<File> downloadFile(String taskId, Boolean allDisplay, String fileType) {
        log.info("开始导出，taskId: {}, fileType: {}", taskId, fileType);
        // 1. 基础参数校验（复用原逻辑）
        if (!StringUtils.hasText(taskId)) {
            return R.fail("缺少任务id");
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            log.error("导出失败：任务不存在，taskId: {}", taskId);
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
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return R.fail("测量方法不存在");
        }

        // 构建表头（与原逻辑一致）
        List<HeadVO> heads = new ArrayList<>();
        // heads.add(new HeadVO("样品编号", "sampleNumber"));
        // heads.add(new HeadVO("样品名称", "sampleName"));
        // heads.add(new HeadVO("样品类型", "sampleType"));
        // heads.add(new HeadVO("稀释比", "dilutionRatio"));
        // heads.add(new HeadVO("样品体积", "sampleVolume"));
        // heads.add(new HeadVO("样品位置", "sampleLocation"));
        // 添加动态表头
        List<HeadVO> vos = tableHead(taskId, methodId, allDisplay).getData();
        heads.addAll(vos);
        // 添加固定补充表头
        // heads.add(new HeadVO("备注", "remark"));
        // heads.add(new HeadVO("采样时间", "samplingTime"));
        // // 最前面加序号列（所有格式统一需要）
        heads.add(0, new HeadVO("序号", "serialNumber"));

        // 查询数据（复用原逻辑）
        QueryDataDTO queryDataDTO = new QueryDataDTO();
        queryDataDTO.setOption(new ArrayList<>());
        queryDataDTO.setTaskId(taskId);
        queryDataDTO.setAllDisplay(allDisplay);
        List<Map<String, Object>> data = queryData(queryDataDTO).getData();
        if (data == null || data.isEmpty()) {
            return R.fail("没有可导出的数据");
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
            log.error("文件生成失败", e);
            // 异常时删除无效文件
            if (file.exists()) {
                file.delete();
            }
            return R.fail("文件生成失败：" + e.getMessage());
        }
    }

    // ---------------------- 原有downloadWord方法（可保留或删除，建议用上面的downloadFile替代） ----------------------
    public R<File> downloadWord(String taskId, Boolean allDisplay) {
        // 直接调用多格式方法，指定格式为word
        return downloadFile(taskId, allDisplay, "word");
    }

    // ---------------------- 新增：三格式文件生成具体实现 ----------------------
    /**
     * 生成Word文件（修复原逻辑的表格列错位问题）
     */
    private void generateWordFile(File file, TMeasureTask task, TMeasureMethod method,
                                 List<HeadVO> heads, List<Map<String, Object>> data, int vosSize) throws Exception {
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

        // 填充表头（修复原逻辑的表头赋值问题）
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < columnCount; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            cell.setText(heads.get(i).getName());
        }

        // 填充数据行（修复原逻辑的索引越界问题）
        int rowIndex = 0;
        for (Map<String, Object> map : data) {
            XWPFTableRow row = table.createRow();
            rowIndex++;
            // 第0列：序号
            row.getCell(0).setText(String.valueOf(rowIndex));

            // 填充其他列（与表头顺序对应）
            for (int i = 1; i < columnCount; i++) {
                HeadVO head = heads.get(i);
                String cellData = String.valueOf(map.getOrDefault(head.getIdentity(), ""));

                // 特殊字段格式化（复用原逻辑）
                if ("sampleType".equals(head.getIdentity())) {
                    cellData = SampleTypeEnum.getName(cellData);
                }
                if ("sampleLocation".equals(head.getIdentity())) {
                    cellData = "样品" + cellData;
                }
                // 空值处理
                if ("null".equals(cellData) || cellData.isEmpty()) {
                    cellData = "-";
                }

                row.getCell(i).setText(cellData);
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
    private void generateTxtFile(File file, List<HeadVO> heads, List<Map<String, Object>> data) throws Exception {
        // 使用BufferedWriter保证效率，UTF-8避免中文乱码
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            // 1. 写入表头（制表符分隔）
            String headerLine = heads.stream()
                    .map(HeadVO::getName)
                    .collect(Collectors.joining("\t"));
            writer.write(headerLine);
            writer.newLine(); // 换行

            // 2. 写入数据（与表头顺序一致）
            for (Map<String, Object> map : data) {
                List<String> rowData = new ArrayList<>();
                for (HeadVO head : heads) {
                    String cellData = String.valueOf(map.getOrDefault(head.getIdentity(), ""));

                    // 特殊字段格式化（与Word保持一致）
                    if ("sampleType".equals(head.getIdentity())) {
                        cellData = SampleTypeEnum.getName(cellData);
                    }
                    if ("sampleLocation".equals(head.getIdentity())) {
                        cellData = "样品" + cellData;
                    }
                    if ("null".equals(cellData) || cellData.isEmpty()) {
                        cellData = "-";
                    }

                    rowData.add(cellData);
                }
                // 制表符分隔列，写入一行
                writer.write(String.join("\t", rowData));
                writer.newLine();
            }
        }
    }

    /**
     * 简化版：生成 XLSX 文件（仅导出数据，无复杂样式）
     */
    private void generateExcelFile(File file, List<HeadVO> heads, List<Map<String, Object>> data) throws Exception {
        // 1. 转换表头（处理空表头）
        List<List<String>> excelHeads = new ArrayList<>();
        for (HeadVO head : heads) {
            List<String> singleHead = new ArrayList<>();
            singleHead.add(StringUtils.hasText(head.getName()) ? head.getName() : "未命名列");
            excelHeads.add(singleHead);
        }

        // 2. 转换数据（处理空值和特殊字符）
        List<List<Object>> excelData = new ArrayList<>();
        for (Map<String, Object> map : data) {
            List<Object> rowData = new ArrayList<>();
            for (HeadVO head : heads) {
                Object value = map.get(head.getIdentity());
                String cellData = "-"; // 默认空值显示

                if (value != null) {
                    cellData = String.valueOf(value);
                    // 特殊字段格式化
                    if ("sampleType".equals(head.getIdentity())) {
                        cellData = SampleTypeEnum.getName(cellData);
                    }
                    if ("sampleLocation".equals(head.getIdentity())) {
                        cellData = "样品" + cellData;
                    }
                    // 清除特殊字符（避免Excel解析异常）
                    cellData = cellData.replace("\r", "").replace("\n", "").replace("\t", " ");
                    // 空字符串处理
                    if (cellData.isEmpty() || "null".equalsIgnoreCase(cellData)) {
                        cellData = "-";
                    }
                }

                rowData.add(cellData);
            }
            excelData.add(rowData);
        }

        // 3. 写入Excel（使用OutputStream确保流正确关闭）
        try (OutputStream out = new FileOutputStream(file)) {
            EasyExcel.write(out)
                    .head(excelHeads)
                    .sheet("测量数据")
                    .doWrite(excelData);
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
    public R<Boolean> saveInputParamMapTableHead(TaskHeadSnapshotDTO snapshotDTO) {

        if (!StringUtils.hasText(snapshotDTO.getTaskId())) {
            return R.fail("未选择任务");
        }

        TMeasureTask task = tMeasureTaskService.getById(snapshotDTO.getTaskId());
        if (task == null) {
            return R.fail("任务不存在");
        }

        TaskHeadSnapshot taskHeadSnapshot = new TaskHeadSnapshot();
        List<HeadVO> taskHeads = new ArrayList<>();
        List<HeadVO> sampleHeads = new ArrayList<>();
        for (HeadVO head : snapshotDTO.getTaskHeads()) {
            if (head.getLocate().equals(TableHeadILocateEnums.sample.getIdentity()) || head.getLocate().equals(TableHeadILocateEnums.system.getIdentity())) {
                sampleHeads.add(head);
            } else {
                taskHeads.add(head);
            }
        }
        BeanUtils.copyProperties(snapshotDTO, taskHeadSnapshot);
        taskHeadSnapshot.setTaskHeads(taskHeads);
        taskHeadSnapshot.setSampleHeads(sampleHeads);

        task.setTableInfo(JSON.toJSONString(taskHeadSnapshot));

        return tMeasureTaskService.updateById(task) ? R.ok(true) : R.fail("保存失败");
    }

    @Override
    public Result<InputParam> insertMethodParameter(InputParam methodParameter) {
        if(methodParameter.getParamName().length() >= 256){
            return Result.fail("名称格式错误");
        }
        String methodMark = methodParameter.getResultIdentity();
        String regex = "^[A-Z]\\d{1,99}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(methodMark);
        boolean res = matcher.matches();
        if (!res) {
            return Result.fail();
        }
        return Result.ok(methodParameter);
    }


}
