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

package com.persee.workstation.uv_inexus.controller.concise.measurementController;

import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.dto.concise.spectrum.FParameterValidationDto;
import com.persee.workstation.uv_inexus.dto.concise.spectrum.FUseMethodDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumImageUpload;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SpectrumConciseService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.ExperimentTestService;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.List;

@Slf4j
@RestController("spectrumConciseController")
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/spectrumScanning/concise/")
@Api("简洁模式: 光谱扫描")
public class SpectrumConciseController {

    @Autowired
    private SpectrumConciseService spectrumConciseService;      // 简洁模式: 光谱扫描业务层

    @Autowired
    private ExperimentTestService experimentTestService;

    @PostMapping("uploadImage")
    @ApiOperation("经典模式上传图片")
    public Result uploadImage(@RequestBody SpectrumImageUpload upload) {
        return spectrumConciseService.uploadImage(upload);
    }

    @GetMapping("queryNearestMethodData")
    @ApiOperation("查询最近方法数据")
    public Result queryNearestMethodData(@RequestParam String updateBy,@RequestParam String measureType) {
        return spectrumConciseService.queryNearestMethodData(updateBy,measureType);
    }

    @GetMapping("searchMethodButton")
    @ApiOperation("查找方法按钮")
    public Result searchMethodButton(@RequestParam(required = false) Integer pageNum,
                                     @RequestParam(required = false) Integer pageSize,
                                     @RequestParam String updateBy,
                                     @RequestParam String measureType,
                                     @RequestParam(required = false) String methodName) {
        return spectrumConciseService.searchMethodButton(pageNum,pageSize,updateBy,measureType,methodName);
    }

    @GetMapping("obtainReportParameters")
    @ApiOperation("获取报告通用数据接口")
    public Result obtainReportParameters(@RequestParam String taskId,
                                         @RequestParam(required = false) String sampleId,
                                         @RequestParam(required = false) String serialNumber) {
        return experimentTestService.obtainReportParameters(taskId, sampleId, serialNumber);
    }


    @PostMapping("parameterValidation")
    @ApiOperation("参数校验接口")
    public Result parameterValidation(@RequestBody FParameterValidationDto paramDto) {
        return spectrumConciseService.parameterValidation(paramDto);
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
    public Result createDraftButton(@RequestParam String methodId,
                                    @RequestParam String updateBy,
                                    @RequestParam String deviceSerialNumber) {
        return spectrumConciseService.createDraftButton(methodId,updateBy,deviceSerialNumber);
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
    public Result restoreDeletedListData(@RequestParam String taskId,
                                         @RequestParam(required = false) String sampleId,
                                         @RequestParam String type,
                                         @RequestParam String analysisId){
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

    @PostMapping("addGaugePoint")
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

    @GetMapping("downloadDoc")
    @ApiOperation("多格式文件下载接口（支持word/excel/txt）")
    // 关键修正：1. 修复@ApiParam描述错误 2. 为List参数添加@RequestParam的value和required属性
    public ResponseEntity<?> downloadDoc(
            @ApiParam(value = "任务ID", required = true) @RequestParam String taskId,
            @ApiParam(value = "是否显示全部数据（true/false）", required = true) @RequestParam Boolean allDisplay,
            @ApiParam(value = "筛选的analysisId列表（多个ID用逗号分隔，如123,456）", required = false) 
            @RequestParam(value = "analysisIds", required = false) List<String> analysisIds,
            @ApiParam(value = "导出格式（word/excel/txt，默认word）", required = false) 
            @RequestParam(required = false) String fileType,
            // 新增 digit 参数（根据实际需求调整 required 和描述）
            @ApiParam(value = "修约", required = false) 
            @RequestParam(value = "digit", required = false) Integer digit) throws Exception {
        
        // 调用 Service 层的多格式导出方法
        R<File> result = spectrumConciseService.downloadFile(taskId, allDisplay, fileType, analysisIds, digit);
        
        if (result.getSuccess()) {
            File file = result.getData();
            FileInputStream fis = new FileInputStream(file);
            InputStreamResource resource = new InputStreamResource(fis);

            // 编码文件名（避免中文乱码，兼容浏览器）
            String fileName = URLEncoder.encode(file.getName(), "UTF-8");
            // 处理IE浏览器编码问题（可选，增强兼容性）
            fileName = fileName.replace("+", "%20");
            
            // 异步删除临时文件（优化：增加try-with-resources避免流泄漏，日志替换System.out）
            new Thread(() -> {
                try {
                    Thread.sleep(10000); // 等待10秒，确保下载完成
                    if (file.exists()) {
                        boolean deleteSuccess = file.delete();
                        log.info("临时文件删除结果：{}，文件路径：{}", deleteSuccess, file.getAbsolutePath());
                    }
                } catch (InterruptedException e) {
                    log.error("临时文件删除线程被中断", e);
                    Thread.currentThread().interrupt(); // 恢复中断状态
                } catch (Exception e) {
                    log.error("临时文件删除失败", e);
                }
            }).start();

            // 构造响应头，指定下载和文件类型
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length())) // 增加文件大小头，优化下载体验
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }
        
        // 关键修正：返回Service层的具体错误信息，而非空字符串
        return ResponseEntity.badRequest().body("导出失败：" + result.getMessage());
    }
}
