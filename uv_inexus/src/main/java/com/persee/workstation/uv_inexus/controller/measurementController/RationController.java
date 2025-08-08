package com.persee.workstation.uv_inexus.controller.measurementController;

import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.RationAddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.RationService;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/quantitativeMeasurement/")
@Api("定量测量")
public class RationController {
    @Autowired
    private RationService rationService;

    @GetMapping("getCurrentInfo")
    @ApiOperation("获取当前信息")
    public Result<MeasurementCurrentInfoVO> getCurrentInfo(@RequestParam String updateBy,
                                                           @RequestParam String measureType) {
        return rationService.getCurrentInfo(updateBy, measureType);
    }

    @PostMapping("updateMethodParameter")
    @ApiOperation("方法参数修改接口")
    public Result updateMethodParameter(@RequestBody MethodParamsDto methodDto) {
        return rationService.updateMethodParameter(methodDto);
    }

    @GetMapping("queryMethodParam")
    @ApiOperation("查询方法参数接口")
    public Result<RationMethodParam> queryMethodParam(@RequestParam String methodId) {
        return rationService.queryMethodParam(methodId);
    }
    @GetMapping("getSampleList")
    @ApiOperation("获取样本列表接口")
    public Result getSampleList(@RequestParam String taskId,
                                @RequestParam String sampleKind) {
        return rationService.querySampleList(taskId, sampleKind);
    }

    @PostMapping("addOrDeleteSample")
    @ApiOperation("添加或删除样本接口")
    public Result addOrDeleteSample(@RequestBody RationAddOrDeleteSampleDTO dto) {
        return rationService.addOrDeleteSample(dto);
    }

    @GetMapping("querySampleHeads")
    @ApiOperation("查询样品表头接口")
    public Result<List<HeadVO>> querySampleHeads(@RequestParam String taskId,
                                                 @RequestParam String sampleKind) {
        return rationService.querySampleHeads(taskId, sampleKind);
    }

    @GetMapping("deleteOneSample")
    @ApiOperation("删除单个样本接口")
    public Result deleteOneSample(String taskId, String sampleId) {
        return rationService.deleteSample(taskId, sampleId);
    }


    @GetMapping("revokeDel")
    @ApiOperation("撤销删除接口")
    public Result revokeDel(@RequestParam String taskId,@RequestParam String sampleId) {
        return rationService.revokeDel(taskId, sampleId);
    }

    @GetMapping("getStandTableHead")
    @ApiOperation("获取标准样品表头信息接口")
    public Result<List<HeadVO>> getTableHead(@RequestParam String taskId,
                                             @RequestParam String methodId,
                                             @RequestParam Boolean allDisplay) {
        return rationService.getStandSampleTableHead(taskId, methodId, allDisplay);
    }

    @GetMapping("getUnknowTableHead")
    @ApiOperation("获取未知样品表头信息接口")
    public Result<List<HeadVO>> getUnknownSampleTableHead(@RequestParam String taskId,
                                                          @RequestParam String methodId,
                                                          @RequestParam Boolean allDisplay) {
        return rationService.getUnknownSampleTableHead(taskId, methodId, allDisplay);
    }

    @GetMapping("getStandSampleData")
    @ApiOperation("获取标准样品数据接口")
    public Result<List<Map<String, String>>> getStandSampleData(@RequestParam String taskId,
                                                                @RequestParam Boolean allDisplay) {
        return rationService.queryStandSampleData(taskId, allDisplay);
    }

    @GetMapping("getUnknowSampleData")
    @ApiOperation("获取未知样品数据接口")
    public Result<List<Map<String, String>>> getUnknowSampleData(@RequestParam String taskId,
                                                                 @RequestParam Boolean allDisplay) {
        return rationService.queryUnknowSampleData(taskId, allDisplay);
    }

    @GetMapping("getCurveInfo")
    @ApiOperation("获取曲线信息接口")
    public Result<RationFitCurveVO> getCurveInfo(@RequestParam String taskId,
                                                 @RequestParam String methodId) {
        return rationService.queryCurveInfo(taskId, methodId);
    }

    @GetMapping("zero")
    @ApiOperation("校零接口")
    public Result photoZero(@RequestParam String deviceSerialNumber,
                            @RequestParam String methodId,
                            @RequestParam String taskId,
                            @RequestParam String updateBy) {
        return rationService.zero(deviceSerialNumber, methodId, taskId, updateBy);
    }
    @GetMapping("zero2")
    @ApiOperation("校零接口2")
    public Result photoZero2(@RequestParam String deviceSerialNumber,
                            @RequestParam String methodId,
                            @RequestParam String taskId,
                            @RequestParam String updateBy) {
        return rationService.zero2(deviceSerialNumber, methodId, taskId, updateBy);
    }
}
