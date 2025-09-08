package com.persee.workstation.uv_inexus.controller.guide.measurementController;

import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TimeSweepService;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

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

    @GetMapping("queryMethodParameter")
    @ApiOperation("查询方法参数接口")
    public Result queryMethodParameter(@RequestParam String methodId) {
        return timeSweepService.queryMethodParameter(methodId);
    }

    @PostMapping("updateMethodParameter")
    @ApiOperation("修改方法参数接口")
    public Result updateMethodParameter(@RequestBody TimeSweepMethodParameterDto params) {
        return timeSweepService.updateMethodParameter(params);
    }

    @GetMapping("querySampleHeads")
    @ApiOperation("获取样本输入表头接口")
    public Result<List<HeadVO>> querySampleHeads(@RequestParam String taskId) {
        return timeSweepService.querySampleHeads(taskId);
    }

    @GetMapping("sampleListPort")
    @ApiOperation("获取样本列表接口")
    public Result sampleListPort(@RequestParam String taskId,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(required = false) Integer digit) {
        return timeSweepService.sampleListPort(taskId,type, digit);
    }

    @DeleteMapping("deleteSampleListPort")
    @ApiOperation("删除样品列表接口")
    public Result deleteSampleListPort(@RequestParam String sampleId,@RequestParam String taskId) {
        return timeSweepService.deleteSampleListPort(sampleId,taskId);
    }

    @GetMapping("changeTheListPort")
    @ApiOperation("获取变换列表接口")
    public Result changeTheListPort(@RequestParam String taskId,
                                    @RequestParam(required = false) String type,
                                    @RequestParam(required = false) Integer digit) {
        return timeSweepService.changeTheListPort(taskId,type, digit);
    }

    @DeleteMapping("deleteChangeListPort")
    @ApiOperation("删除变换列表接口")
    public Result deleteChangeListPort(@RequestParam String analysisId) {
        return timeSweepService.deleteChangeListPort(analysisId);
    }

    @GetMapping("restoreDeletedListData")
    @ApiOperation("恢复已删除数据接口")
    public Result restoreDeletedListData(@RequestParam String taskId,@RequestParam String paramId,@RequestParam String type){
        return timeSweepService.restoreDeletedListData(taskId,paramId,type);
    }

    @GetMapping("setMethodParameters")
    @ApiOperation("设置方法参数接口")
    public Result setMethodParameters(@RequestParam String methodId,@RequestParam String deviceSerialNumber) {
        return timeSweepService.setMethodParameters(methodId,deviceSerialNumber);
    }

    @GetMapping("zero")
    @ApiOperation("校零接口")
    public Result photoZero(@RequestParam(required = false) String taskId,
                            @RequestParam String serialNumber,
                            @RequestParam String methodId) {
        return timeSweepService.zero(taskId,serialNumber,methodId);
    }

    @GetMapping("querySampleList")
    @ApiOperation("获取样本按钮列表接口")
    public Result querySampleList(@RequestParam String taskId) {
        return timeSweepService.querySampleList(taskId);
    }

    @PostMapping("addOrDeleteSamplePort")
    @ApiOperation("添加或删除样本接口")
    public Result addOrDeleteSamplePort(@RequestBody AddOrDeleteSampleDTO paramDto){
        return timeSweepService.addOrDeleteSamplePort(paramDto);
    }

    @PostMapping("uploadImage")
    @ApiOperation("图片上传")
    public Result uploadImage(@RequestBody SpectrumImageUpload upload) {
        return timeSweepService.uploadImage(upload);
    }

    @GetMapping("queryPeakValleyDataList")
    @ApiOperation("查询峰谷数据列表接口")
    public Result queryPeakValleyDataList(@RequestParam String dataId,
                                          @RequestParam(required = false) Integer digit) {
        return timeSweepService.queryPeakValleyDataList(dataId, digit);
    }

    @PostMapping("seekingPeaksAndValleysPort")
    @ApiOperation("寻峰寻谷接口")
    public Result seekingPeaksAndValleysPort(@RequestBody SeekingPeaksValleysDto paramDto){
        return timeSweepService.seekingPeaksAndValleysPort(paramDto);
    }

    @PostMapping("addPeakValleyRemarks")
    @ApiOperation("添加峰谷备注接口")
    public Result addPeakValleyRemarks(@RequestBody PeakValleyMarkingDto paramDto){
        return timeSweepService.addPeakValleyRemarks(paramDto);
    }

    @GetMapping("queryGaugePointDataList")
    @ApiOperation("获取标记点数据列表")
    public Result queryGaugePointDataList(@RequestParam String dataId,
                                      @RequestParam(required = false) Integer digit) {
        return timeSweepService.queryGaugePointDataList(dataId, digit);
    }

    @PostMapping("addGaugePoint")
    @ApiOperation("添加标记点接口")
    public Result addGaugePoint(@RequestBody AddGaugePointDto paramDto){
        return timeSweepService.addGaugePoint(paramDto);
    }

    @PostMapping("addGaugePointRemarks")
    @ApiOperation("添加标记点备注接口")
    public Result addGaugePointRemarks(@RequestBody PeakValleyMarkingDto paramDto){
        return timeSweepService.addGaugePointRemarks(paramDto);
    }

    @PostMapping("updateDataColor")
    @ApiOperation("更新数据颜色接口")
    public Result updateDataColor(@RequestBody UpdateDataColorDto paramDto){
        return timeSweepService.updateDataColor(paramDto);
    }

    @PostMapping("gainStatementData")
    @ApiOperation("获取报表内容数据")
    public Result gainStatementData(@RequestParam String taskId) {
        return timeSweepService.gainStatementData(taskId);
    }


    @GetMapping("exportWord")
    @ApiOperation("导出Word接口")
    public void exportWord(@RequestParam String taskId, HttpServletResponse response) {
        try {
            timeSweepService.exportToWord(taskId, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("导出失败: " + e.getMessage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    @GetMapping("queryArithmeticSourceData")
    @ApiOperation("查询运算源数据接口")
    public Result queryArithmeticSourceData(@RequestParam String taskId) {
        return timeSweepService.queryArithmeticSourceData(taskId);
    }

    @PostMapping("fourOperationsPort")
    @ApiOperation("四则运算接口")
    public Result fourOperationsPort(@RequestBody FourOperationsParamDto paramDto){
        return timeSweepService.fourOperationsPort(paramDto);
    }


    @PostMapping("advancedOperationsPort")
    @ApiOperation("高级运算接口")
    public Result advancedOperationsPort(@RequestBody AdvancedOperationsDto paramDto){
        return timeSweepService.advancedOperationsPort(paramDto);
    }

    @PostMapping("smoothnessPort")
    @ApiOperation("平滑接口")
    public Result smoothnessPort(@RequestBody SmoothnessDto paramDto){
        return timeSweepService.smoothnessPort(paramDto);
    }

    @PostMapping("differentialPort")
    @ApiOperation("微分接口")
    public Result differentialPort(@RequestBody DifferentialDto paramDto){
        return timeSweepService.differentialPort(paramDto);
    }

    @PostMapping("luminanceModeConversionPort")
    @ApiOperation("光度模式转换")
    public Result luminanceModeConversionPort(@RequestBody LuminosityConversionDto paramDto){
        return timeSweepService.luminanceModeConversionPort(paramDto);
    }

    @GetMapping("saveData")
    @ApiOperation("保存数据接口")
    public Result saveData(@RequestParam String taskId) {
        return timeSweepService.saveData(taskId);
    }





}
