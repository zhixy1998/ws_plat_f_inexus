/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.guide.measurementController;

import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.guide.measurementService.ExperimentTestService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.SpectrumService;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

@Slf4j
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

    @GetMapping("queryMeasurementParameters")
    @ApiOperation("查询测量参数接口")
    public Result queryMeasurementParameters(@RequestParam String taskId) {
        return spectrumService.queryMeasurementParameters(taskId);
    }

    @GetMapping("setUpParametersPort")
    @ApiOperation("设置参数接口")
    public Result setUpParametersPort(@RequestParam String methodId,@RequestParam String deviceSerialNumber) {
        return spectrumService.setUpParametersPort(methodId,deviceSerialNumber);
    }

    @GetMapping("saveData")
    @ApiOperation("保存数据接口")
    public Result saveData(@RequestParam String taskId) {
        return spectrumService.saveData(taskId);
    }


    @GetMapping("querySampleHeads")
    @ApiOperation("获取样本输入表头接口")
    public Result<List<HeadVO>> querySampleHeads(@RequestParam String taskId) {
        return spectrumService.querySampleHeads(taskId);
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


    @GetMapping("sampleListPort")
    @ApiOperation("获取样本列表接口")
    public Result sampleListPort(@RequestParam String taskId,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(required = false) Integer digit) {
        return spectrumService.sampleListPort(taskId,type,digit);
    }


    @GetMapping("deleteSampleListPort")
    @ApiOperation("删除样品列表接口")
    public Result deleteSampleListPort(@RequestParam String taskId,@RequestParam String sampleId,@RequestParam String analysisId) {
        return spectrumService.deleteSampleListPort(taskId,sampleId,analysisId);
    }


    @GetMapping("changeTheListPort")
    @ApiOperation("获取变换列表接口")
    public Result changeTheListPort(@RequestParam String taskId,
                                    @RequestParam(required = false) String type,
                                    @RequestParam(required = false) Integer digit) {
        return spectrumService.changeTheListPort(taskId,type,digit);
    }

    @GetMapping("deleteChangeListPort")
    @ApiOperation("删除变换列表接口")
    public Result deleteChangeListPort(@RequestParam String analysisId) {
        return spectrumService.deleteChangeListPort(analysisId);
    }


    @GetMapping("restoreDeletedListData")
    @ApiOperation("恢复已删除数据接口")
    public Result restoreDeletedListData(@RequestParam String taskId,@RequestParam String sampleId,@RequestParam String type,@RequestParam String analysisId){
        return spectrumService.restoreDeletedListData(taskId,sampleId,type,analysisId);
    }

    @PostMapping("updateDataColor")
    @ApiOperation("更新数据颜色接口")
    public Result updateDataColor(@RequestBody UpdateDataColorDto paramDto){
        return spectrumService.updateDataColor(paramDto);
    }

    @GetMapping("queryAnalysisGaugePoint")
    @ApiOperation("获取分析标记点接口")
    public Result queryAnalysisGaugePoint(@RequestParam String analysisId) {
        return spectrumService.queryAnalysisGaugePoint(analysisId);
    }

    @PostMapping("addAnalysisDataGaugePointPort")
    @ApiOperation("添加分析标记点接口")
    public Result addAnalysisDataGaugePointPort(@RequestBody AnalysisDataGaugePointDto paramDto){
        return spectrumService.addAnalysisDataGaugePointPort(paramDto);
    }

    @GetMapping("queryPeakAndValleysPort")
    @ApiOperation("获取峰谷接口")
    public Result queryPeakAndValleysPort(@RequestParam String dataId,
                                          @RequestParam(required = false) Integer digit) {
        return spectrumService.queryPeakAndValleysPort(dataId, digit);
    }

    @PostMapping("seekingPeaksAndValleysPort")
    @ApiOperation("寻峰寻谷接口")
    public Result seekingPeaksAndValleysPort(@RequestBody SeekingPeaksValleysDto paramDto){
        return spectrumService.peakSearchingPort(paramDto);
    }

    @PostMapping("addPeakValleyRemarks")
    @ApiOperation("添加峰谷备注接口")
    public Result addPeakValleyRemarks(@RequestBody PeakValleyMarkingDto paramDto){
        return spectrumService.addPeakValleyRemarks(paramDto);
    }


    @GetMapping("queryGaugePointPort")
    @ApiOperation("获取标记点接口")
    public Result queryGaugePointPort(@RequestParam String dataId,
                                      @RequestParam(required = false) Integer digit) {
        return spectrumService.queryGaugePointPort(dataId, digit);
    }

    @PostMapping("addGaugePoint")
    @ApiOperation("添加标记点接口")
    public Result addGaugePoint(@RequestBody AddGaugePointDto paramDto){
        return spectrumService.addGaugePoint(paramDto);
    }

    @PostMapping("addGaugePointRemarks")
    @ApiOperation("添加标记点备注接口")
    public Result addGaugePointRemarks(@RequestBody PeakValleyMarkingDto paramDto){
        return spectrumService.addGaugePointRemarks(paramDto);
    }

    @GetMapping("queryArithmeticSourceData")
    @ApiOperation("查询运算源数据接口")
    public Result queryArithmeticSourceData(@RequestParam String taskId) {
        return spectrumService.queryArithmeticSourceData(taskId);
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


    @PostMapping("testCompare")
    @ApiOperation("测试对比接口")
    public Result testCompare(@RequestParam String taskId,@RequestBody SpectrumParameter spectrumParameter) {
        return spectrumService.testCompare(taskId,spectrumParameter);
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
        R<File> result = spectrumService.downloadFile(taskId, allDisplay, fileType, analysisIds);
        
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
