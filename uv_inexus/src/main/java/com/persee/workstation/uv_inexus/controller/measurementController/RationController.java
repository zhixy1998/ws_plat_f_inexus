package com.persee.workstation.uv_inexus.controller.measurementController;

import com.persee.workstation.uv_inexus.dto.measurementDto.AddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.RationService;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MethodParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.SampleVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public Result<MethodParam> queryMethodParam(@RequestParam String methodId) {
        return rationService.queryMethodParam(methodId);
    }

    @GetMapping("getSampleList")
    @ApiOperation("获取样本列表接口")
    public Result<List<SampleVO>> getSampleList(@RequestParam String taskId,
                                                @RequestParam String sampleKind) {
        return rationService.querySampleInfo(taskId, sampleKind);
    }

    @PostMapping("addOrDeleteSample")
    @ApiOperation("添加或删除样本接口")
    public Result addOrDeleteSample(@RequestBody AddOrDeleteSampleDTO dto) {
        return rationService.addOrDeleteSample(dto);
    }

    @GetMapping("revokeDel")
    @ApiOperation("撤销删除接口")
    public Result revokeDel(@RequestParam String taskId,@RequestParam String sampleId) {
        return rationService.revokeDel(taskId, sampleId);
    }

    @GetMapping("getStandTableHead")
    @ApiOperation("获取标准样品表头信息接口")
    public Result<List<HeadVO>> getTableHead(@RequestParam String taskId, @RequestParam String methodId, @RequestParam Boolean allDisplay) {
        return rationService.getStandSampleTableHead(taskId, methodId, allDisplay);
    }

    @GetMapping("getUnknowTableHead")
    @ApiOperation("获取未知样品表头信息接口")
    public Result<List<HeadVO>> getUnknownSampleTableHead(@RequestParam String taskId, @RequestParam String methodId, @RequestParam Boolean allDisplay) {
        return rationService.getUnknownSampleTableHead(taskId, methodId, allDisplay);
    }
}
