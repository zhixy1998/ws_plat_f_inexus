/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.controller.guide.measurementController.datamanagement;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.concise.common.Box;
import com.persee.workstation.uv_inexus.dto.concise.common.CopyParam;
import com.persee.workstation.uv_inexus.dto.measurementDto.ProjectManagementDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.TaskManagementDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.UnFinishTasksAndNumParam;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.MethodAndTasks;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TaskQueryParam;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.UnFinishTasksAndNum;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.ProjectQueryParam;

import com.persee.workstation.uv_inexus.service.guide.dataManagementService.DataManagementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Api("数据管理接口")
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/")
public class DataManagementController {

    @Autowired
    private DataManagementService dataManagementService;




    @PostMapping("Tasks")
    @ApiOperation("查询已完成和未完成任务")
    public Result<UnFinishTasksAndNum> unFinishTasksAndNum(@RequestBody UnFinishTasksAndNumParam unFinishTasksAndNumParam) {
        return dataManagementService.getTasks(unFinishTasksAndNumParam);


    }

    @GetMapping("/methodsAndTasksNum")
    @ApiOperation("总任务数，已完成任务数，已完成方法数，已完成任务数")
    public Result<MethodAndTasks> getMethodsAndTasksNum(@RequestParam("updateBy") String userKey) {
        return dataManagementService.getMethodsAndTasksNum(userKey);
    }

    //项目列表
    @PostMapping("/getProjectsManagementList")
    @ApiOperation("所有项目查询")
    public Result<Page<ProjectManagementDto>> getProjectsManagementList(@RequestBody ProjectQueryParam projectQueryParam) {
        return dataManagementService.getProjectsManagementList(projectQueryParam);
    }

    //任务
    @PostMapping("/getTasksManagementList")
    @ApiOperation("任务分页分类查询")
    public Result<Page<TaskManagementDto>> getTasksManagementList(@RequestBody TaskQueryParam taskQueryParam) {
        return dataManagementService.getTasksManagementList(taskQueryParam);
    }

//    @PostMapping("/formulaCheck")
//    @ApiOperation("公式校验")
//    public Result<String> formulaCheck(@RequestBody FormulaCheckParam formulaCheckParam) {
//        return dataManagementService.formulaCheck(formulaCheckParam);
//    }
    @PostMapping("/deleteTask")
    @ApiOperation("任务删除")
    public Result<String> deleteTask(@RequestBody Box deleteTaskIdList) {
        return dataManagementService.deleteTask(deleteTaskIdList);
    }


    @PostMapping("/deleteMethod")
    @ApiOperation("方法删除")
    public Result<String> deleteMethod(@RequestBody Box deleteMethodIdList) {
        return dataManagementService.deleteMethod(deleteMethodIdList);
    }

    @PostMapping("/deleteProject")
    @ApiOperation("项目删除")
    public Result<String> deleteProject(@RequestBody Box deleteProjectIdList) {
        return dataManagementService.deleteProject(deleteProjectIdList);
    }
}
