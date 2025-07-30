package com.persee.workstation.uv_inexus.controller.measurementController;

import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.TimeSweepService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/timeSweep/")
@ApiModel("时间扫描")
public class TimeSweepController {
    @Autowired
    private TimeSweepService timeSweepService;


    @GetMapping("queryEssentialInformation")
    @ApiOperation("查询项目任务方法接口")
    public Result queryProjectTaskMethod(@RequestParam String updateBy,@RequestParam String measureType) {
        return timeSweepService.queryProjectTaskMethod(updateBy,measureType);
    }

    @GetMapping("queryProjectAllMethods")
    @ApiOperation("查询项目下的所有方法")
    public Result queryProjectAllMethods(@RequestParam String projectId, @RequestParam String measureForm) {
        return timeSweepService.queryProjectAllMethods(projectId,measureForm);
    }

    @GetMapping("sampleListPort")
    @ApiOperation("获取样本列表接口")
    public Result sampleListPort(@RequestParam String taskId,@RequestParam(required = false) String type) {
        return timeSweepService.sampleListPort(taskId,type);
    }

    @DeleteMapping("deleteSampleListPort")
    @ApiOperation("删除样品列表接口")
    public Result deleteSampleListPort(@RequestParam String measureId) {
        return timeSweepService.deleteSampleListPort(measureId);
    }

    @GetMapping("changeTheListPort")
    @ApiOperation("获取变换列表接口")
    public Result changeTheListPort(@RequestParam String taskId,@RequestParam(required = false) String type) {
        return timeSweepService.changeTheListPort(taskId,type);
    }

    @DeleteMapping("deleteChangeListPort")
    @ApiOperation("删除变换列表接口")
    public Result deleteChangeListPort(@RequestParam String analysisId) {
        return timeSweepService.deleteChangeListPort(analysisId);
    }

    @GetMapping("setMethodParameters")
    @ApiOperation("设置方法参数接口")
    public Result setMethodParameters(@RequestParam String methodId,@RequestParam String deviceSerialNumber) {
        return timeSweepService.setMethodParameters(methodId,deviceSerialNumber);
    }

    @GetMapping("zero")
    @ApiOperation("校零接口")
    public Result photoZero(@RequestParam String serialNumber,
                            @RequestParam String methodId) {
        return timeSweepService.zero(serialNumber,methodId);
    }


}
