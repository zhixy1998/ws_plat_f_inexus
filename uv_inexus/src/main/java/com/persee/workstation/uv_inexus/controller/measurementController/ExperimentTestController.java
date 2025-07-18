package com.persee.workstation.uv_inexus.controller.measurementController;

import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.ExperimentTestService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/")
@ApiModel("实验测试")
public class ExperimentTestController {
    @Autowired
    private ExperimentTestService experimentTestService;


    @GetMapping("recentlyAccomplishTask")
    @ApiOperation("最近完成的任务")
    public Result recentlyAccomplishTask(@RequestParam String updateBy) {
       return experimentTestService.recentlyAccomplishTask(updateBy);
    }

    @GetMapping("queryAllProject")
    @ApiOperation("查询所有项目接口")
    public Result queryAllProject(@RequestParam String updateBy) {
        return experimentTestService.queryAllProject(updateBy);
    }


    @PostMapping("newProject")
    @ApiOperation("新建项目接口")
    public Result newProject(@RequestBody ProjectParamDto paramDto) {
        return experimentTestService.newProject(paramDto);
    }

    @PostMapping("createMethodPort")
    @ApiOperation("新建方法接口")
    public Result createMethodPort(@RequestBody MethodParamRequestDto methodDto) {
            return experimentTestService.createMethodPort(methodDto);
    }

    @PostMapping("createTask")
    @ApiOperation("新建任务接口")
    public Result createTask(@RequestBody CreateTaskParamDto paramDto) {
        return experimentTestService.createTask(paramDto);
    }

    @GetMapping("queryProjectStatistics")
    @ApiOperation("查询项目统计接口")
    public Result queryProjectStatistics(@RequestParam String updateBy) {
        return experimentTestService.queryProjectStatistics(updateBy);
    }

    @PostMapping("queryTaskPort")
    @ApiOperation("查看任务接口")
    public Result queryTaskPort(@RequestBody QueryTaskDto queryTask) {
        return experimentTestService.queryTaskPort(queryTask);
    }

    @GetMapping("queryMethodProt")
    @ApiOperation("查找方法接口")
    public Result queryMethodProt(@RequestParam String projectId,@RequestParam String measureType,@RequestParam("pageNum") Integer pageNum,@RequestParam("pageSize") Integer pageSize) {
        return experimentTestService.queryMethodProt(projectId,measureType,pageNum,pageSize);
    }

    @GetMapping("continueMeasuringPort")
    @ApiOperation("继续测量接口")
    public Result continueMeasuringPort(@RequestParam String projectId,@RequestParam String methodId){
        return experimentTestService.continueMeasuringPort(projectId, methodId);
    }

    @GetMapping("assignMethodPort")
    @ApiOperation("指定方法接口")
    public Result assignMethodPort(@RequestParam String methodType,@RequestParam String projectId ){
        return experimentTestService.assignMethodPort(methodType,projectId);
    }

    @GetMapping("queryReferenceStandard")
    @ApiOperation("查询参考标准接口")
    public Result queryReferenceStandard(@RequestParam String standardName){
        return experimentTestService.queryReferenceStandard(standardName);
    }

    @GetMapping("preprocessTheData")
    @ApiOperation("前处理数据接口")
    public Result preprocessTheData(@RequestParam String standardId){
        return experimentTestService.preprocessTheData(standardId);
    }

    @GetMapping("zeroCalibrationPort")
    @ApiOperation("校零接口")
    public Result zeroCalibrationPort(String serialNumber) {
        return experimentTestService.zeroCalibrationPort(serialNumber);
    }





}
