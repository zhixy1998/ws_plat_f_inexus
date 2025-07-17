package com.persee.workstation.uv_inexus.controller.measurementController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.persee.workstation.uv_inexus.dto.measurementDto.AddSampleDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.TimeSweepService;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/timeSweep/")
@ApiModel("时间扫描")
public class TimeSweepController {
    @Autowired
    private TimeSweepService timeSweepService;

    @GetMapping("queryEssentialInformation")
    @ApiOperation("查询项目任务方法接口")
    public Result queryProjectTaskMethod(@RequestParam String updateBy) {
        return timeSweepService.queryProjectTaskMethod(updateBy);
    }
   
    @PostMapping("updateMethodParameter")
    @ApiOperation("方法参数修改接口")
    public Result updateMethodParameter(@RequestBody MethodParamsDto methodDto) {
        return timeSweepService.updateMethodParameter(methodDto);
    }

    @GetMapping("methodParameter/{method_id}")
    @ApiOperation("查询方法参数接口")
    public Result queryMethodParameter(@PathVariable String method_id) {
        return timeSweepService.queryMethodParameter(method_id);
    }

    @GetMapping("sampleListPort")
    @ApiOperation("获取样本列表接口")
    public Result sampleListPort(@RequestParam String taskId,@RequestParam(required = false) String type) {
        return timeSweepService.sampleListPort(taskId,type);
    }

    @PostMapping("addSamplePort")
    @ApiOperation("添加样本接口")
    public Result addSamplePort(@RequestBody AddSampleDto paramDto){
        return timeSweepService.addSamplePort(paramDto);
    }

    @DeleteMapping("deleteSamplePort")
    @ApiOperation("删除样本接口")
    public Result deleteSamplePort(@RequestParam String sampleId,@RequestParam String taskId){
        return timeSweepService.deleteSamplePort(sampleId,taskId);
    }
    
}
