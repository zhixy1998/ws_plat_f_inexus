package com.persee.workstation.uv_inexus.controller.measurementController;

import com.persee.workstation.uv_inexus.dto.measurementDto.CreateTaskParamDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.ProjectParamDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.ExperimentTestService;
import com.persee.workstation.uv_inexus.service.measurementService.SpectrumService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/")
@ApiModel("实验测试")
public class ExperimentTestController {
    @Autowired
    private ExperimentTestService experimentTestService;

    @Autowired
    private SpectrumService spectrumService;


    @GetMapping("queryProjectAllMethods/{projectId}/{measureForm}")
    @ApiModelProperty("查询项目下的所有方法")
    public Result queryProjectAllMethods(@PathVariable String projectId, @PathVariable String measureForm) {
        return spectrumService.queryProjectAllMethods(projectId,measureForm);
    }

    @GetMapping("recentlyAccomplishTask/{quantity}")
    @ApiModelProperty("最近完成的任务")
    public Result queryAllTask(@RequestParam String updateBy,@PathVariable Integer quantity) {
        return experimentTestService.queryAllTask(updateBy,quantity);
    }


    @PostMapping("newProject")
    @ApiModelProperty("新建项目接口")
    public Result newProject(@RequestBody ProjectParamDto paramDto) {
        return experimentTestService.newProject(paramDto);
    }

    @PostMapping("createTask")
    @ApiModelProperty("新建任务接口")
    public Result createTask(@RequestBody CreateTaskParamDto paramDto) {
        return experimentTestService.createTask(paramDto);
    }


//    @GetMapping("prpductStatistics")
//    public Result productStatistics(@RequestParam String updateBy) {
//        return experimentTestService.productStatistics(updateBy);
//    }

}
