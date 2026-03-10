/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.guide.measurementController;

import com.persee.workstation.uv_inexus.config.ButtonPermission;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.dto.measurementDto.experimentTest.TimeSweepMethodParamsDto;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TimeSweepService;
import com.persee.workstation.uv_inexus.utils.OperateLog.OperateLog;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MethodChangeCheckVO;
import io.swagger.annotations.ApiModel;
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

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/timeSweep/")
@ApiModel("时间扫描")
public class TimeSweepController {
    @Autowired
    private TimeSweepService timeSweepService;


    @GetMapping("queryEssentialInformation")
    @ApiOperation("查询项目任务方法接口")
    public Result queryProjectTaskMethod(@RequestParam String updateBy,
                                         @RequestParam String measureType) {
        return timeSweepService.queryProjectTaskMethod(updateBy,measureType);
    }


    @GetMapping("queryProjectAllMethods")
    @ApiOperation("查询项目下的所有方法")
    public Result queryProjectAllMethods(@RequestParam String projectId,
                                         @RequestParam String measureForm,
                                         @RequestParam String updateBy) {
        return timeSweepService.queryProjectAllMethods(projectId,measureForm,updateBy);
    }

    @ButtonPermission("wizardModeTimeScanModifyMethodParams")
    @OperateLog(module = "实验测试-向导模式-光谱扫描-修改方法参数", description = "执行实验测试-向导模式-光谱扫描-修改方法参数操作", recordParams = true)
    @PostMapping("checkParam")
    @ApiOperation("检查参数接口")
    public Result<MethodChangeCheckVO> checkParam(@RequestBody TimeSweepMethodParamsDto methodParam) {
        return timeSweepService.checkMethodParam(methodParam);
    }

    @GetMapping("queryMethodParameter")
    @ApiOperation("查询方法参数接口")
    public Result queryMethodParameter(@RequestParam String methodId) {
        return timeSweepService.queryMethodParameter(methodId);
    }

    @ButtonPermission("wizardModeTimeScanApplyMethod")
    @OperateLog(module = "实验测试-向导模式-时间扫描-应用方法", description = "执行实验测试-向导模式-时间扫描-应用方法操作", recordParams = true)
    @PostMapping("updateMethodParameter")
    @ApiOperation("修改方法参数接口")
    public Result updateMethodParameter(@Valid @RequestBody TimeSweepMethodParameterDto params, BindingResult bindingResult) {
        // 校验参数
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(error ->  error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            return Result.fail("参数校验失败: " + errorMsg);
        }
        return timeSweepService.updateMethodParameter(params);
    }



    @GetMapping("sampleListPort")
    @ApiOperation("获取样本列表接口")
    public Result sampleListPort(@RequestParam String taskId,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(required = false) Integer digit) {
        return timeSweepService.sampleListPort(taskId,type, digit);
    }

    @GetMapping("deleteSampleListPort")
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
    public Result restoreDeletedListData(@RequestParam String taskId,@RequestParam String sampleId,@RequestParam String type,@RequestParam String analysisId){
        return timeSweepService.restoreDeletedListData(taskId,sampleId,type,analysisId);
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

    @GetMapping("querySampleHeads")
    @ApiOperation("获取样本输入表头接口")
    public Result<List<HeadVO>> querySampleHeads(@RequestParam(required = false) String taskId) {
        return timeSweepService.querySampleHeads(taskId);
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

    @GetMapping("downloadDoc")
    @ApiOperation("多格式文件下载接口（支持word/excel/txt）")
    // 关键修正：1. 修复@ApiParam描述错误 2. 为List参数添加@RequestParam的value和required属性
    public ResponseEntity<?> downloadDoc(
            @ApiParam(value = "任务ID", required = true) @RequestParam String taskId,
            @ApiParam(value = "是否显示全部数据（true/false）", required = true) @RequestParam Boolean allDisplay,
            @ApiParam(value = "筛选的analysisId列表（多个ID用逗号分隔，如123,456）", required = false) 
            @RequestParam(value = "analysisIds", required = false) List<String> analysisIds,
            @ApiParam(value = "导出格式（word/excel/txt，默认word）", required = false) 
            @RequestParam(required = false) String fileType) throws Exception {
        
        // 调用 Service 层的多格式导出方法
        R<File> result = timeSweepService.downloadFile(taskId, allDisplay, fileType, analysisIds);
        
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
        return timeSweepService.stopMeasure(deviceSerialNumber);
    }

}
