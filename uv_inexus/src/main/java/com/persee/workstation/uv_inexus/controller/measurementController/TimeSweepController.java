package com.persee.workstation.uv_inexus.controller.measurementController;

import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.TimeSweepService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/timeSweep")
@ApiModel("时间扫描")
public class TimeSweepController {
    @Autowired
    private TimeSweepService timeSweepService;

    @PostMapping("updateMethodParameter")
    @ApiOperation("方法参数修改接口")
    public Result updateMethodParameter(@RequestBody MethodParamsDto methodDto) {
        return timeSweepService.updateMethodParameter(methodDto);
    }
}
