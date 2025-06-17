package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.dao.userConfigDao.UserConfigDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.CreateTaskParamDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.ProjectParamDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumSpecimenDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureProject;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureSample;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.systemctlConfigPo.UserConfigPO;
import com.persee.workstation.uv_inexus.service.measurementService.ExperimentTestService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureProjectService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
("experimentTestService")
public class ExperimentTestServiceImpl implements ExperimentTestService {
    @Autowired
    private TMeasureProjectService tMeasureProjectService;
    @Autowired
    private UserConfigDAO userConfigDAO;
    @Autowired
    private TMeasureTaskService tMeasureTaskService;
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
    public Result createTask(CreateTaskParamDto paramDto) {
        // 根据项目id查询项目
        TMeasureProject project = tMeasureProjectService.getById(paramDto.getProjectId());
        //根据项目中的用户名称查询用户
        UserConfigPO userConfigPO = userConfigDAO.selectOne(Wrappers.lambdaQuery(UserConfigPO.class).eq(UserConfigPO::getUserName, project.getCreatorKey()));
        // 创建任务类
        TMeasureTask tMeasureTask = new TMeasureTask();
        tMeasureTask.setUserId(userConfigPO.getId());
        tMeasureTask.setTaskName(paramDto.getTaskName());
        tMeasureTask.setCreateTime(LocalDateTime.now());
        tMeasureTask.setMethodKey(paramDto.getMethodId());
        tMeasureTask.setProjectItemKey(project.getId());
        tMeasureTaskService.save(tMeasureTask);
        // 遍历CreateTaskParamDto中的样品list
        for (SpectrumSpecimenDto specimen : paramDto.getSpecimens()) {
            // 创建样品表
            TMeasureSample sample = new TMeasureSample();
            sample.setTaskId(tMeasureTask.getId());
            sample.setSampleName(specimen.getName());
            sample.setSampleLocation(specimen.getSampleLocation());
        }
        return Result.ok();
    }

    @Override
    public Result queryAllTask(String userName,  Integer quantity) {
        // 根据用户名查询用户

        // 根据用户id查询任务(条件1: 根据采集)

        //根据项目id查询任务
        return null;
    }
}
