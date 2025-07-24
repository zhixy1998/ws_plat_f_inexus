package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.measurementDao.TMeasureDataMapper;
import com.persee.workstation.uv_inexus.dao.measurementDao.TMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTypeEnum;
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
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.vo.measurementVo.*;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.PhotometricReportDataVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.TaskBaseVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        for (TMeasureTask task : taskList) {
            RecentlyCompletedTasksVo vo = new RecentlyCompletedTasksVo();
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
            TMeasureData measureData = tMeasureDataService.getOne(new QueryWrapper<TMeasureData>().eq("task_id", task.getId()).orderByDesc("measurement_end_time").last("limit 1"));
            List<String> lists = JSON.parseArray(task.getSampleKey(), String.class);
            vo.setTaskName(task.getTaskName());
            vo.setMethodName(method.getMethodName());
            vo.setProjectName(project.getProjectName());
            vo.setMeasurementSample(String.valueOf(lists.size()));
            vo.setMeasurementStartTime(measureData.getMeasurementStartTime().format(formatter));
            vo.setMeasurementEndTime(measureData.getMeasurementEndTime().format(formatter));
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
    public Result createTask(CreateTaskParamDto paramDto) {
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
                // 遍历CreateTaskParamDto中的样品list
                for (SpectrumSpecimenDto specimen : paramDto.getSpecimens()) {
                    // 创建样品表
                    TMeasureSample sample = new TMeasureSample();
                    sample.setSampleNumber(specimen.getSampleNumber());
                    sample.setSampleName(specimen.getSampleName());
                    sample.setSampleType(specimen.getSampleType());
                    sample.setDilutionRatio(specimen.getDilutionRatio());
                    sample.setSampleVolume(specimen.getSampleVolume());
                    sample.setSampleLocation(specimen.getSampleLocation());
                    sample.setColor("#000000");
                    tMeasureSampleService.save(sample);
                    TaskSpecimen taskSpecimen = new TaskSpecimen();
                    taskSpecimen.setSpecimenKey(sample.getId());
                    taskSpecimen.setDeleteType("0");
                    taskSpecimen.setIsTested("0");
                    list.add(taskSpecimen);
                }
                break;
            case QUANTITATIVE_MEASUREMENT:
                for (SpectrumSpecimenDto specimen : paramDto.getSpecimens()) {
                    // 创建样品表
                    TMeasureSample sample = new TMeasureSample();
                    sample.setSampleNumber(specimen.getSampleNumber());
                    sample.setSampleName(specimen.getSampleName());
                    sample.setSampleType(specimen.getSampleType());
                    sample.setNormalConcentration(specimen.getNormalConcentration());
                    sample.setSampleLocation(specimen.getSampleLocation());
                    sample.setColor("#000000");
                    tMeasureSampleService.save(sample);
                    TaskSpecimen taskSpecimen = new TaskSpecimen();
                    taskSpecimen.setSpecimenKey(sample.getId());
                    taskSpecimen.setDeleteType("0");
                    taskSpecimen.setIsTested("0");
                    list.add(taskSpecimen);
                }
            break;
            case SPECTRAL_SCAN:
                for (SpectrumSpecimenDto specimen : paramDto.getSpecimens()) {
                    // 创建样品表
                    TMeasureSample sample = new TMeasureSample();
                    sample.setSampleNumber(specimen.getSampleNumber());
                    sample.setSampleName(specimen.getSampleName());
                    sample.setSampleLocation(specimen.getSampleLocation());
                    sample.setColor("#000000");
                    sample.setSampleType(SampleTypeEnum.kind_stand.getIdentity());
                    sample.setColor("");
                    tMeasureSampleService.save(sample);
                    TaskSpecimen taskSpecimen = new TaskSpecimen();
                    taskSpecimen.setSpecimenKey(sample.getId());
                    taskSpecimen.setDeleteType("0");
                    taskSpecimen.setIsTested("0");
                    list.add(taskSpecimen);
                }
                break;
            case TIME_SCAN:
                for (SpectrumSpecimenDto specimen : paramDto.getSpecimens()) {
                    // 创建样品表
                    TMeasureSample sample = new TMeasureSample();
                    sample.setSampleNumber(specimen.getSampleNumber());
                    sample.setSampleName(specimen.getSampleName());
                    sample.setSampleLocation(specimen.getSampleLocation());
                    sample.setColor("#000000");
                    tMeasureSampleService.save(sample);
                    TaskSpecimen taskSpecimen = new TaskSpecimen();
                    taskSpecimen.setSpecimenKey(sample.getId());
                    taskSpecimen.setDeleteType("0");
                    taskSpecimen.setIsTested("0");
                    list.add(taskSpecimen);
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
        ProjectTaskMethodVo vo = new ProjectTaskMethodVo();
        vo.setTask(tMeasureTask);
        vo.setProject(project);
        vo.setMethod(tMeasureMethodService.getById(tMeasureTask.getMethodKey()));
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
        // 根据项目id和方法类型查询方法表数据
        List<TMeasureMethod> list = tMeasureMethodService.list(new QueryWrapper<TMeasureMethod>().eq("project_item_key", projectId).eq("measurement_type", methodType));
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

    /**
     * 校零接口
     */
    @Override
    public Result zeroCalibrationPort(String serialNumber) {
        String ip = deviceInfoService.getIp(serialNumber);
        Command command = new Command();
        command.setCmdId(SetParamEnum.SetWl.getComId());
        command.setParams(new Object[]{560});
        devMessageUtil.wavelengthLocate(command,ip);
        Command com = new Command();
        com.setCmdId(SetParamEnum.Zero.getComId());
        com.setParams(new Object[]{});
        R<String> stringR = devMessageUtil.zeroCalibration(com);
        return Result.ok(stringR);
    }

    @Override
    public Result reportDataQuery(String taskId,String reportName) {

        if ((taskId == null)) {
            return Result.fail("任务id不能为空");
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        // 根据任务id测量数据表
        List<TMeasureData> measureData = tMeasureDataService.list(new QueryWrapper<TMeasureData>().eq("task_id", taskId).eq("is_delete", "0"));
        if (measureData == null || measureData.size() == 0) {
            return Result.fail("未查询到数据");
        }
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("task_id", taskId).eq("is_delete", "0"));
        if (analysisData == null || analysisData.size() == 0) {
            return Result.fail("未查询到数据");
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

            List<HeadVO> vos = photometricService.tableHead(taskId, method.getId(),false).getData();
            heads.addAll(vos);
            heads.add(new HeadVO("备注", "remark"));
            heads.add(new HeadVO("采样时间", "samplingTime"));
            vo.setHeadList(heads);

            List<Map<String, String>> list = photometricService.queryData(taskId, false).getData();
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
            List<ReportDataVo> list = new ArrayList<>();
            for (TMeasureData data : measureData) {
                ReportDataVo vo = new ReportDataVo();
                // 根据data的样品id查询样品名称
                TMeasureSample sample = tMeasureSampleService.getById(data.getSampleKey());
                vo.setName(sample.getSampleName());
                vo.setPeak(JSON.parseArray(data.getPeak(), String.class));
                vo.setValley(JSON.parseArray(data.getGrain(), String.class));
                vo.setMarkPoint(JSON.parseArray(data.getGaugePoint(), String.class));
                vo.setParameterData(JSON.parseArray(data.getOriginalCalculationData(), String.class));
                vo.setDataType("测量数据");
                list.add(vo);
            }
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
            return Result.ok(list);
        }else if (reportName.equals("时间扫描表格")){
            return Result.ok("暂未开放");
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
