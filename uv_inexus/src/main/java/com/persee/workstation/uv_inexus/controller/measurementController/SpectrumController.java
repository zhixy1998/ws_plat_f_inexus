package com.persee.workstation.uv_inexus.controller.measurementController;

import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.ExperimentTestService;
import com.persee.workstation.uv_inexus.service.measurementService.SpectrumService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
    public Result queryProjectTaskMethod(@RequestParam String updateBy,@RequestParam String measureType) {
        return spectrumService.queryProjectTaskMethod(updateBy,measureType);
    }

    @GetMapping("queryProjectAllMethods")
    @ApiOperation("查询项目下的所有方法")
    public Result queryProjectAllMethods(@RequestParam String projectId, @RequestParam String measureForm) {
        return spectrumService.queryProjectAllMethods(projectId,measureForm);
    }

    @GetMapping("methodParameter")
    @ApiOperation("查询方法参数接口")
    public Result queryMethodParameter(@RequestParam String methodId) {
        return spectrumService.queryMethodParameter(methodId);
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
    public Result obtainReportParameters(@RequestParam String taskId) {
        return spectrumService.obtainReportParameters(taskId);
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

    @PostMapping("smoothnessPort")
    @ApiOperation("平滑接口")
    public Result smoothnessPort(@RequestBody SmoothnessDto paramDto){
        return spectrumService.smoothnessPort(paramDto);
    }

    @PostMapping("differentialPort")
    @ApiOperation("微分接口")
    public Result differentialPort(@RequestBody DifferentialDto paramDto){
        return spectrumService.differentialPort(paramDto);
    }

    @PostMapping("luminanceModeConversionPort")
    @ApiOperation("光度模式转换")
    public Result luminanceModeConversionPort(@RequestBody LuminosityConversionDto paramDto){
        return spectrumService.luminanceModeConversionPort(paramDto);
    }

    @GetMapping("sampleListPort")
    @ApiOperation("获取样本列表接口")
    public Result sampleListPort(@RequestParam String taskId,@RequestParam(required = false) String type) {
        return spectrumService.sampleListPort(taskId,type);
    }


    @GetMapping("deleteSampleListPort")
    @ApiOperation("删除样品列表接口")
    public Result deleteSampleListPort(@RequestParam String taskId,@RequestParam String sampleId) {
        return spectrumService.deleteSampleListPort(taskId,sampleId);
    }


    @GetMapping("changeTheListPort")
    @ApiOperation("获取变换列表接口")
    public Result changeTheListPort(@RequestParam String taskId,@RequestParam(required = false) String type) {
        return spectrumService.changeTheListPort(taskId,type);
    }

    @GetMapping("deleteChangeListPort")
    @ApiOperation("删除变换列表接口")
    public Result deleteChangeListPort(@RequestParam String analysisId) {
        return spectrumService.deleteChangeListPort(analysisId);
    }


    @GetMapping("restoreDeletedListData")
    @ApiOperation("恢复已删除数据接口")
    public Result restoreDeletedListData(@RequestParam String taskId,@RequestParam String paramId,@RequestParam String type){
        return spectrumService.restoreDeletedListData(taskId,paramId,type);
    }

    @GetMapping("queryAnalysisGaugePoint")
    @ApiOperation("获取分析标记点接口")
    public Result queryAnalysisGaugePoint(@RequestParam String analysisId) {
        return spectrumService.queryAnalysisGaugePoint(analysisId);
    }

    @PostMapping("addMeasureDataGaugePointPort")
    @ApiOperation("添加测量标记点接口")
    public Result addMeasureDataGaugePointPort(@RequestBody MeasureDataGaugePointDto paramDto){
        return spectrumService.addMeasureDataGaugePointPort(paramDto);
    }

    @PostMapping("addAnalysisDataGaugePointPort")
    @ApiOperation("添加分析标记点接口")
    public Result addAnalysisDataGaugePointPort(@RequestBody AnalysisDataGaugePointDto paramDto){
        return spectrumService.addAnalysisDataGaugePointPort(paramDto);
    }

    @GetMapping("queryPeakAndValleysPort")
    @ApiOperation("获取峰谷接口")
    public Result queryPeakAndValleysPort(@RequestParam String dataId,
                                          @RequestParam String type) {
        return spectrumService.queryPeakAndValleysPort(dataId,type);
    }
    @PostMapping("seekingPeaksAndValleysPort")
    @ApiOperation("寻峰寻谷接口")
    public Result seekingPeaksAndValleysPort(@RequestBody SeekingPeaksValleysDto paramDto){
        return spectrumService.peakSearchingPort(paramDto);
    }

    @GetMapping("queryGaugePointPort")
    @ApiOperation("获取标记点接口")
    public Result queryGaugePointPort(@RequestParam String dataId,
                                          @RequestParam String type) {
        return spectrumService.queryGaugePointPort(dataId,type);
    }

    @PostMapping("addGaugePoint")
    @ApiOperation("添加标记点接口")
    public Result addGaugePoint(@RequestBody AddGaugePointDto paramDto){
        return spectrumService.addGaugePoint(paramDto);
    }

    @GetMapping("testStart")
    @ApiOperation("开始测试")
    public void testStart() {
        spectrumService.testStart();
    }

    @GetMapping("testPythonProt")
    @ApiOperation("测试python接口")
    public void testPythonProt() {
        spectrumService.testPythonProt();
    }

    @GetMapping("setUpParametersPort")
    @ApiOperation("设置参数接口")
    public Result setUpParametersPort(@RequestParam String methodId,@RequestParam String deviceSerialNumber) {
        return spectrumService.setUpParametersPort(methodId,deviceSerialNumber);
    }

    @GetMapping("querySampleList")
    @ApiOperation("获取样本按钮列表接口")
    public Result querySampleList(@RequestParam String taskId) {
        return spectrumService.querySampleList(taskId);
    }

    @PostMapping("addOrDeleteSamplePort")
    @ApiOperation("添加或删除样本接口")
    public Result addOrDeleteSamplePort(@RequestBody AddOrDeleteSampleDTO paramDto){
        return spectrumService.addOrDeleteSamplePort(paramDto);
    }

    @DeleteMapping("deleteSamplePort")
    @ApiOperation("删除样本接口")
    public Result deleteSamplePort(@RequestParam String sampleId,@RequestParam String taskId){
        return spectrumService.deleteSamplePort(sampleId,taskId);
    }

    @PostMapping("uploadImage")
    @ApiOperation("图片上传")
    public Result uploadImage(@RequestBody SpectrumImageUpload upload) {
        return spectrumService.uploadImage(upload);
    }

    @PostMapping("queryTheTaskImage")
    @ApiOperation("获取任务图片接口")
    public Result queryTheTaskImage(@RequestParam String id,@RequestParam String type) {
        return spectrumService.queryTheTaskImage(id,type);
    }



    @GetMapping("testScan")
    @ApiOperation("测试扫描")
    public void testScan() {
        spectrumService.testScan();
    }



    @GetMapping("saveData")
    @ApiOperation("保存数据接口")
    public Result saveData(@RequestParam String taskId) {
        return spectrumService.saveData(taskId);
    }

    @GetMapping("queryPreprocessingOperation")
    @ApiOperation("查询预处理操作接口")
    public Result queryPreprocessingOperation(@RequestParam String methodId) {
        return spectrumService.queryPreprocessingOperation(methodId);
    }


    @GetMapping("obtainTheBaseline")
    @ApiOperation("获取基线接口")
    public Result obtainTheBaseline(@RequestParam(required = false) String taskId,@RequestParam String methodId,@RequestParam String deviceSerialNumber) {
        return spectrumService.obtainTheBaseline(taskId,methodId,deviceSerialNumber);
    }


    @GetMapping("/exportWord")
    @ApiOperation("导出Word接口")
    public void exportWord(@RequestParam String taskId, HttpServletResponse response) {
        try {
            spectrumService.exportToWord(taskId, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("导出失败: " + e.getMessage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }



}
