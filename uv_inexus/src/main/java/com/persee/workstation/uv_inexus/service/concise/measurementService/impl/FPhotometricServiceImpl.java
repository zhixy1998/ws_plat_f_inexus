package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.concise.common.FAddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.FMethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.dto.concise.photometricDto.NewPhotometricMethodParamCheckDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FTaskHeadSnapshotDTO;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.*;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.util.pythonUtil.RestUtil;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricCountDataInfo;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricOriginDataInfo;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricReportDataVO;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricSampleAndDataVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.*;
import com.persee.workstation.uv_inexus.vo.concise.ration.FRepeatMeasureData;
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
@Service("fPhotometricService")
public class FPhotometricServiceImpl implements PhotometricConciseService {
    @Autowired
    FMeasureProjectService fMeasureProjectService;
    @Autowired
    FMeasureTaskService fMeasureTaskService;
    @Autowired
    FMeasureSampleService fMeasureSampleService;
    @Autowired
    SysUserMapper userManageDAO;
    @Autowired
    FMeasureTaskMapper fMeasureTaskMapper;
    @Autowired
    FMeasureMethodService fMeasureMethodService;
    @Autowired
    FMeasureDataService fMeasureDataService;

    @Autowired
    FMeasureAnalysisDataService fMeasureAnalysisDataService;

    @Autowired
    FMeasureTaskAttachmentService fMeasureTaskAttachmentService;

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

    @Value("11")
    String defaultProjectId;


    @Override
    public Result createMethodPort(FMethodParamRequestDto methodDto, String updateBy, FPhotometricParameter photometricParameter) {

        if (photometricParameter.getWaveLength() == null || photometricParameter.getWaveLength().isEmpty()) {
            return Result.fail("请填写波长值");
        }

        // 创建测量方法表
        FMeasureMethod fMeasureMethod = new FMeasureMethod();
        fMeasureMethod.setProjectItemKey(methodDto.getProjectId());
        fMeasureMethod.setMethodName(methodDto.getMethodName());
        fMeasureMethod.setMeasurementType(methodDto.getMethodType());
        fMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(photometricParameter));
        fMeasureMethod.setMethodCreator(updateBy);
        fMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        boolean save = fMeasureMethodService.save(fMeasureMethod);
        if (save) {
            return Result.ok("创建成功");
        } else {
            return Result.fail("创建失败");
        }
    }

    @Override
    public Result<FMeasurementCurrentInfoVO> newDraft(String methodId, String updateBy) {

        SysUserPO sysUserPO = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
        String userId = sysUserPO.getId();

        FMeasureTask task = new FMeasureTask();
        task.setTaskName("新建光度测量草稿" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        task.setIsDelete(createType);
        task.setMeasureType(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription());
        task.setMethodKey(methodId);
        task.setUserId(userId);
        task.setProjectItemKey(defaultProjectId);
        task.setCreateTime(LocalDateTime.now());

        boolean save = fMeasureTaskService.save(task);
        FMeasurementCurrentInfoVO vo = new FMeasurementCurrentInfoVO();
        vo.setMethodId(task.getMethodKey());

//        vo.setProjectId(task.getProjectItemKey());
        if (save) {
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getTaskName());
            return Result.ok(vo);
        } else {
            return Result.fail("新建失败");
        }
    }

    @Override
    public Result<FMethodChangeCheckVO> checkMethodParam(NewPhotometricMethodParamCheckDTO checkDTO) {

        if (checkDTO.getNewMethodParam() == null) {
            return Result.fail("没有可应用的方法参数");
        }

        FMethodChangeCheckVO vo = new FMethodChangeCheckVO();
        vo.setCurrentMethodId(checkDTO.getCurrentMethodId());
        vo.setParamChange(false);
        vo.setClearDataRange(ChangeMethodTypeEnum.noChange.getIdentity());
        vo.setMessage(ChangeMethodTypeEnum.noChange.getMessage());

        if (!StringUtils.hasText(checkDTO.getCurrentMethodId())) {
            log.info("生成新方法");
            vo.setParamChange(true);
            vo.setMessage(ChangeMethodTypeEnum.newMethod.getMessage());
            vo.setClearDataRange(ChangeMethodTypeEnum.newMethod.getIdentity());
            return Result.ok(vo);
        }

        FMeasureMethod method = fMeasureMethodService.getById(checkDTO.getCurrentMethodId());
        FPhotometricParameter oldParam = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);

        FPhotometricParameter methodParam = checkDTO.getNewMethodParam();

        if (oldParam.equals(methodParam)) {
            vo.setParamChange(false);
            vo.setClearDataRange(ChangeMethodTypeEnum.noChange.getIdentity());
            vo.setMessage(ChangeMethodTypeEnum.noChange.getMessage());
            return Result.ok(vo);
        }
        //检查测量数据
        boolean b = checkMeasureParam(oldParam, methodParam);
        if (!b) {
            log.info("测量参数有改变，要清理所有数据");
            vo.setClearDataRange(ChangeMethodTypeEnum.measure.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(ChangeMethodTypeEnum.measure.getMessage());
            return Result.ok(vo);
        }

        //检查分析参数
        boolean analyze = checkAnalyzeParam(oldParam, methodParam);
        if (!analyze) {
            log.info("分析参数有改变，要清理分析数据");
            vo.setClearDataRange(ChangeMethodTypeEnum.analyze.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(ChangeMethodTypeEnum.analyze.getMessage());
        }
        return Result.ok(vo);
    }

    //检查参数是否改变，true--未改变，false--改变了
    private boolean checkMeasureParam(FPhotometricParameter oldParam, FPhotometricParameter newParam) {

        List<String> oldWave = oldParam.getWaveLength();
        List<String> newWave = newParam.getWaveLength();
        if (!Objects.equals(oldWave, newWave)) {
            return false;
        }

        return oldParam.getLuminosity().equals(newParam.getLuminosity()) &&
                oldParam.getSpectralBandwidth().equals(newParam.getSpectralBandwidth()) &&
                oldParam.getResponseTime().equals(newParam.getResponseTime()) &&
                oldParam.getChangeLampWaveLength().equals(newParam.getChangeLampWaveLength()) &&
                oldParam.getEnergyLamp().equals(newParam.getEnergyLamp()) &&
                oldParam.getGain().equals(newParam.getGain())  &&
                oldParam.getRepetitionPattern().equals(newParam.getRepetitionPattern()) &&
                oldParam.getRepetitionTime().equals(newParam.getRepetitionTime()) &&
                oldParam.getTimeInterval().equals(newParam.getTimeInterval());
    }

    //检查参数是否改变，true--未改变，false--改变了
    private boolean checkAnalyzeParam(FPhotometricParameter oldParam, FPhotometricParameter newParam) {
        List<FPreCountResultParam> oldResult = oldParam.getResultParams();
        List<FPreCountResultParam> newResult = newParam.getResultParams();

        if (oldResult == null ) {
            oldResult = new ArrayList<>();
        }
        if (newResult == null ) {
            newResult = new ArrayList<>();
        }

        if (!oldResult.equals(newResult)) {
            if (newResult.size() != oldResult.size()) {
                return false;
            }

            for (int i = 0; i < oldResult.size(); i++) {
                FPreCountResultParam oldResultParam = oldResult.get(i);
                FPreCountResultParam newResultParam = newResult.get(i);
                if (oldResultParam.equals(newResultParam)) {
                    continue;
                }
                if (!oldResultParam.getFormulaText().equals(newResultParam.getFormulaText())) {
                    return false;
                }
            }
        }
        return true;
    }



    @Override
    public Result<FMeasurementCurrentInfoVO> applyMethod(NewPhotometricMethodParamCheckDTO methodDTO) {

        if (methodDTO.getNewMethodParam() == null && !StringUtils.hasText(methodDTO.getNewMethodId())) {
            return Result.fail("没有可应用的方法或参数");
        }
        if (!StringUtils.hasText(methodDTO.getMeasureType())) {
            methodDTO.setMeasureType(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription());
        }
        String methodId = methodDTO.getNewMethodId();


        FMeasurementCurrentInfoVO ans = new FMeasurementCurrentInfoVO();

        SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, methodDTO.getUpdateBy()));
        String userId = po.getId();
        ans.setMethodName(methodDTO.getMethodName());

        if (!StringUtils.hasText(methodId)) {
            FMeasureMethod method = new FMeasureMethod();
            method.setMeasurementParamInfo(JSON.toJSONString(methodDTO.getNewMethodParam()));
            method.setMethodName(methodDTO.getMethodName() + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            method.setMeasurementType(methodDTO.getMeasureType());
            method.setProjectItemKey(defaultProjectId);
            method.setMethodCreator(methodDTO.getUpdateBy());
            method.setMethodCreateTime(LocalDateTime.now());
            method.setMethodUseTime(LocalDateTime.now());
            boolean save = fMeasureMethodService.save(method);

            if (!save) {
                return Result.fail("应用失败");
            }
            methodId = method.getId();
            ans.setMethodName(method.getMethodName());
        }
        ans.setMethodId(methodId);


        FMeasureTask task = new FMeasureTask();
        if (!StringUtils.hasText(methodDTO.getCurrentTaskId())) {
            //任务不存在，创建任务和方法
            task.setTaskName("草稿数据" + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            task.setMethodKey(methodId);
            task.setCreateTime(LocalDateTime.now());
            task.setUserId(userId);
            task.setProjectItemKey(defaultProjectId);
            task.setIsDelete(createType);
            task.setMeasureType(methodDTO.getMeasureType());
            fMeasureTaskService.save(task);

        } else {
            task = fMeasureTaskService.getById(methodDTO.getCurrentTaskId());

            if (methodDTO.getIsSaveDraft() == null || !methodDTO.getIsSaveDraft()) {
                //不保存数据
                if (StringUtils.hasText(task.getSampleKey())) {
                    List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
                    for (FTaskSpecimen specimen : taskSpecimen) {
                        Boolean b = clearData(task.getId(), specimen.getSpecimenKey(), methodDTO.getClearDataRange());
                        if (!b) {
                            return Result.fail("出现异常，清除数据失败");
                        }
                        specimen.setIsTested(createType);
                    }
                    task.setSampleKey(JSON.toJSONString(taskSpecimen));
                }
                task.setMethodKey(methodId);
                task.setDeviceSerialNumber(null);
                task.setIsDelete(createType);
                fMeasureTaskService.updateById(task);

            } else {
                //保存数据
                FMeasureTask newTask = new FMeasureTask();
                newTask.setProjectItemKey(defaultProjectId);
                newTask.setUserId(userId);
                newTask.setTaskName("草稿数据" + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

                if (StringUtils.hasText(task.getSampleKey())) {
                    List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
                    for (FTaskSpecimen specimen : taskSpecimen) {

                        FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());
                        FMeasureSample newSample = new FMeasureSample();
                        BeanUtils.copyProperties(sample, newSample);
                        newSample.setId(null);

                        fMeasureSampleService.save(newSample);
                        specimen.setSpecimenKey(newSample.getId());

                        specimen.setIsTested(createType);
                    }
                    newTask.setSampleKey(JSON.toJSONString(taskSpecimen));
                }
                newTask.setId(null);
                newTask.setMethodKey(methodId);
                newTask.setDeviceSerialNumber(null);
                newTask.setIsDelete(createType);
                fMeasureTaskService.save(newTask);
                task = newTask;

            }
            ans.setTaskId(task.getId());
            ans.setTaskName(task.getTaskName());

        }

        return Result.ok(ans);
    }

    private Boolean clearData(String taskId, String sampleId, String clearDataRange) {
        if (!StringUtils.hasText(clearDataRange) || clearDataRange.equals(ChangeMethodTypeEnum.newMethod.getIdentity()) || clearDataRange.equals(ChangeMethodTypeEnum.noChange.getIdentity())) {
            log.info("没有需要清除的数据");
            return true;
        }

        LambdaQueryWrapper<FMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                .eq(FMeasureAnalysisData::getSampleKey, sampleId);
        boolean remove = fMeasureAnalysisDataService.remove(queryWrapper);
        if (!remove) {
            log.info("清除分析数据失败");
            return false;
        }

        if (clearDataRange.equals(ChangeMethodTypeEnum.measure.getIdentity())) {
            LambdaQueryWrapper<FMeasureData> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FMeasureData::getTaskId, taskId)
                    .eq(FMeasureData::getSampleKey, sampleId);
            boolean remove1 = fMeasureDataService.remove(wrapper);
            if (!remove1) {
                log.info("清除测量数据失败");
                return false;
            }
        }
        return true;

    }


    @Override
    public Result<FMeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType) {

        SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
        String id = po.getId();

        LambdaQueryWrapper<FMeasureTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FMeasureTask::getUserId, id)
                .eq(FMeasureTask::getMeasureType, measureType)
                .orderByDesc(FMeasureTask::getCreateTime);

        List<FMeasureTask> taskList = fMeasureTaskService.list(wrapper);
        if (taskList != null && !taskList.isEmpty()) {
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
//            vo.setProjectId(project.getId());
//            vo.setProjectName(project.getProjectName());
            return Result.ok(vo);
        }
        FMeasurementCurrentInfoVO vo = new FMeasurementCurrentInfoVO();

        LambdaQueryWrapper<FMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureMethod::getMethodCreator, updateBy)
                .eq(FMeasureMethod::getMeasurementType, measureType)
                .orderByDesc(FMeasureMethod::getMethodCreateTime);
        List<FMeasureMethod> list = fMeasureMethodService.list(queryWrapper);
        if (list == null || list.isEmpty()) {
            LambdaQueryWrapper<FMeasureProject> projectWrapper = new LambdaQueryWrapper<>();
            projectWrapper.eq(FMeasureProject::getCreatorKey, updateBy)
                    .orderByDesc(FMeasureProject::getCreateTime);
            List<FMeasureProject> projectList = fMeasureProjectService.list(projectWrapper);
            if (projectList == null || projectList.isEmpty()) {
                return Result.ok(vo);
            }
//            vo.setProjectName(projectList.get(0).getProjectName());
//            vo.setProjectId(projectList.get(0).getId());

        } else {
            FMeasureMethod fMeasureMethod = list.get(0);
            FMeasureProject fMeasureProject = fMeasureProjectService.getById(fMeasureMethod.getProjectItemKey());

            vo.setMethodId(fMeasureMethod.getId());
            vo.setMethodName(fMeasureMethod.getMethodName());
//            vo.setProjectId(fMeasureProject.getId());
//            vo.setProjectName(fMeasureProject.getProjectName());
        }
        return Result.ok(vo);

    }

    @Override
    public Result addOrDeleteSample(FAddOrDeleteSampleDTO dto) {

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
            FMeasureTask newTask = new FMeasureTask();

            FMeasureMethod method = fMeasureMethodService.getById(dto.getMethodId());
            SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, dto.getUpdateBy()));
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
        if (task == null) {
            return Result.fail("任务不存在");
        }

        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        for (FTaskSpecimen specimen : taskSpecimen) {
            if (dto.getDelSampleIds().contains(specimen.getSpecimenKey())) {
                specimen.setDeleteType(deleteType);
                LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(FMeasureData::getTaskId, task.getId())
                        .orderByDesc(FMeasureData::getSerialNumber);
                List<FMeasureData> list = fMeasureDataService.list(queryWrapper);
                if (list != null && !list.isEmpty()) {
                    FMeasureData one = list.get(0);
                    one.setIsDelete(deleteType);
                    fMeasureDataService.updateById(one);
                }
            }
        }

        FTaskHeadSnapshot taskHeadSnapshot = new FTaskHeadSnapshot();

        if (StringUtils.hasText(task.getTableInfo())) {
            taskHeadSnapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
        }

        if (dto.getHeads() != null && !dto.getHeads().isEmpty()) {
            taskHeadSnapshot.setSampleHeads(dto.getHeads());
        }
        taskHeadSnapshot.setTaskId(task.getId());
        task.setTableInfo(JSON.toJSONString(taskHeadSnapshot));


        for (Map<String, String> map : dto.getNewSamples()) {
            String jsonString = JSON.toJSONString(map);
            FSampleVO sample = JSON.parseObject(jsonString, FSampleVO.class);
            FMeasureSample newSample = new FMeasureSample();
            BeanUtils.copyProperties(sample, newSample);
            newSample.setId(sample.getSampleId());

            if (dto.getHeads() != null) {
                JSONObject newColData = new JSONObject();

                for (FHeadVO head : dto.getHeads()) {
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
                fMeasureSampleService.updateById(newSample);
            } else {
                FTaskSpecimen specimen = new FTaskSpecimen();
                specimen.setDeleteType(createType);
                specimen.setIsTested(noTested);
                specimen.setSampleType(newSample.getSampleType());
                log.info("新增的样品：{}", newSample);
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
                         .orderByDesc(FMeasureData::getSerialNumber);
                List<FMeasureData> list = fMeasureDataService.list(queryWrapper);
                if (list != null && !list.isEmpty()) {
                    FMeasureData one = list.get(0);
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

        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(sampleKey, FTaskSpecimen.class);
        for (FTaskSpecimen specimen : taskSpecimen) {
            if (specimen.getSpecimenKey().equals(sampleId)) {
                specimen.setDeleteType(createType);
            }
            LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                    .eq(FMeasureData::getTaskId, task.getId())
                     .orderByDesc(FMeasureData::getSerialNumber);
            List<FMeasureData> list = fMeasureDataService.list(queryWrapper);
            if (list != null && !list.isEmpty()) {
                FMeasureData one = list.get(0);
                one.setIsDelete(createType);
                fMeasureDataService.updateById(one);
            }

        }
        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);

        return Result.ok(fMeasureTaskService.updateById(task));
    }

    @Override
    public Result<List<JSONObject>> newQuerySampleInfo(String taskId) {
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
    public Result<List<FHeadVO>> querySampleHeads(String taskId) {

        if (!StringUtils.hasText(taskId)) {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
            return Result.ok(preHead);
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        if (!StringUtils.hasText(task.getTableInfo())) {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
            return Result.ok(preHead);
        } else {
            FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
            if (snapshot.getSampleHeads() != null && !snapshot.getSampleHeads().isEmpty()) {
                return Result.ok(snapshot.getSampleHeads());
            } else {
                List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
                return Result.ok(preHead);
            }
        }
    }

    @Override
    public Result<FMethodParam> queryMethodParam(String methodId) {

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }

        if (!StringUtils.hasText(method.getMeasurementParamInfo())) {
            return Result.fail("未查询到该方法参数");
        }
        FPhotometricParameter photometricParameter = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);

        FMethodParam param = new FMethodParam();
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
    public R<FPhotometricReportDataVO> getReportCount(String taskId) {

        if (!StringUtils.hasText(taskId)) {
            return R.fail("缺少任务id");
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return R.fail("任务不存在");
        }
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        FPhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);

        if (parameter.getResultParams() == null || parameter.getResultParams().isEmpty()) {
            return R.fail("缺少结果参数");
        }

        List<FHeadVO> heads = new ArrayList<>();

        List<Map<String, Object>> data = new ArrayList<>();
        FPhotometricReportDataVO ans = new FPhotometricReportDataVO();

        heads.add(new FHeadVO("样品编号", "sampleNumber"));
        heads.add(new FHeadVO("样品名称", "sampleName"));
        heads.add(new FHeadVO("样品类型", "sampleType"));
        for (FPreCountResultParam param : parameter.getResultParams()) {
            heads.add(new FHeadVO(param.getResultName(), param.getResultIdentity() + "-" +param.getFormulaText()));
        }
        ans.setHeadList(heads);

        if (StringUtils.hasText(task.getSampleKey())) {
            List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);

            for (FTaskSpecimen specimen : taskSpecimen) {
                Map<String, Object> map = new HashMap<>();
                FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());
                map.put("sampleName", sample.getSampleName());
                map.put("sampleNumber", sample.getSampleNumber());
                map.put("sampleType", SampleTypeEnum.getName(sample.getSampleType()));

                LambdaQueryWrapper<FMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                        .eq(FMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey());
                List<FMeasureAnalysisData> list = fMeasureAnalysisDataService.list(queryWrapper);
                if (list != null && !list.isEmpty()) {
                    FMeasureAnalysisData analysisData = list.get(0);
                    List<FPhotometricCountDataInfo> countDataInfos = JSON.parseArray(analysisData.getAnalysisResult(), FPhotometricCountDataInfo.class);

                    if (countDataInfos != null && !countDataInfos.isEmpty()) {
                        for (FPhotometricCountDataInfo count : countDataInfos) {
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
    public Result<List<FHeadVO>> tableHead(String taskId, String methodId, Boolean allDisplay) {
        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(methodId)) {
            return Result.fail("请选择方法或任务");
        }

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }
        FPhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);
        LinkedHashSet<FHeadVO> ans = new LinkedHashSet<>();

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_stand.getIdentity());
            ans.addAll(preHead);
            return Result.ok(new ArrayList<>(ans));
        }

        if (StringUtils.hasText(task.getTableInfo())) {
            FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
            if (snapshot != null) {
                if (snapshot.getTaskHeads()!= null && !snapshot.getTaskHeads().isEmpty()) {
                    ans.addAll(snapshot.getTaskHeads());
                }
                if (snapshot.getSampleHeads() != null && !snapshot.getSampleHeads().isEmpty()) {
                    ans.addAll(snapshot.getSampleHeads());
                }
            }
        } else {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_stand.getIdentity());
            ans.addAll(preHead);
        }

        if (StringUtils.hasText(task.getSampleKey())) {

            List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
            for (FTaskSpecimen specimen : taskSpecimen) {
                FMeasureSample measureSample = fMeasureSampleService.getById(specimen.getSpecimenKey());
                if (StringUtils.hasText(measureSample.getHeads())){
                    List<FHeadVO> heads = JSON.parseArray(measureSample.getHeads(), FHeadVO.class);

                    if (heads != null && !heads.isEmpty()) {
                        ans.addAll(heads);
                    }
                }
            }

        }

        List<FHeadVO> vos = dealHeads(new ArrayList<>(ans), parameter, allDisplay);
        return Result.ok(new ArrayList<>(vos));
    }


    private List<FHeadVO> dealHeads(List<FHeadVO> heads, FPhotometricParameter parameter, Boolean allDisplay) {
        List<String> waveLength = parameter.getWaveLength();
        Map<String, FHeadVO> map = heads.stream().collect(Collectors.toMap(FHeadVO::getIdentity, head -> head));

        Map<String, List<FHeadVO>> headsMap = new HashMap<>();

        Map<String, FHeadVO> analyzeMap = new HashMap<>();

        for (FHeadVO head : heads) {
            headsMap.computeIfAbsent(head.getLocate(), k -> new ArrayList<>()).add(head);
            if (head.getLocate().equals(TableHeadILocateEnums.measure.getIdentity())) {
                map.put(head.getIdentity(), head);
            }
            if (head.getLocate().equals(TableHeadILocateEnums.analyze.getIdentity())) {
                analyzeMap.put(head.getIdentity(), head);
            }
        }

        List<FHeadVO> allMeasureHeads = new ArrayList<>();

        String mode = parameter.getLuminosity();
        if (mode.equals(WaveLengthEnum.es.getCode()) || mode.equals(WaveLengthEnum.er.getCode())) {
            for (String s : waveLength) {
                String identity = s + "-"+ mode;
                String name = s + WaveLengthEnum.getName(mode);
                FHeadVO newHead = new FHeadVO(name, identity, TableHeadILocateEnums.measure.getIdentity(), name);
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

                FHeadVO absHead = new FHeadVO(absName, absIdentity, TableHeadILocateEnums.measure.getIdentity(), absName);
                FHeadVO tPercentHead = new FHeadVO(tPercentName, tPercentIdentity, TableHeadILocateEnums.measure.getIdentity(), tPercentName);

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
        List<FHeadVO> ans = new ArrayList<>();
        List<FHeadVO> systemHeads = headsMap.get(TableHeadILocateEnums.system.getIdentity());
        if (systemHeads != null) ans.addAll(systemHeads);

        List<FHeadVO> sampleHeads = headsMap.get(TableHeadILocateEnums.sample.getIdentity());
        if (sampleHeads != null) ans.addAll(sampleHeads);
        ans.addAll(allMeasureHeads);
        List<FHeadVO> taskHeads = headsMap.get(TableHeadILocateEnums.data.getIdentity());
        if (taskHeads != null)ans.addAll(taskHeads);

        List<FPreCountResultParam> resultParams = parameter.getResultParams();
        List<FHeadVO> headAnalyze = new ArrayList<>();
        for (FPreCountResultParam results : resultParams) {
            //TODO 方法参数标识待定，后续再改动
            if (!StringUtils.hasText(results.getParam())) {
                results.setParam(results.getResultIdentity() + "-" + results.getFormulaText());
            }
            String name = results.getResultName() + "(" + results.getUnit() + ")";
            FHeadVO analyzeHead = new FHeadVO(name, results.getParam(), TableHeadILocateEnums.analyze.getIdentity(), name);

            if (analyzeMap.containsKey(results.getParam())) {
                analyzeHead = analyzeMap.get(results.getParam());
            }
            headAnalyze.add(analyzeHead);
        }
        ans.addAll(headAnalyze);

        List<FHeadVO> other = headsMap.get(TableHeadILocateEnums.other.getIdentity());
        if (other == null || other.isEmpty()) {
            other = new ArrayList<>();
            FHeadVO samplingTimeHead = new FHeadVO(SampleTableEnum.samplingTime.getName(), SampleTableEnum.samplingTime.getIdentity(), TableHeadILocateEnums.other.getIdentity(), SampleTableEnum.samplingTime.getName());
            FHeadVO remark = new FHeadVO(SampleTableEnum.remark.getName(), SampleTableEnum.remark.getIdentity(), TableHeadILocateEnums.data.getIdentity(), SampleTableEnum.remark.getName());
            other.add(samplingTimeHead);
            other.add(remark);
        }

        ans.addAll(other);

        return ans;
    }
    
    @Override
    public R<List<FPhotometricCountDataInfo>> formula(List<FPhotometricOriginDataInfo> vo, FMeasureSample sample, FMeasureTask task, FPhotometricParameter parameter, Integer digit) {

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

        for (FPhotometricOriginDataInfo dataInfo : vo) {
            if (dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                allData.putAll(dataInfo.getData());
            }
        }

        StringBuilder values = new StringBuilder();

        StringBuilder identity = new StringBuilder();

        for (FInputParam inputParam : parameter.getSampleInputParams()) {

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

        List<FPhotometricCountDataInfo> countList = new ArrayList<>();
        for (FPreCountResultParam param : parameter.getResultParams()) {
            String[] split = param.getFormulaText().split("=");
            String text = split[split.length - 1];
            String result = restUtil.sum(text, identity.toString(), values.toString());
            FPhotometricCountDataInfo countDataInfo = new FPhotometricCountDataInfo();
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
    public Result<List<Map<String, Object>>> queryData(FQueryDataDTO queryDataDTO) {
        String taskId = queryDataDTO.getTaskId();
        Boolean allDisplay = queryDataDTO.getAllDisplay();
        List<String> option = queryDataDTO.getOption();

        if (!StringUtils.hasText(taskId) || allDisplay == null) {
            return Result.fail("未选择任务");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
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

        R<FMeasureTaskAttachment> attachmentResult = fMeasureTaskAttachmentService.queryByTaskId(taskId);
        if (!attachmentResult.getSuccess()) {
            FMeasureTaskAttachment taskAttachment = new FMeasureTaskAttachment();
            taskAttachment.setTaskId(taskId);
            taskAttachment.setDataParam(JSON.toJSONString(queryDataDTO));
            fMeasureTaskAttachmentService.save(taskAttachment);
            attachmentResult = fMeasureTaskAttachmentService.queryByTaskId(taskId);
        }
        FMeasureTaskAttachment attachment = attachmentResult.getData();
        if (!StringUtils.hasText(attachment.getDataParam())) {
            attachment.setDataParam(JSON.toJSONString(queryDataDTO));
            fMeasureTaskAttachmentService.saveOrUpdate(attachment);
        }


        boolean repeat = false;
        FPhotometricParameter parameter = null;
        if (StringUtils.hasText(task.getMethodKey())) {
            FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                parameter= JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);
                if (StringUtils.hasText(parameter.getRepetitionPattern()) && parameter.getRepetitionPattern().equals("单池重复")) {
                    repeat = true;
                }
            }
        }

        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);

        for (FTaskSpecimen specimen : taskSpecimen) {
            if (!allDisplay && specimen.getDeleteType().equals("1")) {
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());

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

            LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FMeasureData::getTaskId, taskId)
                    .eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                    .orderByDesc(FMeasureData::getSerialNumber)
                    .last("limit 1");
            FMeasureData data = fMeasureDataService.getOne(queryWrapper);
            if (!repeat) {
                if (data != null) {
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        List<FPhotometricOriginDataInfo> wlDataList = JSON.parseArray(data.getOriginalCalculationData(), FPhotometricOriginDataInfo.class);

                        for (FPhotometricOriginDataInfo wlData : wlDataList) {
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
                        List<FPhotometricOriginDataInfo> wlDataList = JSON.parseArray(data.getOriginalCalculationData(), FPhotometricOriginDataInfo.class);

                        FPhotometricOriginDataInfo dataInfo = reCountData(wlDataList, digit);
                        for (FPhotometricOriginDataInfo wlData : wlDataList) {

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

    public Map<String,String> getCountData(FMeasureTask task, FMeasureSample sample, FPhotometricParameter parameter, Integer digit, List<FPhotometricOriginDataInfo> wlDataList) {

        Map<String, String> map = new HashMap<>();

        R<List<FPhotometricCountDataInfo>> formula = formula(wlDataList, sample, task, parameter, digit);
        if (formula.getSuccess()) {
            List<FPhotometricCountDataInfo> formulaData = formula.getData();
            for (FPhotometricCountDataInfo count : formulaData) {
                String s = DataCountUtil.formatValueWithBigDecimal(digit, count.getCountValue());
                map.put(count.getResultIdentity() + "-" + count.getFormulaText(), s);
            }
        }

        LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, task.getId())
                .eq(FMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(FMeasureAnalysisData::getAnalysisName).last("limit 1");

        FMeasureAnalysisData one = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (one != null) {
            List<FPhotometricCountDataInfo> countList = JSON.parseArray(one.getAnalysisResult(), FPhotometricCountDataInfo.class);

            if (map.isEmpty()) {
                for (FPhotometricCountDataInfo count : countList) {

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

    private FPhotometricOriginDataInfo reCountData(List<FPhotometricOriginDataInfo> wlDataList, Integer digit) {

        Map<String, List<Double>> data = new HashMap<>();
        Map<String, FRepeatMeasureData> otherData = new HashMap<>();
        Map<String, String> ans = new HashMap<>();
        FPhotometricOriginDataInfo result = new FPhotometricOriginDataInfo();

        for (FPhotometricOriginDataInfo wlData : wlDataList) {
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

            FRepeatMeasureData repeatMeasureData = new FRepeatMeasureData();
            repeatMeasureData.setSd(sd == null ? "未检出" :String.valueOf(sd));
            repeatMeasureData.setRsd(rsd == null ? "未检出" :String.valueOf(rsd));
            otherData.put(entry.getKey(), repeatMeasureData);
            ans.put(entry.getKey(), String.valueOf(mean));
        }
        result.setData(ans);
        result.setOtherData(otherData);
        return result;
    }

    private Map<String, Map<String, String>> extracted(List<String> option, FPhotometricOriginDataInfo wlData, FMeasureSample sample) {

        Map<String, String> originData = wlData.getData();
        Map<String, Map<String, String>> ans = new HashMap<>();
        String mapName = wlData.getWavelength() + "-" + wlData.getMode();

        FRepeatMeasureData absData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode());
        FRepeatMeasureData tPercentData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode());


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
                        FRepeatMeasureData measureData = wlData.getOtherData().get(mapName);
                        children.put(mapName, measureData.getSd());
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.sd.getIdentity(), children);
                    }
                    if (DataIdentityEnums.rsd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getName());
                        FRepeatMeasureData measureData = wlData.getOtherData().get(mapName);
                        children.put(mapName, measureData.getRsd());
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.rsd.getIdentity(), children);
                    }
                }
            }
        }

        return ans;
    }

    @Override
    public R<String> savePhotometricMeasureData(FPhotometricSampleAndDataVO sampleAndDataVO) {

        String taskId = sampleAndDataVO.getTaskId();
        String sampleId = sampleAndDataVO.getSampleId();
        LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureData::getTaskId, taskId)
                .eq(FMeasureData::getSampleKey, sampleId)
                .orderByDesc(FMeasureData::getSerialNumber).last("limit 1");
        FMeasureData fMeasureData = fMeasureDataService.getOne(queryWrapper);

        if (fMeasureData == null) {
            fMeasureData = new FMeasureData();
            fMeasureData.setSerialNumber(0);
        }

        if (StringUtils.hasText(fMeasureData.getOriginalCalculationData()) || StringUtils.hasText(fMeasureData.getInstrumentData())) {
            fMeasureData = new FMeasureData();
            fMeasureData.setSerialNumber(fMeasureData.getSerialNumber() + 1);
        }
        fMeasureData.setTaskId(taskId);
        fMeasureData.setSampleKey(sampleId);
        fMeasureData.setInstrumentData(JSON.toJSONString(sampleAndDataVO.getMachineData()));
        fMeasureData.setOriginalCalculationData(JSON.toJSONString(sampleAndDataVO.getWavelengthData()));
        fMeasureData.setMeasurementStartTime(sampleAndDataVO.getStartTime());
        fMeasureData.setMeasurementEndTime(sampleAndDataVO.getEndTime());
        fMeasureData.setSamplingTime(sampleAndDataVO.getSampleTime());

        fMeasureData.setIsDelete(createType);
        fMeasureDataService.saveOrUpdate(fMeasureData);

        return R.ok(fMeasureData.getId());
    }

    @Override
    public R<Boolean> saveCountData(FPhotometricSampleAndDataVO countData, String dataId) {

        if (countData.getCountData() == null || countData.getCountData().isEmpty()) {
            return R.fail("没有要保存的数据");
        }
        String taskId = countData.getTaskId();
        String sampleId = countData.getSampleId();

        LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                .eq(FMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(FMeasureAnalysisData::getDataVersion).last("limit 1");
        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (analysisData != null) {
            if (StringUtils.hasText(analysisData.getMeasureDataId())) {
                analysisData.setId(null);
                analysisData.setDataVersion(analysisData.getDataVersion() + 1);
            }
            analysisData.setMeasureDataId(dataId);

            analysisData.setAnalysisResult(JSON.toJSONString(countData.getCountData()));
        } else {
            analysisData = new FMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setAnalysisResult(JSON.toJSONString(countData.getCountData()));
            analysisData.setDataVersion(0);
            analysisData.setMeasureDataId(dataId);
        }
        analysisData.setIsDelete(createType);
        fMeasureAnalysisDataService.saveOrUpdate(analysisData);
        return R.ok(true);

    }

    @Override
    public Result saveDataInfo(FSaveDataVO vo) {
        if (!StringUtils.hasText(vo.getTaskId())) {
            return Result.fail("请创建任务");
        }

        if (!StringUtils.hasText(vo.getSampleId())) {
            return Result.fail("缺少样品id");
        }

        if (vo.getData() == null || vo.getData().isEmpty()) {
            return Result.ok();
        }
        String taskId = vo.getTaskId();
        String sampleId = vo.getSampleId();
        LambdaQueryWrapper<FMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                .eq(FMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(FMeasureAnalysisData::getDataVersion).last("limit 1");

        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(queryWrapper);
        if (analysisData != null) {
            Map<String, String> jsonObject;
            if (StringUtils.hasText(analysisData.getAnalysisParam())) {
                jsonObject = JSON.parseObject(analysisData.getAnalysisParam(), new TypeReference<Map<String, String>>(){});
            } else {
                jsonObject = vo.getData();
            }
            analysisData.setAnalysisParam(JSON.toJSONString(jsonObject));
            fMeasureAnalysisDataService.updateById(analysisData);
        } else {
            analysisData = new FMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setAnalysisParam(JSON.toJSONString(vo.getData()));
            analysisData.setDataVersion(0);
            analysisData.setIsDelete(createType);
            fMeasureAnalysisDataService.save(analysisData);
        }

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

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }

        if (!StringUtils.hasText(taskId)){
            //创建任务

            //创建任务
            FMeasureTask newTask = new FMeasureTask();

            SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
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
        FPhotometricParameter photometricParameter = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);
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

        boolean update = fMeasureTaskService.updateById(task);
        return update ? Result.ok() : Result.fail();
    }

    @Override
    public Result<Boolean> saveButton(String taskId, String updateBy) {
        if (!StringUtils.hasText(taskId)) {
            return Result.fail("未创建任务，无法保存");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
        String id = po.getId();
        task.setUserId(id);
        task.setIsDelete("1");
        fMeasureTaskService.updateById(task);
        return Result.ok(true);
    }

    @Override
    public Result<FMeasurementCurrentInfoVO> getInfoByTaskId(String taskId) {
        if (!StringUtils.hasText(taskId)){
            return Result.fail("缺少任务id");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        FMeasurementCurrentInfoVO measurementCurrentInfoVO = new FMeasurementCurrentInfoVO();
        measurementCurrentInfoVO.setTaskId(taskId);
        measurementCurrentInfoVO.setTaskName(task.getTaskName());
        measurementCurrentInfoVO.setMethodId(task.getMethodKey());
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        measurementCurrentInfoVO.setMethodName(method.getMethodName());
//        measurementCurrentInfoVO.setProjectId(task.getProjectItemKey());
        FMeasureProject project = fMeasureProjectService.getById(task.getProjectItemKey());
//        measurementCurrentInfoVO.setProjectName(project.getProjectName());

        return Result.ok(measurementCurrentInfoVO);
    }

    @Override
    public R<File> downloadWord(String taskId, Boolean allDisplay) {
        if (!StringUtils.hasText(taskId)) {
            return R.fail("缺少任务id");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return R.fail("任务不存在");
        }

        String fileName = task.getTaskName() + ".docx";

        String methodId = task.getMethodKey();
        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        List<FHeadVO> heads = new ArrayList<>();

        heads.add(new FHeadVO("样品编号", "sampleNumber"));
        heads.add(new FHeadVO("样品名称", "sampleName"));
        heads.add(new FHeadVO("样品类型", "sampleType"));
        heads.add(new FHeadVO("稀释比", "dilutionRatio"));
        heads.add(new FHeadVO("样品体积", "sampleVolume"));
        heads.add(new FHeadVO("样品位置", "sampleLocation"));

        List<FHeadVO> vos = tableHead(taskId, methodId, allDisplay).getData();
        heads.addAll(vos);
        heads.add(new FHeadVO("备注", "remark"));
        heads.add(new FHeadVO("采样时间", "samplingTime"));

        FQueryDataDTO queryDataDTO = new FQueryDataDTO();
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
                for (FHeadVO head : heads) {

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
    public R<List<FInputParam>> queryInputParam(String methodId) {
        if (!StringUtils.hasText(methodId)) {
            return R.fail("未选择方法");
        }
        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return R.fail("方法不存在");
        }
        FPhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);
        List<FInputParam> inputParams = parameter.getSampleInputParams();

        return R.ok(inputParams);
    }

    @Override
    public R<List<FInputParamMapTableHead>> queryInputParamMapTableHead(String taskId) {

        if (!StringUtils.hasText(taskId)) {
            return R.fail("未选择任务");
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return R.fail("任务不存在");
        }
        List<FInputParamMapTableHead> ans = new ArrayList<>();
        if (task.getTableInfo() != null) {
            FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
            ans = snapshot.getMapRelation();
        }
        return R.ok(ans);

    }

    @Override
    public R<Boolean> saveInputParamMapTableHead(FTaskHeadSnapshotDTO snapshotDTO) {

        if (!StringUtils.hasText(snapshotDTO.getTaskId())) {
            return R.fail("未选择任务");
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

        task.setTableInfo(JSON.toJSONString(taskHeadSnapshot));

        return fMeasureTaskService.updateById(task) ? R.ok(true) : R.fail("保存失败");
    }


}
