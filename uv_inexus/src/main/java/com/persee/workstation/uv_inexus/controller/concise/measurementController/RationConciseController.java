package com.persee.workstation.uv_inexus.controller.concise.measurementController;

import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FRationAddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FTaskHeadSnapshotDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParam;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParamMapTableHead;
import com.persee.workstation.uv_inexus.service.concise.measurementService.RationConciseService;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FMeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.ration.FRationFitCurveVO;
import com.persee.workstation.uv_inexus.vo.concise.ration.FRationMethodParam;
import com.persee.workstation.uv_inexus.vo.concise.ration.FRationSampleDataVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/quantitativeMeasurement/concise")
@Api("简洁模式--定量测量")
public class RationConciseController {
    @Autowired
    private RationConciseService rationService;

    @GetMapping("getCurrentInfo")
    @ApiOperation("获取当前信息")
    public Result<FMeasurementCurrentInfoVO> getCurrentInfo(@RequestParam String updateBy,
                                                            @RequestParam String measureType) {
        return rationService.getCurrentInfo(updateBy, measureType);
    }


    @GetMapping("queryMethodParam")
    @ApiOperation("查询方法参数接口")
    public Result<FRationMethodParam> queryMethodParam(@RequestParam String methodId) {
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
    public Result addOrDeleteSample(@RequestBody FRationAddOrDeleteSampleDTO dto) {
        return rationService.addOrDeleteSample(dto);
    }

    @GetMapping("querySampleHeads")
    @ApiOperation("查询样品表头接口")
    public Result<List<FHeadVO>> querySampleHeads(@RequestParam String taskId,
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
    public Result<List<FHeadVO>> getTableHead(@RequestParam String taskId,
                                             @RequestParam String methodId,
                                             @RequestParam Boolean allDisplay) {
        return rationService.getStandSampleTableHead(taskId, methodId, allDisplay);
    }

    @GetMapping("getUnknowTableHead")
    @ApiOperation("获取未知样品表头信息接口")
    public Result<List<FHeadVO>> getUnknownSampleTableHead(@RequestParam String taskId,
                                                          @RequestParam String methodId,
                                                          @RequestParam Boolean allDisplay) {
        return rationService.getUnknownSampleTableHead(taskId, methodId, allDisplay);
    }

    @GetMapping("getStandSampleData")
    @ApiOperation("获取标准样品数据接口")
    public Result<List<Map<String, Object>>> getStandSampleData(@RequestParam String taskId,
                                                                @RequestParam Boolean allDisplay) {
        return rationService.queryStandSampleData(taskId, allDisplay);
    }

    @PostMapping("querySampleData")
    @ApiOperation("查询样品数据接口")
    public Result<FRationSampleDataVO> querySampleData(@RequestBody FQueryDataDTO queryDataDTO) {
        return rationService.querySampleData(queryDataDTO);
    }

    @GetMapping("getUnknowSampleData")
    @ApiOperation("获取未知样品数据接口")
    public Result<List<Map<String, Object>>> getUnknowSampleData(@RequestParam String taskId,
                                                                 @RequestParam Boolean allDisplay) {
        return rationService.queryUnknowSampleData(taskId, allDisplay);
    }

    @GetMapping("getCurveInfo")
    @ApiOperation("获取曲线信息接口")
    public Result<FRationFitCurveVO> getCurveInfo(@RequestParam String taskId,
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

    @ApiOperation("查询可绑定的方法参数")
    @GetMapping("queryInputParam")
    public Result<List<FInputParam>> queryInputParam(@RequestParam String methodId) {
        R<List<FInputParam>> r = rationService.queryInputParam(methodId);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ApiOperation("保存映射关系及表头信息")
    @PostMapping("saveInputParamMapTableHead")
    public Result<Boolean> saveInputParamMapTableHead(@RequestBody FTaskHeadSnapshotDTO taskHeadSnapshot) {
        R<Boolean> r = rationService.saveInputParamMapTableHead(taskHeadSnapshot);
        if (r.getSuccess()) {
            return Result.ok();
        } else {
            return Result.fail(r.getMessage());
        }
    }
    @ApiOperation("查询已绑定的映射信息")
    @GetMapping("queryInputParamMapTableHead")
    public Result<List<FInputParamMapTableHead>> queryInputParamMapTableHead(@RequestParam String taskId, @RequestParam String sampleKind) {
        R<List<FInputParamMapTableHead>> r = rationService.queryInputParamMapTableHead(taskId, sampleKind);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

}
