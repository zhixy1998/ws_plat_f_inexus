package com.persee.workstation.uv_inexus.controller.concise.measurementController;

import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SpectrumConciseService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("spectrumConciseController")
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/spectrumScanning/concise/")
@ApiModel("简洁模式: 光谱扫描")
public class SpectrumConciseController {

    @Autowired
    private SpectrumConciseService spectrumConciseService;      // 简洁模式: 光谱扫描业务层


    @GetMapping("queryNearestMethodData")
    @ApiOperation("查询最近方法数据")
    public Result queryNearestMethodData(@RequestParam String updateBy,@RequestParam String measureType) {
        return spectrumConciseService.queryNearestMethodData(updateBy,measureType);
    }

    @GetMapping("queryNearestSampleData")
    @ApiOperation("查询最近样品数据")
    public Result queryNearestSampleData(@RequestParam String methodId) {
        return spectrumConciseService.queryNearestSampleData(methodId);
    }

    @GetMapping("queryNearestMeasureData")
    @ApiOperation("查询最近测量数据")
    public Result queryNearestMeasureData(@RequestParam String methodId) {
        return spectrumConciseService.queryNearestMeasureData(methodId);
    }

    @GetMapping("searchMethodButton")
    @ApiOperation("查找方法按钮")
    public Result searchMethodButton(@RequestParam Integer peageNum,
                                     @RequestParam Integer pageSize, @RequestParam String updateBy,
                                     @RequestParam String measureType, @RequestParam(required = false) String methodName) {
        return spectrumConciseService.searchMethodButton(peageNum,pageSize,updateBy,measureType,methodName);
    }


}
