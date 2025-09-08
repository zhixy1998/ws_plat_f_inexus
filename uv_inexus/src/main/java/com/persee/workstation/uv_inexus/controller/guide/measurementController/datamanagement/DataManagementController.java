package com.persee.workstation.uv_inexus.controller.guide.measurementController.datamanagement;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodManagementDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.ProjectManagementDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.SelectMethodsByTypeResult;
import com.persee.workstation.uv_inexus.dto.measurementDto.TaskManagementDto;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.service.guide.dataManagementService.DataManagementService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@ApiModel("数据管理接口")
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/")
public class DataManagementController {
    @Autowired
    private DataManagementService dataManagementService;

    /**
     * 项目
     *
     * @return
     */

    /*
    用户参与任务和方法个数回显
     */
    @GetMapping("/getMethodsAndTasks")
    @ApiOperation("用户参与任务和方法个数回显")
    public R<List<Integer>> getMethodsAndTasks(@RequestParam("updateBy") String userKey) {
        return dataManagementService.getMethodsAndTasks(userKey);
    }

    //项目列表
    @GetMapping("/getProjectsManagementList")
    @ApiOperation("所有项目查询")
    public R<List<ProjectManagementDto>> getProjectsManagementList() {
        return dataManagementService.getProjectsManagementList();
    }


    @GetMapping("/selectProjects")
    @ApiOperation("模糊查询项目接口")
    public R<Page<ProjectManagementDto>> selectProjects(
            @RequestParam("selectParm") String selectParm,
            @RequestParam("pageNum") Integer pageNum,
            @RequestParam("pageSize") Integer pageSize) {
        return dataManagementService.selectProjects(selectParm, pageNum, pageSize);
    }

    /**
     * 方法
     *
     * @return
     */
    @GetMapping("/getMethodsAndSuccessful")
    @ApiOperation("所有方法和已完成方法回显")
    public R<List<Integer>> getMethodsAndSuccessful(@RequestParam("updateBy") String userKey) {
        return dataManagementService.getMethodsAndSuccessful(userKey);
    }

    @GetMapping("/getMethodsManagementList")
    @ApiOperation("方法查询")
    public R<List<MethodManagementDto>> getMethodsManagementList() {
        return dataManagementService.getMethodManagement();
    }

    @GetMapping("/selectMethodsByType")
    @ApiOperation("分类查询//方法统计")
    public R<List<SelectMethodsByTypeResult>> selectMethodsByType() {
        return dataManagementService.selectMethodsByType();
    }

    @GetMapping("/selectMethods")
    @ApiOperation("模糊查询方法接口")
    public R<Page<MethodManagementDto>> selectMethods(
            @RequestParam("selectParm") String selectParm,
            @RequestParam("pageNum") Integer pageNum,
            @RequestParam("pageSize") Integer pageSize) {
        return dataManagementService.selectMethods(selectParm, pageNum, pageSize);
    }

//任务

    @GetMapping("/getTasksAndSuccessful")
    @ApiOperation("任务数量和已完成任务数回显")
    public R<List<Integer>> getTasksAndSuccessful(@RequestParam("updateBy") String userKey) {
        return dataManagementService.getTasksAndSuccessful(userKey);
    }

    @GetMapping("/getTasksManagementList")
    @ApiOperation("任务查询")
    public R<List<TaskManagementDto>> getTasksManagementList() {
        return dataManagementService.getTasksManagementList();
    }

}
