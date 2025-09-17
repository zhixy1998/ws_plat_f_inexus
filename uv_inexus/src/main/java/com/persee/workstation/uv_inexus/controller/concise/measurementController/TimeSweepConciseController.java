package com.persee.workstation.uv_inexus.controller.concise.measurementController;

import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.dto.concise.timesweep.TimeUseMethodDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.concise.measurementService.TimeSweepConciseService;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("timeSweepConciseController")
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/timeSweep/concise/")
@ApiModel("简洁模式: 时间扫描")
public class TimeSweepConciseController {

    @Autowired
    private TimeSweepConciseService timeSweepConciseService;

    @GetMapping("queryNearestMethodData")
    @ApiOperation("查询最近方法数据")
    public Result queryNearestMethodData(@RequestParam String updateBy, @RequestParam String measureType) {
        return timeSweepConciseService.queryNearestMethodData(updateBy,measureType);
    }

    @GetMapping("searchMethodButton")
    @ApiOperation("查找方法按钮")
    public Result searchMethodButton(@RequestParam Integer pageNum,
                                     @RequestParam Integer pageSize, @RequestParam String updateBy,
                                     @RequestParam String measureType, @RequestParam(required = false) String methodName) {
        return timeSweepConciseService.searchMethodButton(pageNum,pageSize,updateBy,measureType,methodName);
    }


    @RequestMapping("applyMethod")
    @ApiOperation("应用接口")
    public Result applyMethod(@RequestBody TimeUseMethodDto paramDto) {
        return timeSweepConciseService.use(paramDto);
    }

    @GetMapping("whetherSaveData")
    @ApiOperation("是否保存数据接口")
    public Result whetherSaveData(@RequestParam String taskId,@RequestParam Boolean isSave) {
        return timeSweepConciseService.whetherSaveData(taskId,isSave);
    }

    @GetMapping("createDraftButton")
    @ApiOperation("创建草稿按钮接口")
    public Result createDraftButton(@RequestParam String methodId,@RequestParam String updateBy) {
        return timeSweepConciseService.createDraftButton(methodId,updateBy);
    }

    @GetMapping("querySampleList")
    @ApiOperation("获取样本按钮列表接口")
    public Result querySampleList(@RequestParam String taskId) {
        return timeSweepConciseService.querySampleListButton(taskId);
    }

    @GetMapping("querySampleHeads")
    @ApiOperation("获取样本输入表头接口")
    public Result<List<FHeadVO>> querySampleHeads(@RequestParam String taskId) {
        return timeSweepConciseService.querySampleHeads(taskId);
    }

    @PostMapping("addOrDeleteSamplePort")
    @ApiOperation("添加或删除样本接口")
    public Result addOrDeleteSamplePort(@RequestBody FAddOrDeleteSampleDTO paramDto) {
        return timeSweepConciseService.addOrDeleteSamplePort(paramDto);
    }

    @PostMapping("detectionTaskStatus")
    @ApiOperation("检测任务状态接口")
    public Result detectionTaskStatus(@RequestParam String taskId) {
        return timeSweepConciseService.detectionTaskStatus(taskId);
    }

    @PostMapping("saveDraftButton")
    @ApiOperation("保存草稿按钮接口")
    public Result saveDraftButton(@RequestParam String taskId,
                                  @RequestParam(required = false) String taskName) {
        return timeSweepConciseService.saveDraftButton(taskId,taskName);
    }

    @GetMapping("querySampleDataList")
    @ApiOperation("查询样品数据列表")
    public Result queryNearestSampleData(@RequestParam String taskId,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) Integer digit) {
        return timeSweepConciseService.querySampleDataList(taskId,type,digit);
    }

    @GetMapping("deleteSampleListPort")
    @ApiOperation("删除样品列表接口")
    public Result deleteSampleListPort(@RequestParam String taskId,@RequestParam String sampleId,@RequestParam String analysisId) {
        return timeSweepConciseService.deleteSampleListPort(taskId,sampleId,analysisId);
    }

    @GetMapping("queryAnalysisDataList")
    @ApiOperation("查询分析数据列表")
    public Result queryNearestMeasureData(@RequestParam String taskId,
                                          @RequestParam(required = false) String type,
                                          @RequestParam(required = false) Integer digit) {
        return timeSweepConciseService.queryAnalysisDataList(taskId,type, digit);
    }

    @GetMapping("deleteChangeListPort")
    @ApiOperation("删除变换列表接口")
    public Result deleteChangeListPort(@RequestParam String analysisId) {
        return timeSweepConciseService.deleteChangeListPort(analysisId);
    }

    @GetMapping("restoreDeletedListData")
    @ApiOperation("恢复已删除数据接口")
    public Result restoreDeletedListData(@RequestParam String taskId,@RequestParam String sampleId,@RequestParam String type,@RequestParam String analysisId){
        return timeSweepConciseService.restoreDeletedListData(taskId,sampleId,type,analysisId);
    }

    @PostMapping("updateDataColor")
    @ApiOperation("更新数据颜色接口")
    public Result updateDataColor(@RequestBody FUpdateDataColorDto paramDto){
        return timeSweepConciseService.updateDataColor(paramDto);
    }

    @GetMapping("queryGaugePoint")
    @ApiOperation("获取标记点接口")
    public Result queryAnalysisGaugePoint(@RequestParam String dataId,
                                          @RequestParam(required = false) Integer digit) {
        return timeSweepConciseService.queryGaugePoint(dataId, digit);
    }

    @PostMapping("addGaugePointPort")
    @ApiOperation("添加标记点接口")
    public Result addAnalysisDataGaugePointPort(@RequestBody FAddGaugePointDto paramDto){
        return timeSweepConciseService.addGaugePointPort(paramDto);
    }

    @PostMapping("addGaugePointRemarks")
    @ApiOperation("添加标记点备注接口")
    public Result addGaugePointRemarks(@RequestBody FPeakValleyMarkingDto paramDto){
        return timeSweepConciseService.addGaugePointRemarks(paramDto);
    }

    @GetMapping("queryPeakAndValleysPort")
    @ApiOperation("获取峰谷接口")
    public Result queryPeakAndValleysPort(@RequestParam String dataId,
                                          @RequestParam(required = false) Integer digit) {
        return timeSweepConciseService.queryPeakAndValleysPort(dataId, digit);
    }

    @PostMapping("seekingPeaksAndValleysPort")
    @ApiOperation("寻峰寻谷接口")
    public Result seekingPeaksAndValleysPort(@RequestBody FSeekingPeaksValleysDto paramDto){
        return timeSweepConciseService.peakSearchingPort(paramDto);
    }

    @PostMapping("addPeakValleyRemarks")
    @ApiOperation("添加峰谷备注接口")
    public Result addPeakValleyRemarks(@RequestBody FPeakValleyMarkingDto paramDto){
        return timeSweepConciseService.addPeakValleyRemarks(paramDto);
    }

    @GetMapping("setUpParametersPort")
    @ApiOperation("设置参数接口")
    public Result setUpParametersPort(@RequestParam String methodId,@RequestParam String deviceSerialNumber) {
        return timeSweepConciseService.setUpParametersPort(methodId,deviceSerialNumber);
    }

    @GetMapping("queryHistoryMethod")
    @ApiOperation("查询历史方法接口")
    public Result queryHistoryMethod(@RequestParam Integer pageNum,
                                     @RequestParam Integer pageSize,
                                     @RequestParam String updateBy,
                                     @RequestParam String measureType,
                                     @RequestParam(required = false) String methodName) {
        return timeSweepConciseService.queryHistoryMethod(pageNum,pageSize,updateBy,measureType,methodName);
    }


    @GetMapping("queryHistoryTask")
    @ApiOperation("查询历史任务接口")
    public Result queryHistoryTask(@RequestParam Integer pageNum,
                                   @RequestParam Integer pageSize,
                                   @RequestParam String updateBy,
                                   @RequestParam String measureType,
                                   @RequestParam(required = false) String taskName) {
        return timeSweepConciseService.queryHistoryTask(pageNum, pageSize, updateBy, measureType, taskName);
    }

    @GetMapping("queryArithmeticSourceData")
    @ApiOperation("查询运算源数据接口")
    public Result queryArithmeticSourceData(@RequestParam String taskId) {
        return timeSweepConciseService.queryArithmeticSourceData(taskId);
    }

    @PostMapping("fourOperationsPort")
    @ApiOperation("四则运算接口")
    public Result fourOperationsPort(@RequestBody FFourOperationsParamDto paramDto){
        return timeSweepConciseService.fourOperationsPort(paramDto);
    }


    @PostMapping("advancedOperationsPort")
    @ApiOperation("高级运算接口")
    public Result advancedOperationsPort(@RequestBody FAdvancedOperationsDto paramDto){
        return timeSweepConciseService.advancedOperationsPort(paramDto);
    }

    @PostMapping("smoothnessPort")
    @ApiOperation("平滑接口")
    public Result smoothnessPort(@RequestBody FSmoothnessDto paramDto){
        return timeSweepConciseService.smoothnessPort(paramDto);
    }

    @PostMapping("differentialPort")
    @ApiOperation("微分接口")
    public Result differentialPort(@RequestBody FDifferentialDto paramDto){
        return timeSweepConciseService.differentialPort(paramDto);
    }

    @PostMapping("luminanceModeConversionPort")
    @ApiOperation("光度模式转换")
    public Result luminanceModeConversionPort(@RequestBody FLuminosityConversionDto paramDto){
        return timeSweepConciseService.luminanceModeConversionPort(paramDto);
    }

}
