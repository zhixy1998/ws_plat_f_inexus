package com.persee.workstation.uv_inexus.controller.measurementController;

import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceInitDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.CreateTaskParamDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.ExperimentMethodDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.QueryMeasuredDataVo;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.ExperimentTestService;
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
    private SpectrumService spectrumService;

    @Autowired
    private ExperimentTestService experimentTestService;

    @GetMapping("queryAllProject")
    @ApiModelProperty("查询所有项目接口")
    public Result queryAllProject(@RequestParam String updateBy) {
        return spectrumService.queryAllProject(updateBy);
    }


    @GetMapping("queryEssentialInformation")
    @ApiModelProperty("查询项目任务方法接口")
    public Result queryProjectTaskMethod(@RequestParam String updateBy) {
        return spectrumService.queryProjectTaskMethod(updateBy);
    }

    @GetMapping("queryProjectAllMethods/{projectId}/{measureForm}")
    @ApiModelProperty("查询项目下的所有方法")
    public Result queryProjectAllMethods(@PathVariable String projectId, @PathVariable String measureForm) {
        return spectrumService.queryProjectAllMethods(projectId,measureForm);
    }

    @GetMapping("methodParameter/{method_id}")
    @ApiModelProperty("查询方法参数接口")
    public Result queryMethodParameter(@PathVariable String method_id) {
        return spectrumService.queryMethodParameter(method_id);
    }

    @PostMapping("newMethod")
    @ApiModelProperty("新建方法接口")
    public Result newMethod(@RequestBody ExperimentMethodDto methodDto) {
        return spectrumService.newMethod(methodDto);
    }
    @PostMapping("createTask")
    @ApiModelProperty("新建任务接口")
    public Result createTask(@RequestBody CreateTaskParamDto paramDto) {
        return experimentTestService.createTask(paramDto);
    }
    @PostMapping("saveMethodParameter")
    @ApiModelProperty("保存方法参数接口")
    public Result saveMethodParameter(@RequestBody ExperimentMethodDto methodDto) {
        return spectrumService.saveMethodParameter(methodDto);
    }

    @GetMapping("measurement/{task_id}")
    @ApiModelProperty("测量接口")
    public Result measurement(@RequestBody DeviceInitDTO initDTO,  @PathVariable String task_id) {
        return spectrumService.measurement();
    }

//    @PostMapping("createTask")
//    @ApiModelProperty("创建任务接口")
//    public  Result createTask(@RequestBody TMeasureTaskDto taskDto) {
//        return SpectrumService.createTask(taskDto);
//    }


    @GetMapping("queryMeasuredData")
    @ApiModelProperty("查询测量数据列表接口")
    public Result queryMeasuredData(@RequestParam Integer pageNum,
                                    @RequestParam Integer pageSize,
                                    @RequestParam String taskId) {
        QueryMeasuredDataVo dataVo = new QueryMeasuredDataVo();
        dataVo.setPageNum(pageNum);
        dataVo.setPageSize(pageSize);
        dataVo.setTaskId(taskId);
        return spectrumService.queryMeasuredData(dataVo);
    }

    @GetMapping("queryMethodProt")
    @ApiModelProperty("查找方法接口")
    public Result queryMethodProt(@RequestParam String projectId) {
        return spectrumService.queryMethodProt(projectId);
    }

    @GetMapping("queryTaskProt")
    @ApiModelProperty("查看任务接口")
    public Result queryTaskProt(@RequestParam String projectId,@RequestParam String methodId,@RequestParam Integer pageNum, @RequestParam Integer pageSize) {
        return spectrumService.queryTaskProt(projectId,methodId,pageNum,pageSize);
    }

    @GetMapping("testPythonProt")
    public void testPythonProt() {
        spectrumService.testPythonProt();
    }

}
