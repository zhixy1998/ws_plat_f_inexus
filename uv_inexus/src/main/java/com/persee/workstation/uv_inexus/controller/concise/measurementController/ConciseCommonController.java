package com.persee.workstation.uv_inexus.controller.concise.measurementController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.concise.common.*;

import com.persee.workstation.uv_inexus.dto.concise.spectrum.FUseMethodDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.QueryTaskDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ConciseCommonService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/concise/")
@RestController
@ApiModel("简洁模式: 通用接口")
public class ConciseCommonController {

    @Autowired
    private ConciseCommonService conciseCommonService;

    @RequestMapping("applyMethod")
    @ApiOperation("应用接口")
    public Result applyMethod(@RequestBody FUseMethodDto paramDto) {
        return conciseCommonService.applyMethod(paramDto);
    }

    @PostMapping("queryHistoryMethod")
    @ApiOperation("历史方法")
    public Result<Page<FMeasureMethodDto>> queryHistoryMethod(@RequestBody QueryMethodDto queryMethodDto) {
       return conciseCommonService.queryHistoryMethod(queryMethodDto);
    }
    @PostMapping("updateMethod")
    @ApiOperation("修改方法名称")
    public Result<String> updateMethod(@RequestBody UpdateMethodDto updateMethodDto) {
        return conciseCommonService.updateMethod(updateMethodDto);
    }
    @PostMapping("authMethodName")
    @ApiOperation("校验方法名称")
    public Result<String> authMethodName(String methodName) {
      return   conciseCommonService.authMethodName(methodName);

    }

    @GetMapping("detailInformation")
    @ApiOperation("点击展示详细测量参数")
    public Result<FMeasureMethodParam> detailInformation(@RequestParam("method") String methodName,
                                                         @RequestParam("methodType") String methodType) {
        return conciseCommonService.detailInformation(methodName, methodType);
    }
    @PostMapping("queryHistoryTask")
    @ApiOperation("历史草稿")
    public Result<Page<FMeasureTask>> queryHistoryTask(@RequestBody SelectTaskDto selectTaskDto) {
        return conciseCommonService.queryHistoryTask(selectTaskDto);
    }


}
