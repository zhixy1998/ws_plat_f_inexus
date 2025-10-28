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

package com.persee.workstation.uv_inexus.controller.guide.measurementController;

import com.alibaba.fastjson.JSON;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.dto.measurementDto.experimentTest.MethodExportRequest;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.service.guide.measurementService.ExperimentTestService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.SampleHeadTemplateService;
import com.persee.workstation.uv_inexus.vo.measurementVo.MethodBaseVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.SampleTemplateVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.TaskBaseVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/")
@ApiModel("实验测试")
public class ExperimentTestController {
    @Autowired
    private ExperimentTestService experimentTestService;

    @Autowired
    SampleHeadTemplateService sampleHeadTemplateService;


    @GetMapping("recentlyAccomplishTask")
    @ApiOperation("最近完成的任务")
    public Result recentlyAccomplishTask(@RequestParam String updateBy,@RequestParam(required = false) String measureType) {
       return experimentTestService.recentlyAccomplishTask(updateBy,measureType);
    }

    @GetMapping("recentlyCreatedTasks")
    @ApiOperation("最近创建的任务")
    public Result recentlyCreatedTasks(@RequestParam String updateBy,@RequestParam(required = false) String measureType) {
        return experimentTestService.recentlyCreatedTasks(updateBy,measureType);
    }

    @GetMapping("queryAllProject")
    @ApiOperation("查询所有项目接口")
    public Result queryAllProject() {
        return experimentTestService.queryAllProject();
    }


    @PostMapping("newProject")
    @ApiOperation("新建项目接口")
    public Result newProject(@RequestBody ProjectParamDto paramDto) {
        return experimentTestService.newProject(paramDto);
    }

    @PostMapping("createMethodPort")
    @ApiOperation("新建方法接口")
    public Result createMethodPort(@RequestBody MethodParamRequestDto methodDto) {
            return experimentTestService.createMethodPort(methodDto);
    }

    @PostMapping("createTask")
    @ApiOperation("新建任务接口")
    public Result createTask(@RequestBody CreateTaskParamDto paramDto) {
        return experimentTestService.createTask(paramDto);
    }

    @GetMapping("queryProjectStatistics")
    @ApiOperation("查询项目统计接口")
    public Result queryProjectStatistics(@RequestParam String updateBy) {
        return experimentTestService.queryProjectStatistics(updateBy);
    }

    @PostMapping("queryTaskPort")
    @ApiOperation("查看任务接口")
    public Result queryTaskPort(@RequestBody QueryTaskDto queryTask) {
        return experimentTestService.queryTaskPort(queryTask);
    }

    @GetMapping("queryMethodProt")
    @ApiOperation("查找方法接口")
    public Result queryMethodProt(@RequestParam String projectId,@RequestParam String measureType,@RequestParam("pageNum") Integer pageNum,@RequestParam("pageSize") Integer pageSize) {
        return experimentTestService.queryMethodProt(projectId,measureType,pageNum,pageSize);
    }

    @GetMapping("useThisMethodPort")
    @ApiOperation("使用此方法接口")
    public Result useThisMethodPort(@RequestParam String methodId){
        return experimentTestService.useThisMethodPort(methodId);
    }

    @GetMapping("continueMeasuringPort")
    @ApiOperation("继续测量接口")
    public Result continueMeasuringPort(@RequestParam String projectId,@RequestParam String methodId){
        return experimentTestService.continueMeasuringPort(projectId, methodId);
    }

    @GetMapping("assignMethodPort")
    @ApiOperation("指定方法接口")
    public Result assignMethodPort(@RequestParam String methodType,@RequestParam String projectId ){
        return experimentTestService.assignMethodPort(methodType,projectId);
    }

    @GetMapping("queryReferenceStandard")
    @ApiOperation("查询参考标准接口")
    public Result queryReferenceStandard(@RequestParam String standardName){
        return experimentTestService.queryReferenceStandard(standardName);
    }

    @GetMapping("preprocessTheData")
    @ApiOperation("前处理数据接口")
    public Result preprocessTheData(@RequestParam String standardId){
        return experimentTestService.preprocessTheData(standardId);
    }

    @GetMapping("reportDataQuery")
    @ApiOperation("报告表格数据查询")
    public Result reportDataQuery(@RequestParam String taskId,
                                  @RequestParam String reportName,
                                  @RequestParam String sampleId){
        return experimentTestService.reportDataQuery(taskId,reportName,sampleId);
    }

    @GetMapping("queryReportTableData")
    @ApiOperation("查询报告表格数据")
    public Result reportTableDataQuery(@RequestParam String taskId,
                                  @RequestParam(required = false) List<String> sampleIds){
        return experimentTestService.newReportTableDataQuery(taskId, sampleIds);
    }

    @GetMapping("obtainReportParameters")
    @ApiOperation("获取报告通用数据接口")
    public Result obtainReportParameters(@RequestParam String taskId,
                                         @RequestParam(required = false) String sampleId,
                                         @RequestParam(required = false) String serialNumber) {
        return experimentTestService.obtainReportParameters(taskId, sampleId, serialNumber);
    }

    @GetMapping("queryReportAllParameters")
    @ApiOperation("获取报告所有参数接口")
    public Result queryReportAllParameters(@RequestParam String taskId,
                                           @RequestParam String sampleId,
                                           @RequestParam(required = false) String serialNumber) {
        return experimentTestService.queryReportAllParameters(taskId, sampleId, serialNumber);
    }

    @GetMapping("getProjectInfo")
    @ApiOperation("获取项目信息")
    public Result<MeasurementCurrentInfoVO> getProjectInfo(@RequestParam String projectId, @RequestParam String measureType){
        return experimentTestService.getProjectInfo(projectId,measureType);
    }

    @GetMapping("getAllTask")
    @ApiOperation("获取项目下所有任务")
    public Result<List<TaskBaseVO>> getAllTask(@RequestParam String projectId, @RequestParam String measureType){
        return experimentTestService.getAllTask(projectId,measureType);
    }


    @GetMapping("useMethodToTest")
    @ApiOperation("使用该方法继续测量")
    public Result<MeasurementCurrentInfoVO> useMethodToTest(@RequestParam String methodId, @RequestParam String updateBy, @RequestParam String deviceSerialNumber){
        return experimentTestService.useMethodToTest(methodId,updateBy,deviceSerialNumber);
    }

    @GetMapping("listOfMethods")
    @ApiOperation("获取方法列表")
    public Result<List<MethodBaseVo>> listOfMethods(@RequestParam(required = false) String methodName,
                                                    @RequestParam Integer pageNum,
                                                    @RequestParam Integer pageSize,
                                                    @RequestParam String sort){
        return experimentTestService.listOfMethods(methodName,pageNum,pageSize, sort);
    }


    @PostMapping("methodExport")
    @ApiOperation("导出方法")
    public ResponseEntity<Resource> exportMethod(@RequestBody MethodExportRequest ids, HttpServletResponse response) throws IOException {
        List<TMeasureMethod> methods = experimentTestService.exportMethod(ids.getIds());
        if (methods.isEmpty()) {
            throw new RuntimeException("方法不存在");
        }

        String json;
        if (methods.size() == 1) {
            json = JSON.toJSONString(methods.get(0), true); // 单个
        } else {
            json = JSON.toJSONString(methods, true); // 多个
        }

        // 转换成字节数组资源
        ByteArrayResource resource = new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=methods.json")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .body(resource);
    }

    @PostMapping("methodImport")
    @ApiOperation("导入方法")
    public Result importMethod(@RequestParam("file") MultipartFile file) {
        return experimentTestService.importMethod(file);
    }


    @DeleteMapping("clearProject")
    @ApiOperation("清除项目")
    public Result clearProject(@RequestParam String projectId) {
        return experimentTestService.clearProject(projectId);
    }

    @DeleteMapping("clearTask")
    @ApiOperation("清除任务")
    public Result clearTask(@RequestParam String taskId) {
        return experimentTestService.clearTask(taskId);
    }

    @DeleteMapping("clearMethod")
    @ApiOperation("清除方法")
    public Result clearMethod(@RequestParam String methodId) {
        return experimentTestService.clearMethod(methodId);
    }

    @ApiOperation("保存样品模板")
    @PostMapping("saveSampleTemplate")
    public Result saveSampleTemplate(@RequestBody SampleHeadTemplateDTO templateDto) {
        R<Boolean> r = sampleHeadTemplateService.createTemplate(templateDto);
        if (r.getSuccess()) {
            return Result.ok();
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ApiOperation("查询样品模板")
    @GetMapping("querySampleTemplate")
    public Result<List<SampleTemplateVO>> querySampleTemplate(@RequestParam String measureType, @RequestParam String search) {
        R<List<SampleTemplateVO>> r = sampleHeadTemplateService.queryTemplate(measureType, search);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ApiOperation("删除样品模板")
    @GetMapping("deleteSampleTemplate")
    public Result<Boolean> deleteSampleTemplate(@RequestParam String templateId, @RequestParam String updateBy) {
        R<Boolean> r = sampleHeadTemplateService.deleteTemplate(templateId, updateBy);
        if (r.getSuccess()) {
            return Result.ok();
        } else {
            return Result.fail(r.getMessage());
        }
    }

}
