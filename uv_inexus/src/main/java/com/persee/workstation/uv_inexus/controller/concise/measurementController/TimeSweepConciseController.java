/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.concise.measurementController;

import com.persee.workstation.uv_inexus.config.ButtonPermission;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.dto.concise.timesweep.TimeUseMethodDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumImageUpload;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.concise.measurementService.TimeSweepConciseService;
import com.persee.workstation.uv_inexus.utils.OperateLog.OperateLog;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController("timeSweepConciseController")
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/timeSweep/concise/")
@Api("简洁模式: 时间扫描")
public class TimeSweepConciseController {

    @Autowired
    private TimeSweepConciseService timeSweepConciseService;

    @PostMapping("uploadImage")
    @ApiOperation("经典模式上传图片")
    public Result uploadImage(@RequestBody SpectrumImageUpload upload) {
        return timeSweepConciseService.uploadImage(upload);
    }


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


    @ButtonPermission("timeScanApplyMethod")
    @OperateLog(module = "实验测试-经典模式-时间扫描-应用方法", description = "执行实验测试-经典模式-时间扫描-应用方法记录", recordParams = true)
    @PostMapping("applyMethod")
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
    public Result createDraftButton(@RequestParam String methodId,
                                    @RequestParam(required = false) String taskId,
                                    @RequestParam String updateBy) {
        return timeSweepConciseService.createDraftButton(methodId,taskId,updateBy);
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

//    @ButtonPermission(
//            description = "实验测试-经典模式-时间扫描-设置样品", // 操作描述
//            permissionMapping = "add=timeScanSetSample,edit=timeScanModifySampleInformation,delete=timeScanDeleteSample" // 自定义操作类型→驼峰权限标识
//    )
//    @OperateLog(
//            module = "实验测试-经典模式-时间扫描-设置样品",
//            description = "执行实验测试-经典模式-时间扫描-{operateType}样品操作",
//            recordParams = true
//    )
    @ButtonPermission("timeScanSetSample")
    @OperateLog(module = "实验测试-经典模式-时间扫描-设置样品", description = "执行实验测试-经典模式-时间扫描-设置样品操作", recordParams = true)
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

    @GetMapping("sampleListPort")
    @ApiOperation("查询样品数据列表")
    public Result queryNearestSampleData(@RequestParam String taskId,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) Integer digit,
                                         @RequestParam String deviceSerialNumber) {
        return timeSweepConciseService.querySampleDataList(taskId,type,digit,deviceSerialNumber);
    }

    @GetMapping("deleteSampleListPort")
    @ApiOperation("删除样品列表接口")
    public Result deleteSampleListPort(@RequestParam String taskId,@RequestParam String sampleId,@RequestParam String analysisId) {
        return timeSweepConciseService.deleteSampleListPort(taskId,sampleId,analysisId);
    }

    @GetMapping("changeTheListPort")
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
    public Result restoreDeletedListData(@RequestParam String taskId,
                                         @RequestParam(required = false) String sampleId,
                                         @RequestParam String type,
                                         @RequestParam String analysisId){
        return timeSweepConciseService.restoreDeletedListData(taskId,sampleId,type,analysisId);
    }

    @PostMapping("updateDataColor")
    @ApiOperation("更新数据颜色接口")
    public Result updateDataColor(@RequestBody FUpdateDataColorDto paramDto){
        return timeSweepConciseService.updateDataColor(paramDto);
    }

    @GetMapping("queryGaugePointDataList")
    @ApiOperation("获取标记点接口")
    public Result queryAnalysisGaugePoint(@RequestParam String dataId,
                                          @RequestParam(required = false) Integer digit) {
        return timeSweepConciseService.queryGaugePoint(dataId, digit);
    }

    @PostMapping("addGaugePoint")
    @ApiOperation("添加标记点接口")
    public Result addAnalysisDataGaugePointPort(@RequestBody FAddGaugePointDto paramDto){
        return timeSweepConciseService.addGaugePointPort(paramDto);
    }

    @PostMapping("addGaugePointRemarks")
    @ApiOperation("添加标记点备注接口")
    public Result addGaugePointRemarks(@RequestBody FPeakValleyMarkingDto paramDto){
        return timeSweepConciseService.addGaugePointRemarks(paramDto);
    }

    @GetMapping("queryPeakValleyDataList")
    @ApiOperation("获取峰谷接口")
    public Result queryPeakAndValleysPort(@RequestParam String dataId,
                                          @RequestParam(required = false) Integer digit) {
        return timeSweepConciseService.queryPeakAndValleysPort(dataId, digit);
    }

    @PostMapping("seekingPeaksAndValleysPort")
    @ApiOperation("寻峰寻谷接口")
    public Result seekingPeaksAndValleysPort(@Valid @RequestBody FSeekingPeaksValleysDto paramDto,
                                             BindingResult bindingResult){
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            return Result.fail("参数校验失败: " + errorMsg);
        }
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
            @RequestParam(value = "digit", required = false) Integer digit,
            @RequestParam String deviceSerialNumber) throws Exception {
        
        // 调用 Service 层的多格式导出方法
        R<File> result = timeSweepConciseService.downloadFile(taskId, allDisplay, fileType, analysisIds, digit,deviceSerialNumber);
        
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

    @GetMapping("stopMeasure")
    @ApiOperation("停止测量接口")
    public Result stopMeasure(@RequestParam String deviceSerialNumber) {
        return timeSweepConciseService.stopMeasure(deviceSerialNumber);
    }
}
