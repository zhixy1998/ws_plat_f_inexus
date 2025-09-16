package com.persee.workstation.uv_inexus.controller.concise.measurementController;

import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ConciseCommonService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/concise/")
@RestController
@ApiModel("简洁模式: 通用接口")
public class ConciseCommonController {

    @Autowired
    private ConciseCommonService conciseCommonService;


    @GetMapping("queryDefaultParameters")
    @ApiOperation("查询默认参数")
    public Result queryDefaultParameters(@RequestParam String measureType) {
        return conciseCommonService.queryDefaultParameters(measureType);
    }

    @GetMapping("searchMethodButton")
    @ApiOperation("查找方法按钮")
    public Result searchMethodButton(@RequestParam Integer pageNum,
                                     @RequestParam Integer pageSize, @RequestParam String updateBy,
                                     @RequestParam String measureType, @RequestParam(required = false) String methodName) {
        return conciseCommonService.searchMethodButton(pageNum,pageSize,updateBy,measureType,methodName);
    }


}
