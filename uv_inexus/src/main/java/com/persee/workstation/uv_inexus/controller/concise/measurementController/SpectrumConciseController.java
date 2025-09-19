package com.persee.workstation.uv_inexus.controller.concise.measurementController;

import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.dto.concise.spectrum.FUseMethodDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SpectrumConciseService;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("spectrumConciseController")
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/spectrumScanning/concise/")
@Api("简洁模式: 光谱扫描")
public class SpectrumConciseController {

    @Autowired
    private SpectrumConciseService spectrumConciseService;      // 简洁模式: 光谱扫描业务层


    @GetMapping("queryNearestMethodData")
    @ApiOperation("查询最近方法数据")
    public Result queryNearestMethodData(@RequestParam String updateBy,@RequestParam String measureType) {
        return spectrumConciseService.queryNearestMethodData(updateBy,measureType);
    }

    @GetMapping("searchMethodButton")
    @ApiOperation("查找方法按钮")
    public Result searchMethodButton(@RequestParam Integer pageNum,
                                     @RequestParam Integer pageSize, @RequestParam String updateBy,
                                     @RequestParam String measureType, @RequestParam(required = false) String methodName) {
        return spectrumConciseService.searchMethodButton(pageNum,pageSize,updateBy,measureType,methodName);
    }


    @PostMapping("applyMethod")
    @ApiOperation("应用接口")
    public Result applyMethod(@RequestBody FUseMethodDto paramDto) {
        return spectrumConciseService.use(paramDto);
    }


    @GetMapping("whetherSaveData")
    @ApiOperation("是否保存数据接口")
    public Result whetherSaveData(@RequestParam String taskId,@RequestParam Boolean isSave) {
        return spectrumConciseService.whetherSaveData(taskId,isSave);
    }

    @GetMapping("createDraftButton")
    @ApiOperation("创建草稿按钮接口")
    public Result createDraftButton(@RequestParam String methodId,@RequestParam String updateBy) {
        return spectrumConciseService.createDraftButton(methodId,updateBy);
    }


    @GetMapping("querySampleList")
    @ApiOperation("获取样本按钮列表接口")
    public Result querySampleList(@RequestParam String taskId) {
        return spectrumConciseService.querySampleListButton(taskId);
    }

    @GetMapping("querySampleHeads")
    @ApiOperation("获取样本输入表头接口")
    public Result<List<FHeadVO>> querySampleHeads(@RequestParam String taskId) {
        return spectrumConciseService.querySampleHeads(taskId);
    }

    @PostMapping("addOrDeleteSamplePort")
    @ApiOperation("添加或删除样本接口")
    public Result addOrDeleteSamplePort(@RequestBody FAddOrDeleteSampleDTO paramDto) {
        return spectrumConciseService.addOrDeleteSamplePort(paramDto);
    }

    @PostMapping("detectionTaskStatus")
    @ApiOperation("检测任务状态接口")
    public Result detectionTaskStatus(@RequestParam String taskId) {
        return spectrumConciseService.detectionTaskStatus(taskId);
    }


    @PostMapping("saveDraftButton")
    @ApiOperation("保存草稿按钮接口")
    public Result saveDraftButton(@RequestParam String taskId,
                                  @RequestParam(required = false) String taskName) {
        return spectrumConciseService.saveDraftButton(taskId,taskName);
    }


    @GetMapping("sampleListPort")
    @ApiOperation("查询样品数据列表")
    public Result queryNearestSampleData(@RequestParam String taskId,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) Integer digit) {
        return spectrumConciseService.querySampleDataList(taskId,type,digit);
    }

    @GetMapping("deleteSampleListPort")
    @ApiOperation("删除样品列表接口")
    public Result deleteSampleListPort(@RequestParam String taskId,@RequestParam String sampleId,@RequestParam String analysisId) {
        return spectrumConciseService.deleteSampleListPort(taskId,sampleId,analysisId);
    }

    @GetMapping("changeTheListPort")
    @ApiOperation("查询分析数据列表")
    public Result queryNearestMeasureData(@RequestParam String taskId,
                                          @RequestParam(required = false) String type,
                                          @RequestParam(required = false) Integer digit) {
        return spectrumConciseService.queryAnalysisDataList(taskId,type, digit);
    }

    @GetMapping("deleteChangeListPort")
    @ApiOperation("删除变换列表接口")
    public Result deleteChangeListPort(@RequestParam String analysisId) {
        return spectrumConciseService.deleteChangeListPort(analysisId);
    }

    @GetMapping("restoreDeletedListData")
    @ApiOperation("恢复已删除数据接口")
    public Result restoreDeletedListData(@RequestParam String taskId,@RequestParam String sampleId,@RequestParam String type,@RequestParam String analysisId){
        return spectrumConciseService.restoreDeletedListData(taskId,sampleId,type,analysisId);
    }

    @PostMapping("updateDataColor")
    @ApiOperation("更新数据颜色接口")
    public Result updateDataColor(@RequestBody FUpdateDataColorDto paramDto){
        return spectrumConciseService.updateDataColor(paramDto);
    }

    @GetMapping("queryGaugePoint")
    @ApiOperation("获取标记点接口")
    public Result queryAnalysisGaugePoint(@RequestParam String dataId,
                                          @RequestParam(required = false) Integer digit) {
        return spectrumConciseService.queryGaugePoint(dataId, digit);
    }

    @PostMapping("addGaugePointPort")
    @ApiOperation("添加标记点接口")
    public Result addAnalysisDataGaugePointPort(@RequestBody FAddGaugePointDto paramDto){
        return spectrumConciseService.addGaugePointPort(paramDto);
    }

    @PostMapping("addGaugePointRemarks")
    @ApiOperation("添加标记点备注接口")
    public Result addGaugePointRemarks(@RequestBody FPeakValleyMarkingDto paramDto){
        return spectrumConciseService.addGaugePointRemarks(paramDto);
    }

    @GetMapping("queryPeakAndValleysPort")
    @ApiOperation("获取峰谷接口")
    public Result queryPeakAndValleysPort(@RequestParam String dataId,
                                          @RequestParam(required = false) Integer digit) {
        return spectrumConciseService.queryPeakAndValleysPort(dataId, digit);
    }

    @PostMapping("seekingPeaksAndValleysPort")
    @ApiOperation("寻峰寻谷接口")
    public Result seekingPeaksAndValleysPort(@RequestBody FSeekingPeaksValleysDto paramDto){
        return spectrumConciseService.peakSearchingPort(paramDto);
    }

    @PostMapping("addPeakValleyRemarks")
    @ApiOperation("添加峰谷备注接口")
    public Result addPeakValleyRemarks(@RequestBody FPeakValleyMarkingDto paramDto){
        return spectrumConciseService.addPeakValleyRemarks(paramDto);
    }

    @GetMapping("setUpParametersPort")
    @ApiOperation("设置参数接口")
    public Result setUpParametersPort(@RequestParam String methodId,@RequestParam String deviceSerialNumber) {
        return spectrumConciseService.setUpParametersPort(methodId,deviceSerialNumber);
    }

    @GetMapping("queryHistoryMethod")
    @ApiOperation("查询历史方法接口")
    public Result queryHistoryMethod(@RequestParam Integer pageNum,
                                     @RequestParam Integer pageSize,
                                     @RequestParam String updateBy,
                                     @RequestParam String measureType,
                                     @RequestParam(required = false) String methodName) {
        return spectrumConciseService.queryHistoryMethod(pageNum,pageSize,updateBy,measureType,methodName);
    }


    @GetMapping("queryHistoryTask")
    @ApiOperation("查询历史任务接口")
    public Result queryHistoryTask(@RequestParam Integer pageNum,
                                   @RequestParam Integer pageSize,
                                   @RequestParam String updateBy,
                                   @RequestParam String measureType,
                                   @RequestParam(required = false) String taskName) {
        return spectrumConciseService.queryHistoryTask(pageNum, pageSize, updateBy, measureType, taskName);
    }

    @GetMapping("queryArithmeticSourceData")
    @ApiOperation("查询运算源数据接口")
    public Result queryArithmeticSourceData(@RequestParam String taskId) {
        return spectrumConciseService.queryArithmeticSourceData(taskId);
    }

    @PostMapping("fourOperationsPort")
    @ApiOperation("四则运算接口")
    public Result fourOperationsPort(@RequestBody FFourOperationsParamDto paramDto){
        return spectrumConciseService.fourOperationsPort(paramDto);
    }

    @PostMapping("advancedOperationsPort")
    @ApiOperation("高级运算接口")
    public Result advancedOperationsPort(@RequestBody FAdvancedOperationsDto paramDto){
        return spectrumConciseService.advancedOperationsPort(paramDto);
    }

    @PostMapping("smoothnessPort")
    @ApiOperation("平滑接口")
    public Result smoothnessPort(@RequestBody FSmoothnessDto paramDto){
        return spectrumConciseService.smoothnessPort(paramDto);
    }

    @PostMapping("differentialPort")
    @ApiOperation("微分接口")
    public Result differentialPort(@RequestBody FDifferentialDto paramDto){
        return spectrumConciseService.differentialPort(paramDto);
    }

    @PostMapping("luminanceModeConversionPort")
    @ApiOperation("光度模式转换")
    public Result luminanceModeConversionPort(@RequestBody FLuminosityConversionDto paramDto){
        return spectrumConciseService.luminanceModeConversionPort(paramDto);
    }

}
