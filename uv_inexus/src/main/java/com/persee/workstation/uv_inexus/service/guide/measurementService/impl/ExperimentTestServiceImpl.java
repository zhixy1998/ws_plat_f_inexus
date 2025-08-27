package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureDataMapper;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.PeakValleyMarking;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.guide.parseReport.CalculationFormula;
import com.persee.workstation.uv_inexus.po.guide.parseReport.StandardPretreatment;
import com.persee.workstation.uv_inexus.po.guide.standardPo.TReportStandardDetection;
import com.persee.workstation.uv_inexus.po.guide.standardPo.TReportStandardPretreatment;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.parseReport.CalculationFormulaService;
import com.persee.workstation.uv_inexus.service.guide.parseReport.StandardPretreatmentService;
import com.persee.workstation.uv_inexus.service.guide.standardService.TReportStandardDetectionService;
import com.persee.workstation.uv_inexus.service.guide.standardService.TReportStandardPretreatmentService;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.vo.measurementVo.*;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.PhotometricReportDataVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.TaskBaseVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.timeSweep.TimeSweepReportDataVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
("experimentTestService")
public class ExperimentTestServiceImpl implements ExperimentTestService {
    @Autowired
    private TMeasureProjectService tMeasureProjectService;
    @Autowired
    private SystemUserManageDAO userConfigDAO;
    @Autowired
    private TMeasureTaskService tMeasureTaskService;
    @Autowired
    private TMeasureDataService tMeasureDataService;
    @Autowired
    private TMeasureMethodService tMeasureMethodService;
    @Autowired
    private TMeasureSampleService tMeasureSampleService;
    @Autowired
    private SpectrumService spectrumService;
    @Autowired
    private PhotometricService photometricService;
    @Autowired
    private RationService rationService;
    @Autowired
    private TMeasureDataMapper tMeasureDataMapper;
    @Autowired
    private TimeSweepService timeSweepService;
    @Autowired
    private TReportStandardDetectionService tReportStandardDetectionService;
    @Autowired
    private TReportStandardPretreatmentService tReportStandardPretreatmentService;
    @Autowired
    private DevMessageUtil devMessageUtil;
    @Autowired
    private DeviceInfoService deviceInfoService;
    @Autowired
    private TMeasureAnalysisDataService tMeasureAnalysisDataService;
    @Autowired
    TMeasureTaskMapper tMeasureTaskMapper;
    // 计算公式的服务
    @Autowired
    private CalculationFormulaService calculationFormulaService;
    // 标准预处理服务
    @Autowired
    private StandardPretreatmentService standardPretreatmentService;

    @Autowired
    private TMeasureTaskAttachmentService tMeasureTaskAttachmentService;


    public DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Result newProject(ProjectParamDto paramDto) {
        // 根据用户名查询项目数据
        List<TMeasureProject> projects = tMeasureProjectService.list(Wrappers.lambdaQuery(TMeasureProject.class).eq(TMeasureProject::getProjectName,paramDto.getProjectName()));
        if (projects.size() > 0) {
            return Result.fail("该项目已存在");
        }
        // 创建项目表
        TMeasureProject tMeasureProject = new TMeasureProject();
        tMeasureProject.setProjectName(paramDto.getProjectName());
        tMeasureProject.setRemark(paramDto.getRemark());
        tMeasureProject.setCreatorKey(paramDto.getUpdateBy());
        tMeasureProject.setCreateTime(LocalDateTime.now());
        tMeasureProject.setLastUpdatedBy(paramDto.getUpdateBy());
        tMeasureProject.setLastUpdatedTime(LocalDateTime.now());
        boolean save = tMeasureProjectService.save(tMeasureProject);
        return Result.ok(save);
    }

    @Override
    public Result recentlyAccomplishTask(String updateBy,String measureType) {
        // 根据用户名查询用户id
        SystemUserManagePO user = userConfigDAO.selectOne(Wrappers.lambdaQuery(SystemUserManagePO.class).eq(SystemUserManagePO::getUserName, updateBy));
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (measureType == null){
            measureType = "光谱扫描";
        }
        // 根据用户id查询字段is_delete为1的数据
        List<TMeasureTask> taskList = tMeasureTaskService.list(new QueryWrapper<TMeasureTask>()
                .eq("user_id", user.getId())
                .eq("is_delete", "1")
                .eq("measure_type", measureType)
                .orderByDesc("create_time").last("limit 3"));
        List<RecentlyCompletedTasksVo> vos = new ArrayList<>();
        if (taskList.size() == 0){
            return Result.ok(vos);
        }
        for (TMeasureTask task : taskList) {
            RecentlyCompletedTasksVo vo = new RecentlyCompletedTasksVo();
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
            TMeasureData measureData = tMeasureDataService.getOne(new QueryWrapper<TMeasureData>().eq("task_id", task.getId()).last("limit 1"));
            List<TaskSpecimen> lists = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            vo.setTaskName(task.getTaskName());
            vo.setTaskId(task.getId());
            vo.setMethodId(task.getMethodKey());
            vo.setMethodName(method.getMethodName());
            vo.setProjectName(project.getProjectName());
            vo.setMeasurementSample(String.valueOf(lists.size()));
            if (measureData != null) {
                vo.setMeasurementStartTime(measureData.getMeasurementStartTime().format(formatter));
            vo.setMeasurementEndTime(measureData.getMeasurementEndTime().format(formatter));
            }

            vos.add(vo);
        }
        return Result.ok(vos);
    }


    /**
     * 查询项目统计接口
     * @param updateBy
     * @return
     */
    @Override
    public Result queryProjectStatistics(String updateBy) {
        /**
         * 实验测试(项目统计)
         * 根据用户名查询用户表
         * 根据用户名去项目表中查询字段创建人和最近更新人
         * 根据用户id去查询方法表中有多少当前用户创建的方法
         * 根据用户id查询测量任务,并且是里面有测量数据的(有测量数据的代表完成测量任务,没测量数据的代表没有完成)
         * {
         * 	项目个数:
         * 	方法个数:
         * 	完成测量任务:
         * 	项目{(项目中的方法个数和任务个数都是针对本项目的)
         * 		项目1[创建人,创建时间,最近更新人,最近更新时间,方法个数,任务个数],
         * 		项目2[创建人,创建时间,最近更新人,最近更新时间,方法个数,任务个数],
         * 		........
         * 	    }
         * }
         */
        SystemUserManagePO user = userConfigDAO.selectOne(Wrappers.lambdaQuery(SystemUserManagePO.class).eq(SystemUserManagePO::getUserName, updateBy));
        // 项目数量
        long countProject = tMeasureProjectService.count(
                Wrappers.lambdaQuery(TMeasureProject.class)
                        .and(wrapper -> wrapper.eq(TMeasureProject::getCreatorKey, updateBy)
                                .or()
                                .eq(TMeasureProject::getLastUpdatedBy, updateBy)));
        // 方法数量
        Long countMethod = tMeasureMethodService.count(
                Wrappers.lambdaQuery(TMeasureMethod.class)
                        .eq(TMeasureMethod::getMethodCreator, updateBy)
        );
        // 测量任务数量
        Long countTask = tMeasureTaskService.count(
                Wrappers.lambdaQuery(TMeasureTask.class)
                        .eq(TMeasureTask::getUserId, user.getId())
        );
        List<TMeasureProject> projects = tMeasureProjectService.list(
                Wrappers.lambdaQuery(TMeasureProject.class)
                        .eq(TMeasureProject::getCreatorKey, updateBy)
                        .or()
                        .eq(TMeasureProject::getLastUpdatedBy, updateBy)
        );
        List<ProjectStatisticsVo> projectStatisticsVos = new ArrayList<>();
        for (TMeasureProject project : projects) {
            // 根据项目id去查询方法表中的方法个数
            long count1 = tMeasureMethodService.count(Wrappers.lambdaQuery(TMeasureMethod.class)
                    .eq(TMeasureMethod::getProjectItemKey, project.getId()));
            // 根据项目id去查询任务表中的任务个数
            long count2 = tMeasureTaskService.count(Wrappers.lambdaQuery(TMeasureTask.class)
                    .eq(TMeasureTask::getProjectItemKey, project.getId()));
            ProjectStatisticsVo vo = new ProjectStatisticsVo();
            vo.setProjectId(project.getId());
            vo.setProjectName(project.getProjectName());
            vo.setCreator(project.getCreatorKey());
            vo.setCreateTime(project.getCreateTime().format(formatter));
            vo.setUpdateBy(project.getLastUpdatedBy());
            vo.setUpdateTime(project.getLastUpdatedTime().format(formatter));
            vo.setMethodCount(String.valueOf(count1));
            vo.setTaskCount(String.valueOf(count2));
            projectStatisticsVos.add(vo);
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("projectCount", countProject);
        map.put("methodCount", countMethod);
        map.put("taskCount", countTask);
        map.put("data", projectStatisticsVos);
        return Result.ok(map);
    }

    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto) {
        // 根据方法名称去方法表中进行查询,查看是否有重复
        List<TMeasureMethod> list = tMeasureMethodService.list(Wrappers.lambdaQuery(TMeasureMethod.class)
                .eq(TMeasureMethod::getMethodName, methodDto.getMethodName()));
        if (list.size() > 0) {
            return Result.fail("方法名称重复");
        }
        Map<String, Object> params = methodDto.getParams();
        MeasureTypeEnum type = MeasureTypeEnum.fromDescription(methodDto.getMethodType());
        // TODO 实验测试的创建方法需要完善
        switch (type){
            case PHOTOMETRIC_MEASUREMENT:
                PhotometricParameter photometricParameter = convertParams(params, PhotometricParameter.class);
                return photometricService.createMethodPort(methodDto, methodDto.getUpdateBy(), photometricParameter);
            case QUANTITATIVE_MEASUREMENT:
                RationParameter rationParameter = convertParams(params, RationParameter.class);
                rationParameter.setEnergyLampName(EnergyLampEnums.getName(rationParameter.getEnergyLamp()));
                rationParameter.setCurveTypeName(RationCurveParamEnums.getName(rationParameter.getCurveType()));
                rationParameter.setCoefficientModeName(RationCurveParamEnums.getName(rationParameter.getCoefficientMode()));
                rationParameter.setEquationTypeName(RationCurveParamEnums.getName(rationParameter.getEquationType()));
                rationParameter.setDifferentialModeName(RationCurveParamEnums.getName(rationParameter.getDifferentialMode()));
                if (rationParameter.getZeroMethod() != null && !rationParameter.getZeroMethod().isEmpty()) {
                    StringBuilder zeroName = new StringBuilder();
                    for (String s : rationParameter.getZeroMethod()) {
                        zeroName.append(RationCurveParamEnums.getName(s)).append(",");
                    }
                    zeroName.deleteCharAt(zeroName.length() - 1);
                    rationParameter.setZeroMethodName(zeroName.toString());
                }
                return rationService.createMethodPort(methodDto, methodDto.getUpdateBy(), rationParameter);
            case SPECTRAL_SCAN:
                SpectrumParameter spectrumParameter = convertParams(params, SpectrumParameter.class);
                return spectrumService.createMethodPort(methodDto, methodDto.getUpdateBy(), spectrumParameter);
            case TIME_SCAN:
                TimeSweepParamter timeSweepParamter = convertParams(params, TimeSweepParamter.class);
                return timeSweepService.createMethodPort(methodDto, methodDto.getUpdateBy(), timeSweepParamter);
            case TWO_COMPONENT_QUANTITATIVE_ANALYSIS:
                break;
        }
        return Result.fail("创建方法失败");
    }

    @Override
    public Result<ProjectTaskMethodVo> createTask(CreateTaskParamDto paramDto) {
        if (!StringUtils.hasText(paramDto.getMethodId())) {
            return Result.fail("请选择方法!");
        }
        if (!StringUtils.hasText(paramDto.getMeasurementType())) {
            return Result.fail("请选择测量类型!");
        }

        // 根据任务名称查询任务,如果为空则继续,如果有值的话则返回错误
        List<TMeasureTask> taskList =  tMeasureTaskService.list(Wrappers.lambdaQuery(TMeasureTask.class).eq(TMeasureTask::getTaskName, paramDto.getTaskName()));
        if (taskList.size()>0){
            return Result.fail("任务名称重复");
        }
        // 根据项目id查询项目
        TMeasureProject project = tMeasureProjectService.getById(paramDto.getProjectId());
        //根据项目中的用户名称查询用户
        SystemUserManagePO userConfigPO = userConfigDAO.selectOne(Wrappers.lambdaQuery(SystemUserManagePO.class).eq(SystemUserManagePO::getUserName, project.getCreatorKey()));
        // 创建任务类
        TMeasureTask tMeasureTask = new TMeasureTask();
        tMeasureTask.setUserId(userConfigPO.getId());
        tMeasureTask.setTaskName(paramDto.getTaskName());
        tMeasureTask.setMeasureType(paramDto.getMeasurementType());
        tMeasureTask.setCreateTime(LocalDateTime.now());
        tMeasureTask.setMethodKey(paramDto.getMethodId());
        tMeasureTask.setProjectItemKey(project.getId());
        tMeasureTask.setDeviceSerialNumber(paramDto.getDeviceSerialNumber());
        List<TaskSpecimen> list = new ArrayList<>();
        MeasureTypeEnum type = MeasureTypeEnum.fromDescription(paramDto.getMeasurementType());
        // TODO 所有测量的创建任务需要完善
        switch (type){
            case PHOTOMETRIC_MEASUREMENT:
                if (paramDto.getSpecimens() != null && !paramDto.getSpecimens().isEmpty()) {
                    for (Map<String, String> cols : paramDto.getSpecimens()) {
                        String jsonString = JSON.toJSONString(cols);
                        TMeasureSample sample = JSON.parseObject(jsonString, TMeasureSample.class);
                        if (paramDto.getHeads() != null) {
                            sample.setHeads(JSON.toJSONString(paramDto.getHeads()));
                            JSONObject newColData = new JSONObject();
                            for (HeadVO head : paramDto.getHeads()) {
                                String s = cols.get(head.getIdentity());
                                newColData.put(head.getIdentity(), s);
                            }
                            sample.setNewColData(JSON.toJSONString(newColData));
                        }
                        sample.setColor("#000000");
                        sample.setDataBulk(0);
                        tMeasureSampleService.save(sample);
                        TaskSpecimen taskSpecimen = new TaskSpecimen();
                        taskSpecimen.setSpecimenKey(sample.getId());
                        taskSpecimen.setDeleteType("0");
                        taskSpecimen.setIsTested("0");
                        list.add(taskSpecimen);
                    }
                }
            case QUANTITATIVE_MEASUREMENT:
                if (paramDto.getRationStandData() != null && !paramDto.getRationStandData().isEmpty()) {
                    for (Map<String, String> map : paramDto.getRationStandData()) {
                        String jsonString = JSON.toJSONString(map);
                        TMeasureSample sample = JSON.parseObject(jsonString, TMeasureSample.class);
                        if (paramDto.getRationStandHeads() != null) {
                            sample.setHeads(JSON.toJSONString(paramDto.getRationStandHeads()));
                            JSONObject newColData = new JSONObject();
                            for (HeadVO head : paramDto.getRationStandHeads()) {
                                String s = map.get(head.getIdentity());
                                newColData.put(head.getIdentity(), s);
                            }
                            sample.setNewColData(JSON.toJSONString(newColData));
                        }
                        sample.setColor("#000000");
                        sample.setDataBulk(0);
                        sample.setSampleKind(SampleTypeEnum.kind_stand.getIdentity());
                        tMeasureSampleService.save(sample);
                        TaskSpecimen taskSpecimen = new TaskSpecimen();
                        taskSpecimen.setSpecimenKey(sample.getId());
                        taskSpecimen.setDeleteType("0");
                        taskSpecimen.setIsTested("0");
                        taskSpecimen.setSampleKind(SampleTypeEnum.kind_stand.getIdentity());
                        taskSpecimen.setSampleType(sample.getSampleType());
                        list.add(taskSpecimen);
                    }
                }
                if (paramDto.getRationUnknowData() != null && !paramDto.getRationUnknowData().isEmpty()) {
                    for (Map<String, String> map : paramDto.getRationUnknowData()) {
                        String jsonString = JSON.toJSONString(map);
                        TMeasureSample sample = JSON.parseObject(jsonString, TMeasureSample.class);
                        if (paramDto.getRationUnknowHeads() != null) {
                            sample.setHeads(JSON.toJSONString(paramDto.getRationUnknowHeads()));
                            JSONObject newColData = new JSONObject();
                            for (HeadVO head : paramDto.getRationUnknowHeads()) {
                                String s = map.get(head.getIdentity());
                                newColData.put(head.getIdentity(), s);
                            }
                            sample.setNewColData(JSON.toJSONString(newColData));
                        }
                        sample.setColor("#000000");
                        sample.setSampleKind(SampleTypeEnum.kind_unknown.getIdentity());
                        sample.setDataBulk(0);
                        tMeasureSampleService.save(sample);
                        TaskSpecimen taskSpecimen = new TaskSpecimen();
                        taskSpecimen.setSpecimenKey(sample.getId());
                        taskSpecimen.setDeleteType("0");
                        taskSpecimen.setIsTested("0");
                        taskSpecimen.setSampleKind(SampleTypeEnum.kind_unknown.getIdentity());
                        taskSpecimen.setSampleType(sample.getSampleType());
                        list.add(taskSpecimen);
                    }
                }
                break;
            case SPECTRAL_SCAN:
                if (paramDto.getSpecimens() != null && !paramDto.getSpecimens().isEmpty()) {
                    for (Map<String, String> map: paramDto.getSpecimens()) {
                        String jsonString = JSON.toJSONString(map);

                        SpectrumSpecimenDto specimen = JSON.parseObject(jsonString, SpectrumSpecimenDto.class);
                        // 创建样品表
                        TMeasureSample sample = new TMeasureSample();
                        sample.setSampleNumber(specimen.getSampleNumber());
                        sample.setSampleName(specimen.getSampleName());
                        sample.setSampleLocation(specimen.getSampleLocation());
                        sample.setColor("#000000");
                        sample.setSampleType(SampleTypeEnum.kind_stand.getIdentity());
                        sample.setDataBulk(0);
                        tMeasureSampleService.save(sample);
                        TaskSpecimen taskSpecimen = new TaskSpecimen();
                        taskSpecimen.setSpecimenKey(sample.getId());
                        taskSpecimen.setDeleteType("0");
                        taskSpecimen.setIsTested("0");
                        list.add(taskSpecimen);
                    }
                }
                break;
            case TIME_SCAN:
                if (paramDto.getSpecimens() != null && !paramDto.getSpecimens().isEmpty()) {
                    for (Map<String, String> map: paramDto.getSpecimens()) {
                        String jsonString = JSON.toJSONString(map);

                        SpectrumSpecimenDto specimen = JSON.parseObject(jsonString, SpectrumSpecimenDto.class);
                        // 创建样品表
                        TMeasureSample sample = new TMeasureSample();
                        sample.setSampleNumber(specimen.getSampleNumber());
                        sample.setSampleName(specimen.getSampleName());
                        sample.setSampleLocation(specimen.getSampleLocation());
                        sample.setColor("#000000");
                        sample.setDataBulk(0);
                        tMeasureSampleService.save(sample);
                        TaskSpecimen taskSpecimen = new TaskSpecimen();
                        taskSpecimen.setSpecimenKey(sample.getId());
                        taskSpecimen.setDeleteType("0");
                        taskSpecimen.setIsTested("0");
                        list.add(taskSpecimen);
                    }
                }
                break;
            case TWO_COMPONENT_QUANTITATIVE_ANALYSIS:
                break;
        }

        tMeasureTask.setSampleKey(JSON.toJSONString(list));
        tMeasureTask.setCreateTime(LocalDateTime.now());
        tMeasureTask.setStartTime(LocalDateTime.now());
        tMeasureTask.setIsDelete("0");
        boolean save = tMeasureTaskService.save(tMeasureTask);
        if (!save) {
            return Result.fail("保存失败");
        }
        TMeasureTaskAttachment tMeasureTaskAttachment = new TMeasureTaskAttachment();
        tMeasureTaskAttachment.setTaskId(tMeasureTask.getId());
        tMeasureTaskAttachmentService.save(tMeasureTaskAttachment);
        ProjectTaskMethodVo vo = new ProjectTaskMethodVo();
        vo.setTaskId(tMeasureTask.getId());
        vo.setTaskName(tMeasureTask.getTaskName());
        vo.setMethodId(tMeasureTask.getMethodKey());
        return Result.ok(vo);
    }

    @Override
    public Result queryTaskPort(QueryTaskDto queryTask) {
        // 判断方法id是all还是id
        QueryWrapper<TMeasureTask> wrapper =  new QueryWrapper<>();
        wrapper.eq("project_item_key", queryTask.getProjectId());
        if (queryTask.getMethodIds().size()!=0){
            wrapper.in("method_key", queryTask.getMethodIds());
        }
        wrapper.eq("project_item_key", queryTask.getProjectId());
        if ((queryTask.getStartTime()!=null&&queryTask.getStartTime()!="")&&(queryTask.getEndTime()!=null&&queryTask.getEndTime()!="")){
            wrapper.between("create_time", queryTask.getStartTime(), queryTask.getEndTime());
        }
        wrapper.eq("measure_type", queryTask.getMeasurementType());
        Page<TMeasureTask> page = tMeasureTaskService.page(new Page<>(queryTask.getPageNum(), queryTask.getPageSize()), wrapper);
        if (page==null||page.getRecords().size() == 0){
            return Result.ok(page);
        }
        page.getRecords().forEach(task -> {
            // 根据任务中的用户id查询用户
            SystemUserManagePO userConfigPO = userConfigDAO.selectOne(Wrappers.lambdaQuery(SystemUserManagePO.class).eq(SystemUserManagePO::getId, task.getUserId()));
            task.setUserId(userConfigPO.getUserName());
            task.setIsDelete(task.getCreateTime().format(formatter));
        });
        return Result.ok(page);
    }

    @Override
    public Result continueMeasuringPort(String projectId, String methodId) {
        TMeasureProject project = tMeasureProjectService.getById(projectId);
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (project == null || method == null) {
            return Result.fail("项目或方法不存在");
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("project", project);
        map.put("method", method);
        return Result.ok(map);
    }

    /**
     * 查询当前用户的所有项目
     * @return
     */
    @Override
    public Result queryAllProject() {
        // 根据用户名查询项目
        List<TMeasureProject> projects = tMeasureProjectService.list();
        return Result.ok(projects);
    }

    @Override
    public Result queryMethodProt(String projectId, String measureType, Integer pageNum, Integer pageSize) {
        MeasureTypeEnum type = MeasureTypeEnum.fromDescription(measureType);
        // TODO 所有测量的查看任务查询需要完善
        switch (type){
            case PHOTOMETRIC_MEASUREMENT:
                if (!measureType.equals(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription())){
                    return Result.fail("请选择光度测量");
                }
                break;
            case QUANTITATIVE_MEASUREMENT:
                if (!measureType.equals(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription())){
                    return Result.fail("请选择定量测量");
                }
                break;
            case SPECTRAL_SCAN:
                if (!measureType.equals(MeasureTypeEnum.SPECTRAL_SCAN.getDescription())){
                    return Result.fail("请选择光谱扫描");
                }
                break;
            case TIME_SCAN:
                if (!measureType.equals(MeasureTypeEnum.TIME_SCAN.getDescription())){
                    return Result.fail("请选择时间扫描");
                }
                break;
            case TWO_COMPONENT_QUANTITATIVE_ANALYSIS:
                if (!measureType.equals(MeasureTypeEnum.TWO_COMPONENT_QUANTITATIVE_ANALYSIS.getDescription())){
                    return Result.fail("请选择双组份定量分析");
                }
                break;
        }
        Page<TMeasureMethod> page = tMeasureMethodService.page(new Page<>(pageNum, pageSize), new QueryWrapper<TMeasureMethod>().eq("project_item_key", projectId).eq("measurement_type", measureType));
        if (page==null||page.getRecords().size() == 0){
            return Result.fail("未查询到数据");
        }
        ArrayList<QueryMethodVO> records = new ArrayList<>();
        for (TMeasureMethod method : page.getRecords()) {
            QueryMethodVO vo = new QueryMethodVO();
            // 将method复制给vo
            BeanUtils.copyProperties(method, vo);
            // 查询当前方法id的所有任务的数量
            Long count = tMeasureTaskService.count(new QueryWrapper<TMeasureTask>().eq("method_key", method.getId()));
            vo.setTaskCount(count);
            if (count > 0){
                // 查询当前方法id的所有任务中最近采集的任务时间,最近采集任务的时间字段是collection_start_time
                TMeasureTask task = tMeasureTaskService.getOne(new QueryWrapper<TMeasureTask>().eq("method_key", method.getId()).orderByDesc("create_time").last("limit 1"));
                vo.setLastTaskTime(task.getStartTime().format(formatter));
            }
            records.add(vo);
        }
        Page<QueryMethodVO> finalPage = new Page<>();
        finalPage.setRecords(records);
        finalPage.setCurrent(page.getCurrent());
        finalPage.setSize(page.getSize());
        finalPage.setTotal(page.getTotal());
        return Result.ok(finalPage);
    }

    @Override
    public Result assignMethodPort(String methodType, String projectId) {
        // 根据方法判定是否是测量方法
        MeasureTypeEnum.fromDescription(methodType);
        if (methodType == null || methodType.equals("")){
            return Result.ok(new ArrayList<>());
        }
        // 根据项目id和方法类型查询方法表数据
        List<TMeasureMethod> list = tMeasureMethodService.list(new QueryWrapper<TMeasureMethod>().eq("project_item_key", projectId).eq("measurement_type", methodType));
        if (list == null || list.size() == 0){
            return Result.ok(new ArrayList<>());
        }
        return Result.ok(list);
    }

    @Override
    public Result queryReferenceStandard(String standardName) {
//         根据标准名称模糊查询标准表数据
        List<TReportStandardDetection> list = tReportStandardDetectionService.list(new QueryWrapper<TReportStandardDetection>().like("standard_name", standardName).eq("release_status", "有效"));
        if (list == null || list.size() == 0){
            return Result.fail("未查询到数据");
        }
        return Result.ok(list);
    }

    @Override
    public Result preprocessTheData(String standardId) {
        // 根据标准id查询前处理信息表数据
        TReportStandardPretreatment info = tReportStandardPretreatmentService.getOne(new QueryWrapper<TReportStandardPretreatment>().eq("detection_id", standardId));
        if (info == null){
            return Result.fail("未查询到前处理信息");
        }
        return Result.ok(info);
    }

    @Override
    public Result reportDataQuery(String taskId,String reportName,String sampleId) {

        if ((taskId == null)) {
            return Result.fail("任务id不能为空");
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        if (reportName.equals("光度测量原始记录表格")){

            PhotometricReportDataVO vo = new PhotometricReportDataVO();

            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());

            List<HeadVO> heads = new ArrayList<>();
            heads.add(new HeadVO("样品编号", "sampleNumber"));
            heads.add(new HeadVO("样品名称", "sampleName"));
            heads.add(new HeadVO("样品类型", "sampleType"));
            heads.add(new HeadVO("稀释比", "dilutionRatio"));
            heads.add(new HeadVO("样品体积", "sampleVolume"));
            heads.add(new HeadVO("样品位置", "sampleLocation"));

            List<HeadVO> vos = photometricService.tableHead(taskId, method.getId(), false).getData();
            heads.addAll(vos);

            heads.add(new HeadVO("备注", "remark"));
            heads.add(new HeadVO("采样时间", "samplingTime"));
            vo.setHeadList(heads);

            PhotoQueryDataDTO queryDataDTO = new PhotoQueryDataDTO();
            queryDataDTO.setOption(new ArrayList<>());
            queryDataDTO.setTaskId(taskId);
            queryDataDTO.setAllDisplay(false);

            List<Map<String, Object>> list = photometricService.queryData(queryDataDTO).getData();

            for (Map<String, Object> stringMap : list) {
                stringMap.put("sampleType", SampleTypeEnum.getName(String.valueOf(stringMap.get("sampleType"))));
            }

            vo.setDataList(list);

            return Result.ok(vo);
        } else if (reportName.equals("光度测量表格")){
            R<PhotometricReportDataVO> reportCount = photometricService.getReportCount(taskId);
            if (reportCount.getSuccess()) {
                PhotometricReportDataVO vo = reportCount.getData();
                return Result.ok(vo);
            }

            return Result.fail(reportCount.getMessage());
        }
        else if(reportName.equals("定量测量表格")){
            return Result.ok("暂未开放");
        }
        else if (reportName.equals("光谱扫描表格")) {

            // 根据任务id测量数据表
            List<TMeasureData> measureData = tMeasureDataService.list(new QueryWrapper<TMeasureData>().eq("task_id", taskId).eq("is_delete", "0"));
            if (measureData == null || measureData.size() == 0) {
                return Result.fail("未测量,请先测量");
            }
            List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("task_id", taskId).eq("is_delete", "0"));

            List<ReportDataVo> list = new ArrayList<>();
            for (TMeasureData data : measureData) {
                ReportDataVo vo = new ReportDataVo();
                // 根据data的样品id查询样品名称
                TMeasureSample sample = tMeasureSampleService.getById(data.getSampleKey());
                vo.setName(sample.getSampleName());
                List<PeakValleyMarking> peak = JSON.parseArray(data.getPeak(), PeakValleyMarking.class);
                vo.setPeak(peak.stream().map(x->x.getX()).collect(Collectors.toCollection(ArrayList::new)));
                List<PeakValleyMarking> valley = JSON.parseArray(data.getPeak(), PeakValleyMarking.class);
                vo.setValley(valley.stream().map(x->x.getX()).collect(Collectors.toCollection(ArrayList::new)));
                List<PeakValleyMarking> markKing = JSON.parseArray(data.getPeak(), PeakValleyMarking.class);
                vo.setMarkPoint(markKing.stream().map(x->x.getX()).collect(Collectors.toCollection(ArrayList::new)));
                vo.setParameterData(JSON.parseArray(data.getOriginalCalculationData(), String.class));
                vo.setDataType("测量数据");
                list.add(vo);
            }
            if (analysisData != null || analysisData.size() != 0) {
                for (TMeasureAnalysisData data : analysisData) {
                    ReportDataVo vo = new ReportDataVo();
                    // 根据data的样品id查询样品名称
                    vo.setName(data.getAnalysisName());
                    vo.setPeak(JSON.parseArray(data.getPeak(), String.class));
                    vo.setValley(JSON.parseArray(data.getGrain(), String.class));
                    vo.setMarkPoint(JSON.parseArray(data.getGaugePoint(), String.class));
                    vo.setParameterData(JSON.parseArray(data.getAnalysisResult(), String.class));
                    vo.setDataType("分析数据");
                    list.add(vo);
                }
            }

            return Result.ok(list);
        }else if (reportName.equals("时间扫描表格")){
            TimeSweepReportDataVo vo = new TimeSweepReportDataVo();

            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());

            TimeSweepParamter param = JSON.parseObject(method.getMeasurementParamInfo(), TimeSweepParamter.class);


            List<HeadVO> heads = new ArrayList<>();
            heads.add(new HeadVO("序号", "serialNumber"));
            heads.add(new HeadVO("峰/谷", "type"));
            heads.add(new HeadVO("波长(nm)", "waveLength"));
            heads.add(new HeadVO(param.getLuminosity().equals("1")?"Abs":param.getLuminosity().equals("2")?"T%":"3".equals(param.getLuminosity())?"Es":"Er", "luminosity"));
            heads.add(new HeadVO("注释", "remark"));
            vo.setHeadList(heads);


            List<PeakValleyMarking> list = timeSweepService.queryReportFormData(taskId,sampleId);
            List<Map<String, String>> dataList = new ArrayList<>();
            int i = 1;
            for (PeakValleyMarking marking : list) {
                Map<String, String> map = new HashMap<>();
                map.put("serialNumber", String.valueOf(i));
                map.put("type", marking.getType());
                map.put("waveLength", marking.getX());
                map.put("luminosity", marking.getY());
                map.put("remark", marking.getRemark()==null||marking.getRemark().equals("")?"":marking.getRemark());
                dataList.add(map);
                i++;
            }
            vo.setDataList(dataList);
            return Result.ok(vo);
        }else{
            return Result.fail("类型错误,请重新选择");
        }
    }

    @Override
    public Result<MeasurementCurrentInfoVO> getProjectInfo(String projectId, String measureType) {

        if (!StringUtils.hasText(projectId) || !StringUtils.hasText(measureType)) {
            System.out.println("缺少参数");
            return Result.fail("缺少参数");
        }

        MeasurementCurrentInfoVO vo = new MeasurementCurrentInfoVO();

        TMeasureProject project = tMeasureProjectService.getById(projectId);
        if (project == null) {
            return Result.fail("项目不存在");
        }
        vo.setProjectId(project.getId());
        vo.setProjectName(project.getProjectName());

        LambdaQueryWrapper<TMeasureTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureTask::getProjectItemKey, projectId)
                .eq(TMeasureTask::getMeasureType, measureType)
                .orderByDesc(TMeasureTask::getCreateTime);
        TMeasureTask task = tMeasureTaskService.list(queryWrapper).get(0);
        if (task != null) {
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getTaskName());
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            vo.setMethodId(method.getId());
            vo.setMethodName(method.getMethodName());
        } else {
            LambdaQueryWrapper<TMeasureMethod> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TMeasureMethod::getProjectItemKey, projectId)
                    .eq(TMeasureMethod::getMeasurementType, measureType)
                    .orderByDesc(TMeasureMethod::getMethodCreateTime);
            TMeasureMethod method = tMeasureMethodService.list(wrapper).get(0);
            if (method != null) {
                vo.setMethodId(method.getId());
                vo.setMethodName(method.getMethodName());
            }

        }
        return Result.ok(vo);
    }

    @Override
    public Result<List<TaskBaseVO>> getAllTask(String projectId, String measureType) {
        if (!StringUtils.hasText(projectId) || !StringUtils.hasText(measureType)) {
            System.out.println("缺少参数");
            return Result.fail("缺少参数");
        }
        List<TaskBaseVO> taskList =  tMeasureTaskMapper.getAllTask(projectId, measureType);

        if (taskList == null || taskList.isEmpty()) {
            return Result.fail("没有找到任务");
        }

        return Result.ok(taskList);
    }

    @Override
    public Result useThisMethodPort(String methodId) {
        // 使用StringUtils 判断参数是否为空
        if (!StringUtils.hasText(methodId)){
            return Result.fail("缺少参数");
        }
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null){
            return Result.fail("没有找到该方法");
        }

        // 根据方法的测量类型和方法id去查询最近时间的任务
        TMeasureTask task = tMeasureTaskService.getOne(new QueryWrapper<TMeasureTask>()
                .eq("measure_type", method.getMeasurementType())
                .eq("method_key", methodId)
                .orderByDesc("create_time")
                .last("limit 1"));
        if (task == null){
            return Result.ok("没有找到该任务");
        }
        Map<String,String> params = new HashMap<>();
        params.put("taskId", task.getId());
        params.put("taskName", task.getTaskName());
        return Result.ok(params);
    }

    @Override
    public Result<MeasurementCurrentInfoVO> useMethodToTest(String methodId, String updateBy, String deviceSerialNumber) {
        if (!StringUtils.hasText(methodId)) {
            return Result.fail("未选择方法");
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }
        if (!StringUtils.hasText(method.getMeasurementType())) {
            return Result.fail("当前方法缺少类型");
        }

        CreateTaskParamDto dto = new CreateTaskParamDto();
        dto.setMethodId(methodId);
        dto.setProjectId(method.getProjectItemKey());
        dto.setMeasurementType(method.getMeasurementType());
        dto.setDeviceSerialNumber(deviceSerialNumber);

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        String collect = new Random().ints(8, 0, chars.length())
                .mapToObj(i -> String.valueOf(chars.charAt(i)))
                .collect(Collectors.joining());

        dto.setTaskName(method.getMeasurementType() + "task_" + collect);
        List<Map<String, String>> taskSpecimen = new ArrayList<>();
        dto.setSpecimens(taskSpecimen);

        Result task = createTask(dto);
        if (!task.getCode().equals(200)) {
           return Result.fail(task.getMessage());
        }

        MeasurementCurrentInfoVO vo = new MeasurementCurrentInfoVO();
        vo.setProjectId(method.getProjectItemKey());
        vo.setMethodId(methodId);
        ProjectTaskMethodVo data =(ProjectTaskMethodVo) task.getData();
        vo.setTaskId(data.getTaskId());
        vo.setTaskName(dto.getTaskName());
        vo.setMethodName(method.getMethodName());

        TMeasureProject project = tMeasureProjectService.getById(method.getProjectItemKey());

        vo.setProjectName(project.getProjectName());

        return Result.ok(vo);
    }

    /**
     * 获取所有方法或按条件查询方法
     * @param methodName
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public Result listOfMethods(String methodName, Integer pageNum, Integer pageSize, String sort) {
        // 1. 获取所有方法或按条件查询方法
        List<MethodBaseVo> methodVo;
        if (methodName == null || methodName.isEmpty()) {
            List<TMeasureMethod> methods = tMeasureMethodService.list();  // 查询所有方法
            if (methods == null || methods.isEmpty()){
                return Result.ok(new ArrayList<>());
            }
            methodVo = methods.stream()
                    .map(method -> {
                        // 将TMeasureMethod拷贝到MethodBaseVo
                        MethodBaseVo vo = new MethodBaseVo();
                        vo.setId(method.getId());
                        vo.setMethodName(method.getMethodName());
                        vo.setCreator(method.getMethodCreator());
                        vo.setMeasurementType(method.getMeasurementType());
                        vo.setCreateTime(method.getMethodCreateTime().format(formatter));
                        TMeasureProject project = tMeasureProjectService.getById(method.getProjectItemKey());
                        if (project != null){
                            vo.setProject(project.getProjectName());
                        }
                        return vo;
                    })
                    .collect(Collectors.toList());
        } else {
            List<TMeasureMethod> methods = tMeasureMethodService.list(Wrappers.lambdaQuery(TMeasureMethod.class).eq(TMeasureMethod::getMethodName, methodName));  // 根据方法名查询
            methodVo = methods.stream()
                    .map(method -> {
                        // 将TMeasureMethod拷贝到MethodBaseVo
                        MethodBaseVo vo = new MethodBaseVo();
                        vo.setId(method.getId());
                        vo.setMethodName(method.getMethodName());
                        vo.setCreator(method.getMethodCreator());
                        vo.setMeasurementType(method.getMeasurementType());
                        vo.setCreateTime(method.getMethodCreateTime().format(formatter));
                        TMeasureProject project = tMeasureProjectService.getById(method.getProjectItemKey());
                        vo.setProject(project.getProjectName());
                        return vo;
                    })
                    .collect(Collectors.toList());
        }
        // 2. 获取每个方法的最近任务使用时间
        for (MethodBaseVo method : methodVo) {
            // 获取方法的ID
            String methodId = method.getId();

            // 查询该方法的最近任务，按创建时间降序排序
            TMeasureTask latestTask = tMeasureTaskService.list(Wrappers.lambdaQuery(TMeasureTask.class)
                            .eq(TMeasureTask::getMethodKey, methodId)
                            .orderByDesc(TMeasureTask::getCreateTime)
                            .last("LIMIT 1"))  // 只获取最近的一条任务
                    .stream()                           // 将查询结果转换为流 (Stream)
                    .findFirst()                         // 获取流中的第一条数据（如果有的话）
                    .orElse(null);                       // 如果没有数据，返回 null

            // 获取最近任务的创建时间，作为使用时间
            LocalDateTime lastUsedTime = latestTask != null ? latestTask.getCreateTime() : null;
            if (lastUsedTime == null) {
                method.setRecentUsageTime("—");
            } else{
                method.setRecentUsageTime(lastUsedTime.format(formatter));  // 将使用时间设置到方法对象中
            }

        }
        // 3. 根据使用时间进行排序（根据sort参数判断排序方式）
        if (sort.equals("0")) {  // 倒序
            methodVo.sort((m1, m2) -> {
                if (m1.getRecentUsageTime() == null) return 1;
                if (m2.getRecentUsageTime() == null) return -1;
                return m2.getRecentUsageTime().compareTo(m1.getRecentUsageTime());
            });
        } else if (sort.equals("1")) {  // 顺序
            methodVo.sort((m1, m2) -> {
                if (m1.getRecentUsageTime() == null) return 1;
                if (m2.getRecentUsageTime() == null) return -1;
                return m1.getRecentUsageTime().compareTo(m2.getRecentUsageTime());
            });
        }

        // 4. 分页处理
        int total = methodVo.size();  // 总条数
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, methodVo.size());

        // 获取当前页的数据
        List<MethodBaseVo> pagedResult = methodVo.subList(fromIndex, toIndex);

        // 构造分页返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("data", pagedResult);  // 数据
        response.put("total", total);       // 总条数
        response.put("pageNum", pageNum);  // 当前页码
        response.put("pageSize", pageSize);  // 每页大小
        response.put("totalPages", (int) Math.ceil((double) total / pageSize)); // 总页数

        return Result.ok(response);

    }

    /**
     * 最近创建的任务
     * @param updateBy
     * @param measureType
     * @return
     */
    @Override
    public Result recentlyCreatedTasks(String updateBy, String measureType) {
        // 根据用户名查询用户id
        SystemUserManagePO user = userConfigDAO.selectOne(Wrappers.lambdaQuery(SystemUserManagePO.class).eq(SystemUserManagePO::getUserName, updateBy));
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (measureType == null){
            measureType = "光谱扫描";
        }
        // 根据用户id查询字段is_delete为1的数据
        List<TMeasureTask> taskList = tMeasureTaskService.list(new QueryWrapper<TMeasureTask>()
                .eq("user_id", user.getId())
                .eq("measure_type", measureType)
                .orderByDesc("create_time").last("limit 3"));
        List<RecentlyCompletedTasksVo> vos = new ArrayList<>();
        if (taskList == null){
            return Result.ok(new ArrayList<>());
        }
        for (TMeasureTask task : taskList) {
            RecentlyCompletedTasksVo vo = new RecentlyCompletedTasksVo();
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
            TMeasureData measureData = tMeasureDataService.getOne(new QueryWrapper<TMeasureData>().eq("task_id", task.getId()).last("limit 1"));
            List<TaskSpecimen> lists = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            if(lists != null){
                vo.setMeasurementSample(String.valueOf(lists.size()));
            }else{
                vo.setMeasurementSample("0");
            }
            vo.setTaskName(task.getTaskName());
            vo.setTaskId(task.getId());
            vo.setMethodId(task.getMethodKey());
            vo.setMethodName(method.getMethodName());
            vo.setProjectName(project.getProjectName());

            if (measureData != null) {
                if (measureData.getMeasurementStartTime() != null&& measureData.getMeasurementEndTime() != null){
                    vo.setMeasurementStartTime(measureData.getMeasurementStartTime().format(formatter));
                    vo.setMeasurementEndTime(measureData.getMeasurementEndTime().format(formatter));
                }else{
                    vo.setMeasurementStartTime("—");
                    vo.setMeasurementEndTime("—");
                }
            }

            vos.add(vo);
        }
        return Result.ok(vos);
    }

    @Override
    public Result obtainReportParameters(String taskId) {

        if ((taskId == null)) {
            return Result.fail("任务id不能为空");
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        if (task.getMeasureType().equals(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription())) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            SpectralReportVo vo = new SpectralReportVo();
            TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
            SystemUserManagePO user = userConfigDAO.selectById(task.getUserId());
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            PhotometricParameter methodparam = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);

            if (StringUtils.hasText(task.getSampleKey())) {
                List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
                if (!taskSpecimen.isEmpty()) {
                    TaskSpecimen specimen = taskSpecimen.get(0);
                    TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                    vo.setSampleName(sample.getSampleName());
                    vo.setSampleNumber(sample.getSampleNumber());
                    vo.setSamplingTime(task.getCreateTime().format(formatter));

                    LambdaQueryWrapper<TMeasureData> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(TMeasureData::getTaskId, taskId)
                            .eq(TMeasureData::getSampleKey, sample.getId())
                            .orderByDesc(TMeasureData::getSerialNumber)
                            .last("LIMIT 1");
                    TMeasureData one = tMeasureDataService.getOne(wrapper);
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
                    wave.append(s).append(",");
                }
            }
            if (wave.length() > 0) {
                wave.deleteCharAt(wave.length() - 1);
            }
            vo.setInstrumentWavelength(wave.toString());

            vo.setReferenceMethod(method.getMethodName());
            vo.setReferenceStandard(methodparam.getReferenceStandardName());

            vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            vo.setLuminosityMode(WaveLengthEnum.getName(methodparam.getLuminosity()));
            vo.setSpectrumBandwidth(methodparam.getSpectralBandwidth());

            vo.setInstrumentName("紫外可见分光光度计");
            vo.setInstrumentModel("T600");

            vo.setReferenceMethod(method.getMethodName());
            vo.setReferenceStandard(methodparam.getReferenceStandardName());
            StringBuilder steps = new StringBuilder();
            methodparam.getPreProcessParameters().forEach(preProcessParameter -> {
                steps.append(preProcessParameter).append("\n");
            });

            StringBuilder result = new StringBuilder();
            methodparam.getResultParams().forEach(resultParam -> {
                result.append(resultParam.getFormulaText()).append("\n");
            });
            vo.setCalculationFormula(result.toString());

            result.setLength(0);
            methodparam.getSampleInputParams().forEach(sampleInputParam -> {
                result.append(sampleInputParam.getResultIdentity()).append("——").append(sampleInputParam.getParamName()).append("\n");
            });
            vo.setFormulaParams(result.toString());

            vo.setSteps(steps.toString());
            vo.setDetectionStartTime(task.getStartTime().format(formatter));
            if (task.getEndTime() != null){
                vo.setDetectionEndTime(task.getEndTime().format(formatter));
            }
            vo.setDataType("测量数据");

            vo.setAmbientTemperature("26℃");
            vo.setRelativeHumidity("60%");
            vo.setSampleState("正常");
            vo.setColorPlateWavelength("1");


            vo.setProject(project.getProjectName());
            vo.setDetector(user.getUserName());
            vo.setEnergyLight(methodparam.getEnergyLamp());
            vo.setMethodName(method.getMethodName());

            return Result.ok(vo);

        } else if (task.getMeasureType().equals(MeasureTypeEnum.SPECTRAL_SCAN.getDescription())) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            SpectralReportVo vo = new SpectralReportVo();
//            TMeasureTask task = tMeasureTaskService.getById(taskId);
            TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
            SystemUserManagePO user = userConfigDAO.selectById(task.getUserId());
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            SpectrumParameter methodparam = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
            TMeasureData data = tMeasureDataService.getOne(new QueryWrapper<TMeasureData>().eq("task_id", taskId).orderByDesc("measurement_end_time").last("LIMIT 1"));
            // 根据方法表的计算公式id和前处理id进行查
            if(StringUtils.hasText(method.getMultipleCalculationFormulaKey())) {
                CalculationFormula calculationFormula = calculationFormulaService.findById(Integer.valueOf(method.getMultipleCalculationFormulaKey()));
                vo.setCalculationFormula(calculationFormula.getResultName());
            }
            if(StringUtils.hasText(method.getPreprocessKey())){
                StandardPretreatment serviceById = standardPretreatmentService.findById(Integer.valueOf(method.getPreprocessKey()));
                vo.setReferenceStandard(serviceById.getPretreatmentDescription());
            }
            TMeasureSample byId = tMeasureSampleService.getById(data.getSampleKey());
            vo.setSampleName(byId.getSampleName());
            vo.setSampleNumber(byId.getSampleNumber());
            vo.setDataType("测量数据");
            vo.setScanSpeed(methodparam.getScanSpeed()=="0"?"慢速":methodparam.getScanSpeed()=="1"?"中速":methodparam.getScanSpeed()=="2"?"快速":"极快");
            vo.setInstrumentWavelength(methodparam.getWaveLengthInterval());
            vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            vo.setLuminosityMode(methodparam.getLuminosity()=="1"?"Abs":methodparam.getLuminosity()=="2"?"T%":methodparam.getLuminosity()=="3"?"Es":"Er");
            vo.setSpectrumBandwidth(methodparam.getSpectralBandwidth());
            vo.setReferenceMethod(method.getMethodName());
            vo.setRemark(data.getRemark());
            vo.setDetectionStartTime(data.getMeasurementStartTime().format(formatter));
            vo.setDetectionEndTime(data.getMeasurementEndTime().format(formatter));
            vo.setProject(project.getProjectName());
            vo.setDetector(user.getUserName());
            vo.setEnergyLight(methodparam.getEnergyLamp()=="0"?"钨灯":methodparam.getEnergyLamp()=="1"?"氘灯":"汞灯");
            vo.setStairCorrection(methodparam.getLadderCorrection() == true ? "启用" : "不启用");
            vo.setSpectrumScanImage(data.getDataImage());
            vo.setOriginalData(data.getOriginalCalculationData());
            vo.setGain(methodparam.getGain().toString());
            vo.setScanRange(methodparam.getStartWaveLength() + "-" + methodparam.getEndWaveLength());
            vo.setScanInterval(methodparam.getWaveLengthInterval());
            vo.setMethodName(method.getMethodName());
            vo.setRemark(data.getRemark());
            vo.setSpectrumScanImage(data.getDataImage());
            return Result.ok(vo);
        }else if (task.getMeasureType().equals(MeasureTypeEnum.TIME_SCAN.getDescription())){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            SpectralReportVo vo = new SpectralReportVo();
            TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
            SystemUserManagePO user = userConfigDAO.selectById(task.getUserId());
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            TimeSweepParamter methodparam = JSON.parseObject(method.getMeasurementParamInfo(), TimeSweepParamter.class);
            TMeasureData data = tMeasureDataService.getOne(new QueryWrapper<TMeasureData>().eq("task_id", taskId).orderByDesc("measurement_end_time").last("LIMIT 1"));
            // 根据方法表的计算公式id和前处理id进行查
            if(StringUtils.hasText(method.getMultipleCalculationFormulaKey())) {
                CalculationFormula calculationFormula = calculationFormulaService.findById(Integer.valueOf(method.getMultipleCalculationFormulaKey()));
                vo.setCalculationFormula(calculationFormula.getResultName());
            }
            if(StringUtils.hasText(method.getPreprocessKey())){
                StandardPretreatment serviceById = standardPretreatmentService.findById(Integer.valueOf(method.getPreprocessKey()));
                vo.setReferenceStandard(serviceById.getPretreatmentDescription());
            }
            TMeasureSample byId = tMeasureSampleService.getById(data.getSampleKey());
            vo.setSampleName(byId.getSampleName());
            vo.setSampleNumber(byId.getSampleNumber());
            vo.setDataType("测量数据");
            vo.setInstrumentWavelength(methodparam.getWaveLength());
            vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            vo.setLuminosityMode(methodparam.getLuminosity().equals("1")?"Abs":methodparam.getLuminosity().equals("2")?"T%":methodparam.getLuminosity().equals("3")?"Es":"Er");
            vo.setSpectrumBandwidth(methodparam.getSpectralBandwidth());
            vo.setReferenceMethod(method.getMethodName());
            vo.setRemark(data.getRemark());
            vo.setDetectionStartTime(data.getMeasurementStartTime().format(formatter));
            vo.setDetectionEndTime(data.getMeasurementEndTime().format(formatter));
            vo.setProject(project.getProjectName());
            vo.setDetector(user.getUserName());
            vo.setEnergyLight(methodparam.getEnergyLamp().equals("0")?"钨灯":methodparam.getEnergyLamp().equals("1")?"氘灯":"汞灯");
            vo.setOriginalData(data.getOriginalCalculationData());
            vo.setGain(methodparam.getGain().toString());
            vo.setScanInterval(methodparam.getIntervalTime());
            vo.setMethodName(method.getMethodName());
            vo.setTimeScanImage(data.getDataImage());
            return Result.ok(vo);
        }else{
            return Result.ok();
        }
    }

    /**
     * 导出方法
     * @param ids
     * @return
     */
    @Override
    public List<TMeasureMethod> exportMethod(List<String> ids) {
        return tMeasureMethodService.listByIds(ids);
    }

    /**
     * 导入方法
     * @param file
     * @return
     */
    @Override
    public Result importMethod(MultipartFile file) {
        String json = null;
        try {
            json = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<TMeasureMethod> methodList = new ArrayList<>();
        try {
            // 尝试解析成数组
            methodList = JSON.parseArray(json, TMeasureMethod.class);
        } catch (Exception e) {
            // 如果不是数组，那就是单个对象
            TMeasureMethod method = JSON.parseObject(json, TMeasureMethod.class);
            methodList.add(method);
        }

        for (TMeasureMethod method : methodList) {
            // 判断是否存在
            TMeasureMethod exist = tMeasureMethodService.getById(method.getId());
            if (exist != null) {
                // 这里根据需求选择覆盖 or 新建
                method.setId(UUID.randomUUID().toString()); // 新建
            }
            tMeasureMethodService.save(method);
        }

        return Result.ok("成功导入 " + methodList.size() + " 个方法");
    }

    /**
     * 清空项目
     * @param projectId
     * @return
     */
    @Override
    public Result clearProject(String projectId) {
        // 校验：检查项目是否存在
        TMeasureProject project = tMeasureProjectService.getById(projectId);
        if (project == null) {
            return Result.fail("项目不存在");
        }

        // 1. 判断任务是否为空
        long taskCount = tMeasureTaskService.count(new QueryWrapper<TMeasureTask>().eq("project_id", projectId));
        if (taskCount > 0) {
            return Result.fail("请先删除所有任务");
        }

        // 2. 将所有该项目的方法的 projectId 字段清空
        List<TMeasureMethod> methods = tMeasureMethodService.list(new QueryWrapper<TMeasureMethod>().eq("project_id", projectId));
        if (!methods.isEmpty()) {
            for (TMeasureMethod method : methods) {
                method.setProjectItemKey(null);
            }
            tMeasureMethodService.updateBatchById(methods);
        }

        // 3. 删除项目
        boolean removed = tMeasureProjectService.removeById(projectId);
        if ( removed){
            return Result.ok("项目已清空");
        }else {
            return Result.fail("项目清空失败");
        }

    }

    /**
     * 清除任务
     * @param taskId
     * @return
     */
    @Override
    public Result clearTask(String taskId) {
        // 校验：检查任务是否存在
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        // 1. 删除任务样品表
        tMeasureSampleService.remove(new QueryWrapper<TMeasureSample>().eq("task_id", taskId));

        // 2. 删除任务测量数据
        tMeasureDataService.remove(new QueryWrapper<TMeasureData>().eq("task_id", taskId));

        // 3. 删除任务分析数据
        tMeasureAnalysisDataService.remove(new QueryWrapper<TMeasureAnalysisData>().eq("task_id", taskId));

        // 4. 删除任务附属信息（比如附件、扩展表等）
        tMeasureTaskAttachmentService.remove(new QueryWrapper<TMeasureTaskAttachment>().eq("task_id", taskId));

        // 5. 删除任务本身
        boolean removed = tMeasureTaskService.removeById(taskId);
        if (removed){
            return Result.ok("任务已删除");
        }else  {
            return Result.fail("任务删除失败");
        }
    }

    /**
     * 清除方法
     * @param methodId
     * @return
     */
    @Override
    public Result clearMethod(String methodId) {
        // 校验：检查方法是否存在
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }

        // 1. 判断是否有任务在使用该方法
        long taskCount = tMeasureTaskService.count(new QueryWrapper<TMeasureTask>().eq("method_key", methodId));
        if (taskCount > 0) {
            return Result.fail("请先删除使用该方法的所有任务");
        }

        // 2. 删除方法
        boolean removed = tMeasureMethodService.removeById(methodId);
        if (removed){
            return Result.ok("方法已删除");
        }else {
            return Result.fail("方法删除失败");
        }
    }


    private <T> T convertParams(Map<String, Object> params, Class<T> clazz) {
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//            // 将 Map 转换为指定的 Java 对象
//            return objectMapper.convertValue(params, clazz);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
        String jsonString = JSON.toJSONString(params);
        T t = JSON.parseObject(jsonString, clazz);
        return t;

    }
}
