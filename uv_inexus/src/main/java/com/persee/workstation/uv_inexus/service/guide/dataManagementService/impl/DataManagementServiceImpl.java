package com.persee.workstation.uv_inexus.service.guide.dataManagementService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureMethodMapper;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureProjectMapper;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.ProjectManagementDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.SelectMethodsByTypeResult;
import com.persee.workstation.uv_inexus.dto.measurementDto.TaskManagementDto;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureProject;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TaskQueryParam;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.ProjectQueryParam;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.guide.dataManagementService.DataManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataManagementServiceImpl implements DataManagementService {

    @Autowired
    private TMeasureMethodMapper methodManagementMapper;
    @Autowired
    private TMeasureProjectMapper projectManagementMapper;
    @Autowired
    private TMeasureTaskMapper taskManagementMapper;
    @Autowired
    private SystemUserManageDAO systemUserManageDAO;


    //方法
    
/*
方法任务全部数据返回
 */
    @Override
    public Result<MethodAndTasks> getMethodsAndTasksNum(String userKey) {
        LambdaQueryWrapper<SystemUserManagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemUserManagePO::getUserName, userKey);
        SystemUserManagePO user = systemUserManageDAO.selectOne(queryWrapper);
        String userName = user.getUserName();
        Integer methodsNum = methodManagementMapper.getMethodsNum(userName);
        MethodAndTasks methodAndTasks = new MethodAndTasks();
        //总创建方法
        methodAndTasks.setAllMethodNum(methodsNum);
        //已完成方法
        String id = user.getId();
        Set<String> method = taskManagementMapper.selectMethodIds(id);
        Integer size = method.size();
        methodAndTasks.setUsedMethodNum(size);
        String userId = user.getId();
        //查询用户参与的所有任务个数
        Integer tasksNum = taskManagementMapper.getTasksNum(userId);
        methodAndTasks.setAllTaskNum(tasksNum);

        //查询已完成的任务
        Integer finishTaskNum = taskManagementMapper.getFinishTaskNum(userId);
        methodAndTasks.setFinishTaskNum(finishTaskNum);
        //方法统计封装
        try {
            List<SelectMethodsByTypeResult> results = methodManagementMapper.selectMethodsGroupByType();
            methodAndTasks.setMethodCounts(results);
        } catch (Exception e) {
            log.error("根据类型分类查询方法失败", e);
            return Result.fail("查询失败");
        }
        //任务统计封装
        TaskCount taskCount = new TaskCount();
        LocalDateTime now = LocalDateTime.now();

        // 统计各个时间段的数据
        taskCount.setLastWeek(getTaskCountByTimeRange(now.minusWeeks(1), now));
        taskCount.setLastMonth(getTaskCountByTimeRange(now.minusMonths(1), now));
        taskCount.setHalfYear(getTaskCountByTimeRange(now.minusMonths(6), now));
        taskCount.setLastYear(getTaskCountByTimeRange(now.minusYears(1), now));
        taskCount.setThreeYears(getTaskCountByTimeRange(now.minusYears(3), now));
        taskCount.setAll(getTaskCountByTimeRange(null, null)); // 所有数据

        // 将任务统计结果设置到 methodAndTasks 中
        methodAndTasks.setTaskCounts(taskCount);

        return Result.ok(methodAndTasks);


    }

    @Override
    public Result<Page<ProjectManagementDto>> getProjectsManagementList(ProjectQueryParam projectQueryParam) {
      // 参数校验
      if (projectQueryParam == null) {
          return Result.fail("查询参数不能为空");
      }

        // 分页参数默认值设置
        int pageNum = projectQueryParam.getPage() != null ? projectQueryParam.getPage() : 1;
        int pageSize = projectQueryParam.getSize() != null ? projectQueryParam.getSize() : 10;

        Page<TMeasureProject> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TMeasureProject> queryWrapper = new LambdaQueryWrapper<>();

        // 模糊查询条件（添加空值判断）
        if (projectQueryParam.getKeyword() != null) {
            queryWrapper.like(TMeasureProject::getProjectName, projectQueryParam.getKeyword());
        }

        Page<TMeasureProject> result = projectManagementMapper.selectPage(page, queryWrapper);
        Page<ProjectManagementDto> dtoPage = new Page<>();
        dtoPage.setCurrent(result.getCurrent());
        dtoPage.setSize(result.getSize());
        dtoPage.setTotal(result.getTotal());
        dtoPage.setPages(result.getPages());

        List<ProjectManagementDto> dtoList = result.getRecords().stream()
                .map(this::projectConvertToDto)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        return Result.ok(dtoPage);
    }

    private ProjectManagementDto projectConvertToDto(TMeasureProject tMeasureProject) {
        String id = tMeasureProject.getId();
        ProjectManagementDto dto = new ProjectManagementDto();
        BeanUtils.copyProperties(tMeasureProject, dto);
        LambdaQueryWrapper<TMeasureTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureTask::getProjectItemKey,id);
        List<TMeasureTask> tMeasureTasks = taskManagementMapper.selectList(queryWrapper);
        int size = tMeasureTasks.size();
        Long size1 = (long) size;
        dto.setTaskNum(size1);

        LambdaQueryWrapper<TMeasureMethod> meQueryWrapper = new LambdaQueryWrapper<>();
        meQueryWrapper.eq(TMeasureMethod::getProjectItemKey,id);
        List<TMeasureMethod> tMeasureMethods = methodManagementMapper.selectList(meQueryWrapper);

        int size3 = tMeasureMethods.size();
        Long size4 = (long) size3;
        dto.setMethodNum(size4);
        return dto;
    }

    private TaskCountByTime getTaskCountByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<TMeasureTask> queryWrapper = new QueryWrapper<>();

        // 添加时间范围条件
        if (startTime != null) {
            queryWrapper.ge("start_time", startTime);
        }
        if (endTime != null) {
            queryWrapper.le("end_time", endTime);
        }

        // 统计总数
        Long totalCount = taskManagementMapper.selectCount(queryWrapper);

        // 统计完成数量
        QueryWrapper<TMeasureTask> finishedWrapper = new QueryWrapper<>();
        if (startTime != null) {
            finishedWrapper.ge("start_time", startTime);
        }
        if (endTime != null) {
            finishedWrapper.le("end_time", endTime);
        }
        finishedWrapper.eq("is_delete", 1);
        Long finishedCount = taskManagementMapper.selectCount(finishedWrapper);

        // 计算未完成数量
        Long unFinishedCount = totalCount - finishedCount;

        // 封装结果
        TaskCountByTime countByTime = new TaskCountByTime();
        countByTime.setFinishedCount(finishedCount);
        countByTime.setUnFinishedCount(unFinishedCount);

        return countByTime;
    }


    /*
    查询全部任务
     */
    @Override
    public Result<Page<TaskManagementDto>> getTasksManagementList(TaskQueryParam taskQueryParam) {
        // 参数校验
        if (taskQueryParam == null) {
            return Result.fail("查询参数不能为空");
        }

        // 分页参数默认值设置
        int pageNum = taskQueryParam.getPage() != null ? taskQueryParam.getPage() : 1;
        int pageSize = taskQueryParam.getSize() != null ? taskQueryParam.getSize() : 10;

        Page<TMeasureTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TMeasureTask> queryWrapper = new LambdaQueryWrapper<>();

        // 精确查询条件（添加空值判断）
        if (taskQueryParam.getSelectType() != null) {
            queryWrapper.eq(TMeasureTask::getMeasureType, taskQueryParam.getSelectType());
        }

        // 模糊查询条件（添加空值判断）
        if (StringUtils.isNotBlank(taskQueryParam.getKeyword())) {
            queryWrapper.like(TMeasureTask::getTaskName, taskQueryParam.getKeyword());
        }

        Page<TMeasureTask> result = taskManagementMapper.selectPage(page, queryWrapper);
        Page<TaskManagementDto> dtoPage = new Page<>();
        dtoPage.setCurrent(result.getCurrent());
        dtoPage.setSize(result.getSize());
        dtoPage.setTotal(result.getTotal());
        dtoPage.setPages(result.getPages());

        List<TaskManagementDto> dtoList = result.getRecords().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        return Result.ok(dtoPage);
    }

    private TaskManagementDto convertToDto(TMeasureTask entity) {
        TaskManagementDto dto = new TaskManagementDto();
        BeanUtils.copyProperties(entity, dto);
        LambdaQueryWrapper<TMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureMethod::getId,entity.getMethodKey());
        TMeasureMethod tMeasureMethod = methodManagementMapper.selectOne(queryWrapper);
        dto.setMethodName(tMeasureMethod.getMethodName());
        LambdaQueryWrapper<TMeasureProject> proQueryWrapper = new LambdaQueryWrapper<>();
        proQueryWrapper.eq(TMeasureProject::getId,entity.getProjectItemKey());
        TMeasureProject tMeasureProject = projectManagementMapper.selectOne(proQueryWrapper);
        dto.setProjectName(tMeasureProject.getProjectName());
        return dto;
    }


}
