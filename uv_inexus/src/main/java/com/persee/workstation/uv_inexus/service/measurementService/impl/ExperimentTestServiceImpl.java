package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.dao.userManageDao.UserConfigDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureData;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureProject;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.userManagePo.UserConfigPO;
import com.persee.workstation.uv_inexus.service.measurementService.*;
import com.persee.workstation.uv_inexus.vo.measurementVo.ProjectStatisticsVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.RecentlyAccomplishTaskVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
("experimentTestService")
public class ExperimentTestServiceImpl implements ExperimentTestService {
    @Autowired
    private TMeasureProjectService tMeasureProjectService;
    @Autowired
    private UserConfigDAO userConfigDAO;
    @Autowired
    private TMeasureTaskService tMeasureTaskService;
    @Autowired
    private TMeasureDataService tMeasureDataService;
    @Autowired
    private TMeasureMethodService tMeasureMethodService;
    @Autowired
    private TMeasureStandardService tMeasureStandardService;
    @Autowired
    private TMeasureSampleService tMeasureSampleService;
    @Autowired
    private SpectrumService spectrumService;
    @Autowired
    private PhotometricService photometricService;

    @Override
    public Result newProject(ProjectParamDto paramDto) {
        // 创建项目表
        TMeasureProject tMeasureProject = new TMeasureProject();
        tMeasureProject.setProjectName(paramDto.getProjectName());
        tMeasureProject.setRemark(paramDto.getRemark());
        tMeasureProject.setCreatorKey(paramDto.getUserName());
        tMeasureProject.setCreateTime(LocalDateTime.now());
        boolean save = tMeasureProjectService.save(tMeasureProject);
        return Result.ok(save);
    }

    @Override
    public Result recentlyAccomplishTask(String updateBy) {
        // 根据用户名查询用户id
        UserConfigPO user = userConfigDAO.selectOne(Wrappers.lambdaQuery(UserConfigPO.class).eq(UserConfigPO::getUserName, updateBy));
        // 根据用户id查询任务表
        List<TMeasureTask> tasks = tMeasureTaskService.list(Wrappers.lambdaQuery(TMeasureTask.class).eq(TMeasureTask::getUserId, user.getId()));
        // 提取任务id
        List<String> taskIds = tasks.stream().map(TMeasureTask::getId).collect(Collectors.toList());
        // 根据任务表中的id查询测量数据
        List<TMeasureData> measureData = tMeasureDataService.list(Wrappers.lambdaQuery(TMeasureData.class)
                .in(TMeasureData::getTaskId, taskIds)
                .orderByDesc(TMeasureData::getMeasurementStartTime)
                .last("LIMIT 3"));

        ArrayList<RecentlyAccomplishTaskVo> list = new ArrayList<>();
        for (TMeasureData tMeasureData : measureData) {
            RecentlyAccomplishTaskVo vo = new RecentlyAccomplishTaskVo();
            TMeasureTask task = tMeasureTaskService.getById(tMeasureData.getTaskId());
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            TMeasureProject project = tMeasureProjectService.getById(method.getProjectItemKey());
            vo.setMeasureId(tMeasureData.getId());
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getTaskName());
            vo.setMethodId(method.getId());
            vo.setMethodName(method.getMethodName());
            vo.setProjectId(project.getId());
            vo.setProjectName(project.getProjectName());
            vo.setMeasurementStartTime(tMeasureData.getMeasurementStartTime());
            vo.setMeasurementEndTime(tMeasureData.getMeasurementEndTime());
            list.add(vo);
        }
        return Result.ok(list);
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
        UserConfigPO user = userConfigDAO.selectOne(Wrappers.lambdaQuery(UserConfigPO.class).eq(UserConfigPO::getUserName, updateBy));
        long countProject = tMeasureProjectService.count(
                Wrappers.lambdaQuery(TMeasureProject.class)
                        .and(wrapper -> wrapper.eq(TMeasureProject::getCreatorKey, updateBy)
                                .or()
                                .eq(TMeasureProject::getLastUpdatedBy, updateBy)));
        Long countMethod = tMeasureMethodService.count(
                Wrappers.lambdaQuery(TMeasureMethod.class)
                        .eq(TMeasureMethod::getMethodCreator, updateBy)
        );
        Long countTask = tMeasureTaskService.count(
                Wrappers.lambdaQuery(TMeasureTask.class)
                        .eq(TMeasureTask::getUserId, user.getId())
                        .isNotNull(TMeasureTask::getCollectionStartTime)
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
            vo.setCreateTime(project.getCreateTime());
            vo.setUpdateBy(project.getLastUpdatedBy());
            vo.setUpdateTime(project.getLastUpdatedTime());
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
    public Result createMethodPort(MethodParamRequestDto methodDto,String updateBy) {
        // 根据方法名称去方法表中进行查询,查看是否有重复
        List<TMeasureMethod> list = tMeasureMethodService.list(Wrappers.lambdaQuery(TMeasureMethod.class)
                .eq(TMeasureMethod::getMethodName, methodDto.getMethodName()));
        if (list.size() > 0) {
            return Result.fail("方法名称重复");
        }
        Map<String, Object> params = methodDto.getParams();
        MeasureTypeEnum type = MeasureTypeEnum.fromDescription(methodDto.getMethodType());
        boolean save = false;
        // TODO 实验测试的创建方法需要完善
        switch (type){
            case PHOTOMETRIC_MEASUREMENT:
                PhotometricParameter photometricParameter = convertParams(params, PhotometricParameter.class);
                return photometricService.createMethodPort(methodDto, updateBy, photometricParameter);
            case QUANTITATIVE_MEASUREMENT:
                break;
            case SPECTRAL_SCAN:
                SpectrumParameter spectrumParameter = convertParams(params, SpectrumParameter.class);
                return spectrumService.createMethodPort(methodDto, updateBy, spectrumParameter);
            case TIME_SCAN:
                break;
            case TWO_COMPONENT_QUANTITATIVE_ANALYSIS:
                break;
        }
        if (save){
            return Result.ok("创建成功");
        } else {
            return Result.fail("创建失败");
        }
    }

    @Override
    public Result createTask(CreateTaskParamDto paramDto) {
        MeasureTypeEnum type = MeasureTypeEnum.fromDescription(paramDto.getMeasurementType());
        // TODO 所有测量的创建任务需要完善
        switch (type){
            case PHOTOMETRIC_MEASUREMENT:
                return photometricService.createTask(paramDto);
            case QUANTITATIVE_MEASUREMENT:

            case SPECTRAL_SCAN:
                return spectrumService.createTask(paramDto);
            case TIME_SCAN:

            case TWO_COMPONENT_QUANTITATIVE_ANALYSIS:
        }
        return Result.fail("创建失败");
    }

    @Override
    public Result queryTaskPort(QueryTaskDto queryTask) {
        MeasureTypeEnum type = MeasureTypeEnum.fromDescription(queryTask.getMeasurementType());
        // TODO 所有测量的查看任务查询需要完善
        switch (type){
            case PHOTOMETRIC_MEASUREMENT:
                return photometricService.queryTaskPort(queryTask);
            case QUANTITATIVE_MEASUREMENT:
                return Result.fail("暂不支持");
            case SPECTRAL_SCAN:
                return spectrumService.queryTaskPort(queryTask);
            case TIME_SCAN:
                return Result.fail("暂不支持");
            case TWO_COMPONENT_QUANTITATIVE_ANALYSIS:
                return Result.fail("暂不支持");
        }
        return Result.fail("暂不支持");
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
     * @param updateBy
     * @return
     */
    @Override
    public Result queryAllProject(String updateBy) {
        // 根据用户名查询项目
        List<TMeasureProject> projects = tMeasureProjectService.list(new QueryWrapper<TMeasureProject>().eq("creator_key", updateBy));
        return Result.ok(projects);
    }

    @Override
    public Result queryMethodProt(String projectId, String measureType) {
        MeasureTypeEnum type = MeasureTypeEnum.fromDescription(measureType);
        // TODO 所有测量的查看任务查询需要完善
        switch (type){
            case PHOTOMETRIC_MEASUREMENT:
                return photometricService.queryMethodProt(projectId, measureType);
            case QUANTITATIVE_MEASUREMENT:
                return Result.fail("暂不支持");
            case SPECTRAL_SCAN:
                return spectrumService.queryMethodProt(projectId, measureType);
            case TIME_SCAN:
                return Result.fail("暂不支持");
            case TWO_COMPONENT_QUANTITATIVE_ANALYSIS:
                return Result.fail("暂不支持");
        }
        return Result.fail("暂不支持");
    }

    private <T> T convertParams(Map<String, Object> params, Class<T> clazz) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 将 Map 转换为指定的 Java 对象
            return objectMapper.convertValue(params, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
