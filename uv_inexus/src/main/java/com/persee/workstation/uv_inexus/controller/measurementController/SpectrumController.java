package com.persee.workstation.uv_inexus.controller.measurementController;

import com.persee.workstation.uv_inexus.dto.measurementDto.ExperimentMethodDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.SpectrumService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/spectrumScanning/")
@ApiModel("光谱扫描")
public class SpectrumController {

    @Autowired
    private SpectrumService SpectrumService;

    @PostMapping("newMethod")
    @ApiModelProperty("新建任务接口")
    public Result newMethod(@RequestBody ExperimentMethodDto methodDto) {
        boolean save = SpectrumService.newMethod(methodDto);
        return Result.ok(save);
    }

    @GetMapping("queryEssentialInformation")
    @ApiModelProperty("查询基本信息接口")
    public Result queryTask() {

        return Result.ok();
    }

}
