package com.persee.workstation.uv_inexus.controller.measurementController;

import com.persee.workstation.uv_inexus.model.Result;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/")
@ApiModel("实验测试")
public class ExperimentController {


    @GetMapping("/recentTask")
    @ApiModelProperty()
    public Result recentTask(){

        return Result.ok();
    }

}
