package com.persee.workstation.uv_inexus.controller.measurementController;

import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.ExperimentTestService;
import com.persee.workstation.uv_inexus.service.measurementService.SpectrumService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/spectrumScanning/")
@ApiModel("光谱扫描")
public class SpectrumController {

    @Autowired
    private SpectrumService spectrumService;

    @Autowired
    private ExperimentTestService experimentTestService;


    @GetMapping("queryEssentialInformation")
    @ApiOperation("查询项目任务方法接口")
    public Result queryProjectTaskMethod(@RequestParam String updateBy) {
        return spectrumService.queryProjectTaskMethod(updateBy);
    }

    @PostMapping("queryProjectAllMethods")
    @ApiOperation("查询项目下的所有方法")
    public Result queryProjectAllMethods(@RequestParam String projectId, @RequestParam String measureForm) {
        return spectrumService.queryProjectAllMethods(projectId,measureForm);
    }

    @GetMapping("methodParameter/{method_id}")
    @ApiOperation("查询方法参数接口")
    public Result queryMethodParameter(@PathVariable String method_id) {
        return spectrumService.queryMethodParameter(method_id);
    }

    @PostMapping("saveMethodParameter")
    @ApiOperation("方法参数修改接口")
    public Result saveMethodParameter(@RequestBody SpectralMethodParameterDto methodDto) {
        return spectrumService.updateMethodParameter(methodDto);
    }


    @GetMapping("queryMeasuredData")
    @ApiOperation("查询测量数据列表接口")
    public Result queryMeasuredData(@RequestParam Integer pageNum,
                                    @RequestParam Integer pageSize,
                                    @RequestParam String taskId) {
        QueryMeasuredDataVo dataVo = new QueryMeasuredDataVo();
        dataVo.setPageNum(pageNum);
        dataVo.setPageSize(pageSize);
        dataVo.setTaskId(taskId);
        return spectrumService.queryMeasuredData(dataVo);
    }

    @GetMapping("obtainReportParameters")
    @ApiOperation("获取报告参数接口")
    public Result obtainReportParameters(@RequestParam String methodId,@RequestParam String updateBy) {
        return spectrumService.obtainReportParameters(methodId,updateBy);
    }


    @GetMapping("queryMeasurementParameters")
    @ApiOperation("查询测量参数接口")
    public Result queryMeasurementParameters(@RequestParam String taskId) {
        return spectrumService.queryMeasurementParameters(taskId);
    }

    @PostMapping("fourOperationsPort")
    @ApiOperation("四则运算接口")
    public Result fourOperationsPort(@RequestBody FourOperationsParamDto paramDto){
        return spectrumService.fourOperationsPort(paramDto);
    }

    @PostMapping("advancedOperationsPort")
    @ApiOperation("高级运算接口")
    public Result advancedOperationsPort(@RequestBody AdvancedOperationsDto paramDto){
        return spectrumService.advancedOperationsPort(paramDto);
    }

    @PostMapping("luminanceModeConversionPort")
    @ApiOperation("光度模式转换")
    public Result luminanceModeConversionPort(@RequestBody LuminosityConversionDto paramDto){
        return spectrumService.luminanceModeConversionPort(paramDto);
    }

    @GetMapping("sampleListPort")
    @ApiOperation("获取样本列表接口")
    public Result sampleListPort(@RequestParam String taskId) {
        return spectrumService.sampleListPort(taskId);
    }

    @GetMapping("changeTheListPort")
    @ApiOperation("获取变换列表接口")
    public Result changeTheListPort(@RequestParam String taskId) {
        return spectrumService.changeTheListPort(taskId);
    }

    @GetMapping("testPythonProt")
    @ApiOperation("测试python接口")
    public void testPythonProt() {
        spectrumService.testPythonProt();
    }



}
