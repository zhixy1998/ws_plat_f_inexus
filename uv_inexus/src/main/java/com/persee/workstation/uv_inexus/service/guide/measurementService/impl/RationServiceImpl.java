package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskHeadSnapshot;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingParam;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingReceiveInfo;
import com.persee.workstation.uv_inexus.util.pythonUtil.RestUtil;
import com.persee.workstation.uv_inexus.vo.measurementVo.InputParam;
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
    TMeasureTaskMapper tMeasureTaskMapper;
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
    TMeasureTaskAttachmentService taskAttachmentService;

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
        // 通用参数：方法名称
        if (methodDto.getMethodName() != null && methodDto.getMethodName().length() > 256) {
            return Result.fail("方法名格式错误");
        }
        // 通用参数：参考标准方法长度（≤256）
        if (methodDto.getReferenceStandardName() != null && methodDto.getReferenceStandardName().length() > 256) {
            return Result.fail("参考标准方法长度超限");
        }
        // 通用参数：前处理参数 每个成员≤500字符，最多10个
        if (methodDto.getPreProcessParameters() != null) {
            if (methodDto.getPreProcessParameters().size() > 10) {
                return Result.fail("前处理参数个数超限");
            }
            for (String p : methodDto.getPreProcessParameters()) {
                if (p != null && p.length() > 500) {
                    return Result.fail("前处理参数长度超限");
                }
            }
        }
        // 测量类参数（定量测定）
        // 波长值：1-3个成员，数值190-900，保留1位小数
        if (methodDto.getWaveLength() != null) {
            if (methodDto.getWaveLength().size() < 1 || methodDto.getWaveLength().size() > 3) {
                return Result.fail("波长个数需在1-3之间");
            }
            for (String wl : methodDto.getWaveLength()) {
                try { double v = Double.parseDouble(wl); if (v < 190.0 || v > 900.0) return Result.fail("波长超出范围(190-900)"); String s = String.format(java.util.Locale.ROOT, "%.1f", v); if (!s.equals(String.valueOf(v)) && !String.valueOf(v).matches("^\\d+\\.\\d$")) return Result.fail("波长需保留1位小数"); } catch (Exception e) { return Result.fail("波长格式错误"); }
            }
        }
        // 波长系数：1-3个成员
        if (methodDto.getCoefficientParams() != null) {
            if (methodDto.getCoefficientParams().size() < 1 || methodDto.getCoefficientParams().size() > 3) {
                return Result.fail("波长系数个数需在1-3之间");
            }
        }
        // 曲线次数：1-4
        if (org.springframework.util.StringUtils.hasText(methodDto.getCurveType())) {
            try { int n = Integer.parseInt(methodDto.getCurveType()); if (n < 1 || n > 4) return Result.fail("曲线次数范围应为1-4"); } catch (Exception ignored) {}
        }
        // 方程类型：枚举
        if (StringUtils.hasText(methodDto.getEquationType())) {
          Set<String> allow = new HashSet<>(Arrays.asList("qtCFA","qtAFC"));
            if (!allow.contains(methodDto.getEquationType()))
                return Result.fail("方程类型不合法");
        }
        // 零点方式：字节，默认0 - 此处放宽，若提供需为0-255的整数
        // 曲线系数方式：枚举
        if (org.springframework.util.StringUtils.hasText(methodDto.getCoefficientMode())) {
            Set<String> allow = new HashSet<>(Arrays.asList("ctInput","ctCurve","0","1"));
            if (!allow.contains(methodDto.getCoefficientMode())) return Result.fail("曲线系数方式不合法");
        }
        // 曲线系数数据：1-10 个成员
        if (methodDto.getCoefficientParams() != null && (methodDto.getCoefficientParams().size() < 1 || methodDto.getCoefficientParams().size() > 10)) {
            return Result.fail("曲线系数数据个数需在1-10之间");
        }
        // 拟合结果单位：1-10字符
        // 该字段在定量分析结果中，若存在则校验
        if (methodDto.getResultParams() != null) {
            for (PreCountResultParam r : methodDto.getResultParams()) {
                if (r.getUnit() != null && (r.getUnit().length() < 1 || r.getUnit().length() > 10)) {
                    return Result.fail("结果单位长度应为1-10");
                }
            }
        }

        // 根据方法id查询当前方法
        TMeasureMethod method = tMeasureMethodService.getById(methodDto.getMethodId());
        if (method == null) {
            return Result.fail("方法不存在");
        }
        RationParameter parameter = new RationParameter();
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
        for (PreCountResultParam resultParam : parameter.getResultParams()) {
            resultParam.setParam(resultParam.getResultIdentity() + "-" + resultParam.getFormulaText());
        }

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

        if (!StringUtils.hasText(sampleKind)) {
            log.info("缺少样品种类");
            return Result.fail("缺少样品种类");
        }

        if (!StringUtils.hasText(taskId)) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), sampleKind);
            return Result.ok(preHead);
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        if (!StringUtils.hasText(task.getTableInfo())) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), sampleKind);
            return Result.ok(preHead);
        } else {
            RationTaskHeadSnapShot snapShot = JSON.parseObject(task.getTableInfo(), RationTaskHeadSnapShot.class);
            if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity())) {
                TaskHeadSnapshot standHead = snapShot.getStandHead();
                if (standHead != null && standHead.getSampleHeads()!=null && !standHead.getSampleHeads().isEmpty()) {
                    return Result.ok(standHead.getSampleHeads());
                } else {
                    List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), sampleKind);
                    return Result.ok(preHead);
                }
            } else if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                TaskHeadSnapshot unknownHead = snapShot.getUnknownHead();
                if (unknownHead != null && unknownHead.getSampleHeads()!=null && !unknownHead.getSampleHeads().isEmpty()) {
                    return Result.ok(unknownHead.getSampleHeads());
                } else {
                    List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), sampleKind);
                    return Result.ok(preHead);
                }
            } else {
                return Result.fail("样品种类出错");
            }
        }

    }


    @Override
    @Transactional
    public Result addOrDeleteSample(RationAddOrDeleteSampleDTO dto) {

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

        RationTaskHeadSnapShot taskHeadSnapShot = new RationTaskHeadSnapShot();
        if (StringUtils.hasText(task.getTableInfo())) {
            taskHeadSnapShot = JSON.parseObject(task.getTableInfo(), RationTaskHeadSnapShot.class);
        }

        /*
        处理表头
         */
        if (dto.getStandHeads() != null && !dto.getStandHeads().isEmpty()) {
            TaskHeadSnapshot standHead = new TaskHeadSnapshot();
            if (taskHeadSnapShot.getStandHead() != null) {
                BeanUtils.copyProperties(taskHeadSnapShot.getStandHead(), standHead);
            }
            standHead.setTaskId(task.getId());
            standHead.setSampleHeads(dto.getStandHeads());
            taskHeadSnapShot.setStandHead(standHead);
        }

        if (dto.getUnknowHeads() != null && !dto.getUnknowHeads().isEmpty()) {
            TaskHeadSnapshot unknownHead = new TaskHeadSnapshot();
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
            SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);

            TMeasureSample newSample = new TMeasureSample();
            BeanUtils.copyProperties(sample, newSample);
            newSample.setId(sample.getSampleId());
            newSample.setSampleKind(stand);
            if (dto.getStandHeads() != null && !dto.getStandHeads().isEmpty()) {
                JSONObject newColData = new JSONObject();

                for (HeadVO head : dto.getStandHeads()) {
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
                tMeasureSampleService.updateById(newSample);
            } else {
                TaskSpecimen specimen = new TaskSpecimen();
                specimen.setDeleteType(createType);
                specimen.setIsTested(noTested);
                specimen.setSampleType(newSample.getSampleType());
                specimen.setSampleKind(stand);
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
                specimen.setSampleKind(unknown);
                specimen.setSampleType(newSample.getSampleType());
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

        RationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), RationTaskHeadSnapShot.class);

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(sampleKey, TaskSpecimen.class);
        for (TaskSpecimen specimen : taskSpecimen) {
            if (specimen.getSpecimenKey().equals(sampleId)) {
                specimen.setDeleteType(createType);
                TMeasureSample newSample = tMeasureSampleService.getById(sampleId);
                if (specimen.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                    newSample.setHeads(String.valueOf(shot.getUnknownHead().getSampleHeads()));
                } else if (specimen.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                    newSample.setHeads(String.valueOf(shot.getStandHead().getSampleHeads()));
                }
                tMeasureSampleService.updateById(newSample);

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
        if(methodId.length() != 32){
            return Result.fail("方法ID格式应为32位");
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

        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);

        //根据任务中记录的样品id判断是否存在空白样品
        TMeasureTask task = tMeasureTaskService.getById(taskId);

        if (task == null) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_stand.getIdentity());
            ans.addAll(preHead);
            return Result.ok(new ArrayList<>(ans));
        }

        if (StringUtils.hasText(task.getTableInfo())) {
            RationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), RationTaskHeadSnapShot.class);
            if (shot != null && shot.getStandHead() != null) {
                if (shot.getStandHead().getTaskHeads() != null) {
                    ans.addAll(shot.getStandHead().getTaskHeads());
                }
                if (shot.getStandHead().getSampleHeads() != null) {
                    ans.addAll(shot.getStandHead().getSampleHeads());
                }
            }
        } else {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_stand.getIdentity());
            ans.addAll(preHead);
        }

        boolean empty = false;
        if (StringUtils.hasText(task.getSampleKey())) {
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
                    if (heads != null && !heads.isEmpty()) {
                        ans.addAll(heads);
                    }
                }
            }
        }
        List<HeadVO> result = dealHeads(new ArrayList<>(ans), parameter, SampleTypeEnum.kind_stand.getIdentity(), allDisplay, empty);
        return Result.ok(result);

    }

    private List<HeadVO> dealHeads(List<HeadVO> heads, RationParameter parameter, String sampleKind, Boolean allDisplay, Boolean empty) {
        List<String> waveLength = parameter.getWaveLength();
        Map<String, HeadVO> map = heads.stream().collect(Collectors.toMap(HeadVO::getIdentity, head -> head));

        for (String s : waveLength) {
            String identity = s + "-"+ WaveLengthEnum.abs.getCode();
            String tPercentIdentity = s + "-"+ WaveLengthEnum.tPercent.getCode();
            String absName = s + WaveLengthEnum.abs.getName();
            String tPercentName = s + WaveLengthEnum.tPercent.getName();

            if (!map.containsKey(identity)) {
                HeadVO newHead = new HeadVO(absName, identity, TableHeadILocateEnums.measure.getIdentity(), absName);
                heads.add(newHead);
            }

            if (allDisplay) {
                if (!map.containsKey(tPercentIdentity)) {
                    HeadVO newHead = new HeadVO(tPercentName, tPercentIdentity, TableHeadILocateEnums.measure.getIdentity(), tPercentName);
                    heads.add(newHead);
                }
            } else {
                if (map.containsKey(tPercentIdentity)) {
                    HeadVO vo = map.get(tPercentIdentity);
                    heads.remove(vo);
                }
            }
        }
        String sumAbsIdentity = SampleTableEnum.sumAbs.getIdentity();
        String absIdentity = SampleTableEnum.Abs.getIdentity();
        if (!map.containsKey(absIdentity)) {
            HeadVO newHead = new HeadVO(SampleTableEnum.Abs.getName(), absIdentity, TableHeadILocateEnums.measure.getIdentity(), SampleTableEnum.Abs.getName());
            heads.add(newHead);
        }
        if (empty) {
            if (!map.containsKey(sumAbsIdentity)) {
                HeadVO newHead = new HeadVO(SampleTableEnum.sumAbs.getName(), sumAbsIdentity, TableHeadILocateEnums.measure.getIdentity(), SampleTableEnum.sumAbs.getName());
                heads.add(newHead);
            }
        } else {
            if (map.containsKey(sumAbsIdentity)) {
                HeadVO vo = map.get(sumAbsIdentity);
                heads.remove(vo);
            }
        }

        if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
            List<PreCountResultParam> resultParams = parameter.getResultParams();
            for (PreCountResultParam results : resultParams) {
                //TODO 方法参数标识待定，后续再改动
                if (!StringUtils.hasText(results.getParam())) {
                    results.setParam(results.getResultIdentity() + "-" + results.getFormulaText());
                }
                if (!map.containsKey(results.getParam())) {
                    String name = results.getResultName() + "(" + results.getUnit() + ")";
                    HeadVO newHead = new HeadVO(name, results.getParam(), TableHeadILocateEnums.analyze.getIdentity(), name);
                    heads.add(newHead);
                }
            }
        }

        return heads;
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

        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);

        TMeasureTask task = tMeasureTaskService.getById(taskId);

        if (task == null) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_unknown.getIdentity());
            ans.addAll(preHead);
            return Result.ok(new ArrayList<>(ans));
        }

        if (StringUtils.hasText(task.getTableInfo())) {
            RationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), RationTaskHeadSnapShot.class);
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
                    if (heads != null && !heads.isEmpty()) {
                        ans.addAll(heads);
                    }
                }
            }
        }
        List<HeadVO> result = dealHeads(new ArrayList<>(ans), parameter, SampleTypeEnum.kind_unknown.getIdentity(), allDisplay, empty);
        return Result.ok(result);
    }

    @Override
    public R<String> saveRationSampleMeasureData(RationDataVO dataInfo) {

        String sampleId = dataInfo.getSampleId();
        String taskId = dataInfo.getTaskId();

        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getSampleKey, sampleId)
                .orderByDesc(TMeasureData::getSerialNumber)
                .last("limit 1");
        TMeasureData data = tMeasureDataService.getOne(queryWrapper);

        TMeasureData tMeasureData = new TMeasureData();
        tMeasureData.setTaskId(taskId);
        tMeasureData.setSampleKey(sampleId);
        tMeasureData.setInstrumentData(JSON.toJSONString(dataInfo.getMachineData()));
        tMeasureData.setOriginalCalculationData(JSON.toJSONString(dataInfo.getWavelengthData()));
        tMeasureData.setMeasurementStartTime(dataInfo.getStartTime());
        tMeasureData.setMeasurementEndTime(dataInfo.getEndTime());
        tMeasureData.setSamplingTime(dataInfo.getSampleTime());

        if (data != null) {
            tMeasureData.setSerialNumber(data.getSerialNumber() + 1);
        } else {
            tMeasureData.setSerialNumber(0);
        }
        tMeasureData.setIsDelete(createType);
        tMeasureDataService.save(tMeasureData);

        LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(TMeasureAnalysisData::getDataVersion)
                .last("limit 1");
        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (analysisData != null && analysisData.getMeasureDataId() == null) {

            analysisData.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            analysisData.setMeasureDataId(tMeasureData.getId());
            analysisData.setIsDelete(createType);
            tMeasureAnalysisDataService.updateById(analysisData);

        } else {
            analysisData = new TMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setMeasureDataId(tMeasureData.getId());
            analysisData.setDataVersion(0);
            analysisData.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            analysisData.setIsDelete(createType);
            tMeasureAnalysisDataService.save(analysisData);
        }


        return R.ok(tMeasureData.getId());

    }

    @Override
    public R<RationSaveCountDataInfo> saveSampleMeasureData(RationDataVO dataInfo) {
        String sampleId = dataInfo.getSampleId();
        String taskId = dataInfo.getTaskId();

        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getSampleKey, sampleId)
                .orderByDesc(TMeasureData::getSerialNumber)
                .last("limit 1");
        TMeasureData data = tMeasureDataService.getOne(queryWrapper);

        TMeasureData tMeasureData = new TMeasureData();
        tMeasureData.setTaskId(taskId);
        tMeasureData.setSampleKey(sampleId);
        tMeasureData.setInstrumentData(JSON.toJSONString(dataInfo.getMachineData()));
        tMeasureData.setOriginalCalculationData(JSON.toJSONString(dataInfo.getWavelengthData()));
        tMeasureData.setMeasurementStartTime(dataInfo.getStartTime());
        tMeasureData.setMeasurementEndTime(dataInfo.getEndTime());
        tMeasureData.setSamplingTime(dataInfo.getSampleTime());

        if (data != null) {
            tMeasureData.setSerialNumber(data.getSerialNumber() + 1);
        } else {
            tMeasureData.setSerialNumber(0);
        }
        tMeasureData.setIsDelete(createType);
        tMeasureDataService.save(tMeasureData);

        LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(TMeasureAnalysisData::getDataVersion)
                .last("limit 1");
        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (analysisData != null && analysisData.getMeasureDataId() == null) {

            analysisData.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            analysisData.setMeasureDataId(tMeasureData.getId());
            analysisData.setIsDelete(createType);
            tMeasureAnalysisDataService.updateById(analysisData);

        } else {
            analysisData = new TMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setMeasureDataId(tMeasureData.getId());
            analysisData.setDataVersion(0);
            analysisData.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            analysisData.setIsDelete(createType);
            tMeasureAnalysisDataService.save(analysisData);
        }

        RationSaveCountDataInfo countDataInfo = new RationSaveCountDataInfo();
        countDataInfo.setTaskId(taskId);
        countDataInfo.setSampleId(sampleId);
        countDataInfo.setMeasureDataId(tMeasureData.getId());
        countDataInfo.setAnalyzeDataId(analysisData.getId());
        countDataInfo.setCountData(dataInfo.getCountData());

        return R.ok(countDataInfo);
    }

    @Override
    public R<Boolean> saveAnalyzeData(RationSaveCountDataInfo countData) {
        String taskId = countData.getTaskId();
        String sampleId = countData.getSampleId();
        String measureDataId = countData.getMeasureDataId();
        TMeasureAnalysisData analysisData;
        if (StringUtils.hasText(countData.getAnalyzeDataId())) {
            analysisData = tMeasureAnalysisDataService.getById(countData.getAnalyzeDataId());
        } else {
            LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
            analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                    .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                    .eq(TMeasureAnalysisData::getMeasureDataId, measureDataId)
                    .orderByDesc(TMeasureAnalysisData::getDataVersion)
                    .last("limit 1");
            analysisData = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        }

        if (analysisData == null) {
            analysisData = new TMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setMeasureDataId(measureDataId);
            analysisData.setDataVersion(0);
            analysisData.setIsDelete(createType);
            analysisData.setAnalysisResult(JSON.toJSONString(countData.getCountData()));
            tMeasureAnalysisDataService.save(analysisData);
        } else {
            if (!analysisData.getMeasureDataId().equals(measureDataId)) {
                analysisData.setMeasureDataId(measureDataId);
            }
            analysisData.setAnalysisResult(JSON.toJSONString(countData.getCountData()));
            tMeasureAnalysisDataService.updateById(analysisData);
        }

        return R.ok(true);

    }

    @Override
    public void saveEmptySampleData(List<RationSaveCountDataInfo> emptySampleList) {
        for (RationSaveCountDataInfo emptyData : emptySampleList) {
            LambdaQueryWrapper<TMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureAnalysisData::getTaskId, emptyData.getTaskId())
                    .eq(TMeasureAnalysisData::getSampleKey, emptyData.getSampleId())
                    .eq(TMeasureAnalysisData::getMeasureDataId, emptyData.getMeasureDataId())
                    .orderByDesc(TMeasureAnalysisData::getDataVersion)
                    .last("limit 1");
            TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(queryWrapper);
            if (analysisData != null) {
                analysisData.setAnalysisResult(JSON.toJSONString(emptyData.getCountData()));
                tMeasureAnalysisDataService.updateById(analysisData);
            } else {
                analysisData = new TMeasureAnalysisData();
                analysisData.setTaskId(emptyData.getTaskId());
                analysisData.setSampleKey(emptyData.getSampleId());
                analysisData.setMeasureDataId(emptyData.getMeasureDataId());
                analysisData.setDataVersion(0);
                analysisData.setAnalysisResult(JSON.toJSONString(emptyData.getCountData()));
                analysisData.setIsDelete(createType);
                tMeasureAnalysisDataService.save(analysisData);
            }
        }
    }

    @Override
    public R<RationCountDataInfo> countAbsValue(List<RationWavelengthDataInfo> originData, RationParameter parameter, Integer  digit) {

        List<String> waveLength = parameter.getWaveLength();
        List<String> lengthParams = parameter.getWaveLengthParams();
        double ans = 0;

        for (int i = 0; i < waveLength.size(); i++) {
            double value = Double.MIN_VALUE;
            for (RationWavelengthDataInfo dataInfo : originData) {
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
        RationCountDataInfo countDataInfo = new RationCountDataInfo();
        countDataInfo.setColValue(DataCountUtil.formatValue(digit, ans));
        countDataInfo.setIdentity(SampleTableEnum.Abs.getIdentity());
        countDataInfo.setHeadName(SampleTableEnum.Abs.getName());

        return R.ok(countDataInfo);
    }

    @Override
    public R<RationCountDataInfo> countAbsValue(List<RationWavelengthDataInfo> originData, RationParameter parameter) {

        List<String> waveLength = parameter.getWaveLength();
        List<String> lengthParams = parameter.getWaveLengthParams();
        double ans = 0;

        for (int i = 0; i < waveLength.size(); i++) {
            double value = Double.MIN_VALUE;
            for (RationWavelengthDataInfo dataInfo : originData) {
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
        RationCountDataInfo countDataInfo = new RationCountDataInfo();
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
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        boolean repeat = false;
        if (StringUtils.hasText(task.getMethodKey())) {
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
                if (StringUtils.hasText(parameter.getRepeatMode()) && parameter.getRepeatMode().equals("单池重复")) {
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
                            .eq(TMeasureAnalysisData::getAnalysisName, data.getSerialNumber());

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
                        Map<String, Map<String, String>> childrenList = new HashMap<>();
                        List<Map<String, String>> middle = new ArrayList<>();
                        List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                        for (RationWavelengthDataInfo dataInfo : list) {
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

                    LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                    analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                            .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                            .eq(TMeasureAnalysisData::getAnalysisName, data.getSerialNumber());

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
    public Result<RationSampleDataVO> querySampleData(QueryDataDTO queryDataDTO) {

        String taskId = queryDataDTO.getTaskId();
        Boolean allDisplay = queryDataDTO.getAllDisplay();
        Integer digit = queryDataDTO.getDigit();
        List<String> option = queryDataDTO.getOption();

        if (!StringUtils.hasText(taskId)) {
            return Result.fail("未选择任务");
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
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
        TMeasureTaskAttachment attachment;
        boolean recount = false;

        R<TMeasureTaskAttachment> r = taskAttachmentService.queryByTaskId(taskId);
        if (!r.getSuccess()) {
            log.info("未查询到任务附件信息");
            attachment = new TMeasureTaskAttachment();
            attachment.setTaskId(taskId);
            attachment.setDataParam(JSON.toJSONString(queryDataDTO));
            taskAttachmentService.save(attachment);
            recount = true;
        } else {
            attachment = r.getData();

            if (!StringUtils.hasText(attachment.getDataParam())) {
                attachment.setDataParam(JSON.toJSONString(queryDataDTO));
                taskAttachmentService.saveOrUpdate(attachment);
                recount = true;
            } else {
                QueryDataDTO dataDTO = JSON.parseObject(attachment.getDataParam(), QueryDataDTO.class);
                if (dataDTO.getDigit() == null || !dataDTO.getDigit().equals(digit)) {
                    recount = true;

                    attachment.setDataParam(JSON.toJSONString(queryDataDTO));
                    taskAttachmentService.saveOrUpdate(attachment);
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
        RationSampleDataVO dataVO = new RationSampleDataVO();

        List<Map<String, Object>> standSampleData = new ArrayList<>();
        List<Map<String, Object>> unknowSampleData = new ArrayList<>();
        boolean repeat = false;
        if (StringUtils.hasText(task.getMethodKey())) {
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
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
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        for (TaskSpecimen specimen : taskSpecimen) {
            if (!allDisplay && specimen.getDeleteType().equals("1")) {
                continue;
            }
            if (specimen.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                Map<String, Object> singleSampleData = getSingleSampleData(task, sample, repeat, option, digit);

                standSampleData.add(singleSampleData);
            } else if (specimen.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {

                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

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

    private Map<String, Map<String, String>> extracted(List<String> option, RationWavelengthDataInfo wlData, TMeasureSample sample) {

        Map<String, Map<String, String>> ans = new HashMap<>();

        RepeatMeasureData absData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode());
        RepeatMeasureData tPercentData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode());

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

    public Map<String, Object> getSingleSampleData(TMeasureTask task, TMeasureSample sample, Boolean repeat, List<String> option, Integer digit) {

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
        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getSampleKey, sampleId)
                .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
        TMeasureData data = tMeasureDataService.getOne(queryWrapper);
        if (!repeat) {
            if (data != null) {

                if (StringUtils.hasText(data.getOriginalCalculationData())) {
                    List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                    for (RationWavelengthDataInfo dataInfo : list) {
                        map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), DataCountUtil.formatValueWithBigDecimal(digit, dataInfo.getAbsValue()));
                        map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), DataCountUtil.formatValueWithBigDecimal(digit, dataInfo.getTPercentValue()));
                    }
                }

                map.put(SampleTableEnum.samplingTime.getIdentity(), data.getSamplingTime());
                map.put(SampleTableEnum.remark.getIdentity(), data.getRemark());

                LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                        .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                        .eq(TMeasureAnalysisData::getMeasureDataId, data.getId())
                        .orderByDesc(TMeasureAnalysisData::getDataVersion).last("limit 1");

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
            if (data != null) {
                map.put(SampleTableEnum.samplingTime.getIdentity(), data.getSamplingTime());
                map.put(SampleTableEnum.remark.getIdentity(), data.getRemark());
                if (StringUtils.hasText(data.getOriginalCalculationData())) {
                    Map<String, Map<String, String>> childrenList = new HashMap<>();
                    List<Map<String, String>> middle = new ArrayList<>();
                    List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                    for (RationWavelengthDataInfo dataInfo : list) {
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

                LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                        .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                        .eq(TMeasureAnalysisData::getMeasureDataId, data.getId())
                        .orderByDesc(TMeasureAnalysisData::getDataVersion).last("limit 1");

                TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                if (one != null && one.getAnalysisResult() != null) {
                    List<RationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), RationCountDataInfo.class);
                    for (RationCountDataInfo countData : countDataList) {
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
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        boolean repeat = false;
        if (StringUtils.hasText(task.getMethodKey())) {
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
                if (StringUtils.hasText(parameter.getRepeatMode()) && parameter.getRepeatMode().equals("单池重复")) {
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
                        Map<String, Map<String, String>> childrenList = new HashMap<>();
                        List<Map<String, String>> middle = new ArrayList<>();
                        List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                        for (RationWavelengthDataInfo dataInfo : list) {
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
    public R<FittingReceiveInfo> fittingCurve(List<RationCountDataInfo> countData, Boolean empty, String methodId, List<String> inputParam) {
        
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
        RationFitCurveVO vo = new RationFitCurveVO();
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
    public R<List<RationCountDataInfo>> formula(List<RationWavelengthDataInfo> vo, TMeasureSample sample, TMeasureTask task, RationParameter parameter, Integer digit) {
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

        for (RationWavelengthDataInfo dataInfo : vo) {
            if (dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                allData.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue()==null? null :dataInfo.getAbsValue());
                allData.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue()==null? null :dataInfo.getTPercentValue());
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

        List<RationCountDataInfo> countList = new ArrayList<>();
        for (PreCountResultParam param : parameter.getResultParams()) {
            String[] split = param.getFormulaText().split("=");
            String text = split[split.length - 1];
            String result = restUtil.sum(text, identity.toString(), values.toString());
            RationCountDataInfo countDataInfo = new RationCountDataInfo();
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

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("没有样品");
            return R.fail("没有样品");
        }
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
        boolean repeat = parameter.getRepeatMode().equals("单池重复");

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        boolean empty = false;
        boolean lastIsEmpty = false;
        Double minuend = 0.0;
        //存放空白样数据（计算后的值）
        List<RationSaveCountDataInfo> emptyDataList = new ArrayList<>();
        //存放空白样计算出的Abs的值
        List<RationCountDataInfo> emptyList = new ArrayList<>();

        //参与曲线拟合的数据
        List<RationCountDataInfo> fitCurveData = new ArrayList<>();
        //参与曲线拟合的参数
        List<String> fitCurveParams = new ArrayList<>();

        empty = taskSpecimen.stream()
                .anyMatch(specimen ->
                        specimen.getSpecimenKey().equals(dealSampleKind)
                                && specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())
                );

        for (TaskSpecimen specimen : taskSpecimen) {

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

            TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getTaskId, taskId)
                    .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                    .orderByDesc(TMeasureData::getSerialNumber)
                    .last("limit 1");
            TMeasureData data = tMeasureDataService.getOne(queryWrapper);
            if (data == null) {
                log.info("没有测量数据");
                continue;
            }
            if (!StringUtils.hasText(data.getOriginalCalculationData())) {
                log.info("没有测量数据");
                continue;
            }
            List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
            if (list == null || list.isEmpty()) {
                log.info("没有测量数据");
                continue;
            }
            RationCountDataInfo ansData;
            RationSaveCountDataInfo dealData = new RationSaveCountDataInfo();
            dealData.setSampleId(specimen.getSpecimenKey());
            dealData.setTaskId(taskId);
            dealData.setMeasureDataId(data.getId());

            if (repeat) {
                List<RationWavelengthDataInfo> wavelengthDataInfos = countRepeatData(list, digit);
                R<RationCountDataInfo> absValue = countAbsValue(wavelengthDataInfos, parameter, digit);
                if (!absValue.getSuccess()) {
                    log.info("计算abs失败");
                    RationCountDataInfo value = getAbsValue(taskId, sample.getId(), data.getId(), empty);
                    if (value == null) {
                        return R.fail("数据处理出错且没有找到旧数据，处理失败");
                    }
                    fitCurveData.add(value);
                    fitCurveParams.add(sample.getNormalConcentration());

                    continue;
                }
                ansData = absValue.getData();

            } else {

                R<RationCountDataInfo> absValue = countAbsValue(list, parameter, digit);
                if (!absValue.getSuccess()) {
                    log.info("重新计算abs失败");
                    RationCountDataInfo value = getAbsValue(taskId, sample.getId(), data.getId(), empty);
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
                RationCountDataInfo yAbs = new RationCountDataInfo();
                yAbs.setHeadName(SampleTableEnum.sumAbs.getName());
                yAbs.setIdentity(SampleTableEnum.sumAbs.getIdentity());
                yAbs.setColValue(0.0);

                List<RationCountDataInfo> currentList = new ArrayList<>();

                if (lastIsEmpty) {
                    emptyList.add(ansData);

                    double sub = 0.0;
                    //计算平均值
                    for (RationCountDataInfo dataInfo : emptyList) {
                        sub += dataInfo.getColValue();
                    }
                    minuend = DataCountUtil.formatValue(digit, sub / emptyList.size());
                    //添加到空白列表
                    currentList.add(ansData);
                    yAbs.setColValue(ansData.getColValue() - minuend);
                    currentList.add(yAbs);
                    dealData.setCountData(currentList);
                    emptyDataList.add(dealData);

                    for (RationSaveCountDataInfo emptyData : emptyDataList) {
                        List<RationCountDataInfo> emList = emptyData.getCountData();
                        List<RationCountDataInfo> newList = new ArrayList<>();

                        for (RationCountDataInfo dataInfo : emList) {
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
                List<RationCountDataInfo> currentList = new ArrayList<>();

                //测量过的数据，非空白样，添加标准浓度为了拟合曲线
                //TODO 未来需要添加质量和浓度的区分
                fitCurveParams.add(sample.getNormalConcentration());

                if (empty) {
                    RationCountDataInfo sumAbsData = new RationCountDataInfo();
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
        tMeasureTaskService.updateById(task);
        return R.ok();
    }

    @Override
    public R<Boolean> changeUnknowSampleDigit(String taskId, Integer digit) {
        String dealSampleKind = SampleTypeEnum.kind_unknown.getIdentity();
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("没有样品");
            return R.fail("没有样品");
        }
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
        boolean repeat = parameter.getRepeatMode().equals("单池重复");

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        boolean empty = false;
        boolean lastIsEmpty = false;
        Double minuend = 0.0;
        //存放空白样数据（计算后的值）
        List<RationSaveCountDataInfo> emptyDataList = new ArrayList<>();
        //存放空白样计算出的Abs的值
        List<RationCountDataInfo> emptyList = new ArrayList<>();

        empty = taskSpecimen.stream()
                .anyMatch(specimen ->
                        specimen.getSpecimenKey().equals(dealSampleKind)
                                && specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())
                );

        for (TaskSpecimen specimen : taskSpecimen) {

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

            TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getTaskId, taskId)
                    .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                    .orderByDesc(TMeasureData::getSerialNumber)
                    .last("limit 1");
            TMeasureData data = tMeasureDataService.getOne(queryWrapper);
            if (data == null) {
                log.info("没有测量数据");
                continue;
            }
            if (!StringUtils.hasText(data.getOriginalCalculationData())) {
                log.info("没有测量数据");
                continue;
            }
            List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
            if (list == null || list.isEmpty()) {
                log.info("没有测量数据");
                continue;
            }
            RationCountDataInfo ansData;
            RationSaveCountDataInfo dealData = new RationSaveCountDataInfo();
            dealData.setSampleId(specimen.getSpecimenKey());
            dealData.setTaskId(taskId);
            dealData.setMeasureDataId(data.getId());

            if (repeat) {
                List<RationWavelengthDataInfo> wavelengthDataInfos = countRepeatData(list, digit);
                R<RationCountDataInfo> absValue = countAbsValue(wavelengthDataInfos, parameter, digit);
                if (!absValue.getSuccess()) {
                    log.info("当前样品处理出错，跳过");
                    continue;
                }
                ansData = absValue.getData();

            } else {
                R<RationCountDataInfo> absValue = countAbsValue(list, parameter, digit);
                if (!absValue.getSuccess()) {
                    log.info("计算abs出错，跳过");
                    continue;
                }
                ansData = absValue.getData();
            }

            if (specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {

                empty = true;
                RationCountDataInfo yAbs = new RationCountDataInfo();
                yAbs.setHeadName(SampleTableEnum.sumAbs.getName());
                yAbs.setIdentity(SampleTableEnum.sumAbs.getIdentity());
                yAbs.setColValue(0.0);

                List<RationCountDataInfo> currentList = new ArrayList<>();

                if (lastIsEmpty) {
                    emptyList.add(ansData);

                    double sub = 0.0;
                    //计算平均值
                    for (RationCountDataInfo dataInfo : emptyList) {
                        sub += dataInfo.getColValue();
                    }
                    minuend = DataCountUtil.formatValue(digit, sub / emptyList.size());
                    //添加到空白列表
                    currentList.add(ansData);
                    yAbs.setColValue(ansData.getColValue() - minuend);
                    currentList.add(yAbs);
                    dealData.setCountData(currentList);
                    emptyDataList.add(dealData);

                    for (RationSaveCountDataInfo emptyData : emptyDataList) {
                        List<RationCountDataInfo> emList = emptyData.getCountData();
                        List<RationCountDataInfo> newList = new ArrayList<>();

                        for (RationCountDataInfo dataInfo : emList) {
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
                List<RationCountDataInfo> currentList = new ArrayList<>();
                /*
                未知样品
                计算对象：countData
                首先，有空白，就计算sumAbs值，并且赋值到countData中
                然后，根据曲线计算，公式计算
                最后保存，计算完，abs值就保存一次，但是要注意替换。
                 */
                double toCountData = 0.0;

                if (empty) {
                    RationCountDataInfo sumAbsData = new RationCountDataInfo();
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
                RationCountDataInfo valueData = new RationCountDataInfo();
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

    public RationCountDataInfo getAbsValue(String taskId, String sampleId, String measureDataId, Boolean empty) {
        LambdaQueryWrapper<TMeasureAnalysisData> query = new LambdaQueryWrapper<>();
        query.eq(TMeasureAnalysisData::getTaskId, taskId)
                .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                .eq(TMeasureAnalysisData::getMeasureDataId, measureDataId).orderByDesc(TMeasureAnalysisData::getDataVersion);
        List<TMeasureAnalysisData> list = tMeasureAnalysisDataService.list(query);
        if (list != null && !list.isEmpty()) {
            if (empty) {
                for (TMeasureAnalysisData analysisData : list) {
                    if (analysisData.getAnalysisResult() != null) {
                        List<RationCountDataInfo> countDataInfoList = JSON.parseArray(analysisData.getAnalysisResult(), RationCountDataInfo.class);
                        for (RationCountDataInfo dataInfo : countDataInfoList) {
                            if (dataInfo.getIdentity().equals(SampleTableEnum.sumAbs.getIdentity())) {
                                return dataInfo;
                            }
                        }
                    }
                }
            } else {
                for (TMeasureAnalysisData analysisData : list) {
                    if (analysisData.getAnalysisResult() != null) {
                        List<RationCountDataInfo> countDataInfoList = JSON.parseArray(analysisData.getAnalysisResult(), RationCountDataInfo.class);
                        for (RationCountDataInfo dataInfo : countDataInfoList) {
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
    public List<RationWavelengthDataInfo> countRepeatData(List<RationWavelengthDataInfo> originData, Integer digit) {

        List<RationWavelengthDataInfo> ans = new ArrayList<>();

        Map<String, List<Double>> absValue = new HashMap<>();
        Map<String, List<Double>> tPercentValue = new HashMap<>();
        Map<String, RationWavelengthDataInfo> dataMap = new HashMap<>();
        Set<String> wavelengthSet = new HashSet<>();

        for (RationWavelengthDataInfo data : originData) {
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
            RationWavelengthDataInfo dataInfo = dataMap.get(wavelength);
            RepeatMeasureData repeatData;
            Map<String, RepeatMeasureData> otherData = new HashMap<>();

            List<Double> allAbs = absValue.get(wavelength);
            double mean = DataCountUtil.calculateMean(allAbs, digit);
            Double absSD = DataCountUtil.calculateSD(allAbs, digit);
            Double absRsd = DataCountUtil.calculateRSD(allAbs, digit);
            repeatData = new RepeatMeasureData(absSD == null ? "未检出" :String.valueOf(absSD), absRsd == null ? "未检出" :String.valueOf(absRsd));
            otherData.put(wavelength + "-" + WaveLengthEnum.abs.getCode(), repeatData);

            List<Double> allPercent = tPercentValue.get(wavelength);
            double meanT = DataCountUtil.calculateMean(allPercent, digit);
            Double percentSD = DataCountUtil.calculateSD(allPercent, digit);
            Double percentRsd = DataCountUtil.calculateRSD(allPercent, digit);
            repeatData = new RepeatMeasureData(percentSD == null ? "未检出" :String.valueOf(percentSD), percentRsd == null ? "未检出" :String.valueOf(percentRsd));
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

    @Override
    public Result zero2(String deviceSerialNumber, String methodId, String taskId, String updateBy) {
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
        boolean update = tMeasureTaskService.updateById(task);
        return update ? Result.ok() : Result.fail();
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
        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
        List<InputParam> inputParams = parameter.getSampleInputParams();

        return R.ok(inputParams);
    }

    @Override
    public R<List<InputParamMapTableHead>> queryInputParamMapTableHead(String taskId, String sampleKind) {

        if (!StringUtils.hasText(taskId)) {
            return R.fail("未选择任务");
        }
        if (!StringUtils.hasText(sampleKind)) {
            return R.fail("未选择样品种类");
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return R.fail("任务不存在");
        }
        List<InputParamMapTableHead> ans = new ArrayList<>();
        if (!StringUtils.hasText(task.getTableInfo())) {
            return R.ok(ans);
        }
        RationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), RationTaskHeadSnapShot.class);

        if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity())) {
            TaskHeadSnapshot standHead = shot.getStandHead();
            ans = standHead.getMapRelation();
        } else if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
            TaskHeadSnapshot unknownHead = shot.getUnknownHead();
            ans = unknownHead.getMapRelation();
        }
        return R.ok(ans);

    }

    @Override
    public R<Boolean> saveInputParamMapTableHead(TaskHeadSnapshotDTO snapshotDTO) {

        if (!StringUtils.hasText(snapshotDTO.getTaskId())) {
            return R.fail("未选择任务");
        }
        if (!StringUtils.hasText(snapshotDTO.getSampleKind())) {
            return R.fail("未选择样品种类");
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

        RationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), RationTaskHeadSnapShot.class);
        if (snapshotDTO.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
            shot.setStandHead(taskHeadSnapshot);
        } else if (snapshotDTO.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {
            shot.setUnknownHead(taskHeadSnapshot);
        } else {
            log.info("样品种类有误");
            return R.fail("样品种类有误");
        }

        task.setTableInfo(JSON.toJSONString(shot));

        return tMeasureTaskService.updateById(task) ? R.ok(true) : R.fail("保存失败");
    }

}
