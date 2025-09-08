package com.persee.workstation.uv_inexus.service.guide.dataManagementService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.MethodManagementMapper;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.ProjectManagementMapper;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.TaskManagementMapper;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodManagementDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.ProjectManagementDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.SelectMethodsByTypeResult;
import com.persee.workstation.uv_inexus.dto.measurementDto.TaskManagementDto;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureProject;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.guide.dataManagementService.DataManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DataManagementServiceImpl implements DataManagementService {

    @Autowired
    private MethodManagementMapper methodManagementMapper;
    @Autowired
    private ProjectManagementMapper projectManagementMapper;
    @Autowired
    private TaskManagementMapper taskManagementMapper;
    @Autowired
    private SystemUserManageDAO systemUserManageDAO;


    //方法


    /*
    用户参与方法个数回显
     */
    @Override
    public R<List<Integer>> getMethodsAndSuccessful(String userKey) {
        QueryWrapper<SystemUserManagePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userKey);
        SystemUserManagePO user = systemUserManageDAO.selectOne(queryWrapper);
        return null;
    }


    /**
     * 查询所有方法
     */
    public R<List<MethodManagementDto>> getMethodManagement() {
        List<FMeasureMethod> FMeasureMethodList = methodManagementMapper.selectList(null);
        if (FMeasureMethodList.isEmpty()) {
            return R.ok(new ArrayList<>());
        }
        ArrayList<MethodManagementDto> methodManagementDtos = new ArrayList<>();
        for (FMeasureMethod fMeasureMethod : FMeasureMethodList) {
            MethodManagementDto methodManagementDto = new MethodManagementDto();
            BeanUtils.copyProperties(fMeasureMethod, methodManagementDto);
            methodManagementDtos.add(methodManagementDto);
        }
        return R.ok(methodManagementDtos);
    }

    /**
     * 模糊查询方法接口
     *
     * @return
     */
    @Override
    public R<Page<MethodManagementDto>> selectMethods(String selectParm, Integer pageNum, Integer pageSize) {
        Page<FMeasureMethod> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
        if (selectParm != null && !selectParm.trim().isEmpty()) {
            queryWrapper.like(FMeasureMethod::getMethodName, selectParm);
        }
        Page<FMeasureMethod> resultPage = methodManagementMapper.selectPage(page, queryWrapper);
        Page<MethodManagementDto> dtoPage = new Page<>(pageNum, pageSize, resultPage.getTotal());
        List<MethodManagementDto> dtoList = new ArrayList<>();
        for (FMeasureMethod record : resultPage.getRecords()) {
            MethodManagementDto dto = new MethodManagementDto();
            BeanUtils.copyProperties(record, dto);
            dtoList.add(dto);
        }
        dtoPage.setRecords(dtoList);
        return R.ok(dtoPage);
    }

    /**
     * 根据类型查询方法
     *
     * @return
     */
    public R<List<SelectMethodsByTypeResult>> selectMethodsByType() {
        try {
            List<SelectMethodsByTypeResult> results = methodManagementMapper.selectMethodsGroupByType();
            return R.ok(results);
        } catch (Exception e) {
            log.error("根据类型分类查询方法失败", e);
            return R.fail("查询失败");
        }
    }

    //任务

    /*
    用户参与任务个数和成功任务数
     */
    @Override
    public R<List<Integer>> getTasksAndSuccessful(String userKey) {
        QueryWrapper<SystemUserManagePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userKey);
        SystemUserManagePO user = systemUserManageDAO.selectOne(queryWrapper);
        return null;

    }


    /*
    查询全部任务
     */
    @Override
    public R<List<TaskManagementDto>> getTasksManagementList() {
        List<FMeasureTask> tasks = taskManagementMapper.selectList(null);
        if (tasks.isEmpty()) {
            return R.ok(new ArrayList<>());
        }
        ArrayList<TaskManagementDto> taskManagementDtos = new ArrayList<>();
        for (FMeasureTask fMeasureTask : tasks) {
            TaskManagementDto taskManagementDto = new TaskManagementDto();
            BeanUtils.copyProperties(fMeasureTask, taskManagementDto);
            taskManagementDtos.add(taskManagementDto);
        }
        return R.ok(taskManagementDtos);
    }

    //项目

    /*
    /*
    用户参与方法个数回显
     */
    @Override
    public R<List<Integer>>  getMethodsAndTasks(String userKey) {
        QueryWrapper<SystemUserManagePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userKey);
        SystemUserManagePO user = systemUserManageDAO.selectOne(queryWrapper);
        if (user == null) {
            return R.fail("没有这个人");
        }
        String userName = user.getUserName();
        ArrayList<Integer> MethodsAndTasks = new ArrayList<>();
        Integer methodsNum = methodManagementMapper.getMethodsNum(userName);
        if (methodsNum == null) {
            return R.fail("用户名错误");
        }
        MethodsAndTasks.add(methodsNum);

        String userId = user.getId();
        Integer tasksNum = taskManagementMapper.getTasksNum(userId);
        if (tasksNum == null) {
            return R.fail("id错误");
        }
        MethodsAndTasks.add(tasksNum);
        return R.ok(MethodsAndTasks);
    }


    @Override
    public R<List<ProjectManagementDto>> getProjectsManagementList() {
        List<FMeasureProject> projectList = projectManagementMapper.selectList(null);
        if (projectList.isEmpty()) {
            return R.ok(new ArrayList<>());
        }
        ArrayList<ProjectManagementDto> projectManagementDtos = new ArrayList<>();
        for (FMeasureProject fMeasureProject : projectList) {
            ProjectManagementDto projectManagementDto = new ProjectManagementDto();
            BeanUtils.copyProperties(fMeasureProject, projectManagementDto);
            projectManagementDtos.add(projectManagementDto);
        }
        return R.ok(projectManagementDtos);
    }

    /****
     * 分类模糊查询
     * @param selectParm
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public R<Page<ProjectManagementDto>> selectProjects(String selectParm, Integer pageNum, Integer pageSize) {
        Page<FMeasureProject> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FMeasureProject> queryWrapper = new LambdaQueryWrapper<>();
        if (selectParm != null && !selectParm.trim().isEmpty()) {
            queryWrapper.like(FMeasureProject::getProjectName, selectParm);
        }
        Page<FMeasureProject> resultPage = projectManagementMapper.selectPage(page, queryWrapper);
        Page<ProjectManagementDto> dtoPage = new Page<>(pageNum, pageSize, resultPage.getTotal());
        List<ProjectManagementDto> dtoList = new ArrayList<>();
        for (FMeasureProject record : resultPage.getRecords()) {
            ProjectManagementDto dto = new ProjectManagementDto();
            BeanUtils.copyProperties(record, dto);
            dtoList.add(dto);
        }
        dtoPage.setRecords(dtoList);
        return R.ok(dtoPage);
    }


}
