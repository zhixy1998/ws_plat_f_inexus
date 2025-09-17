package com.persee.workstation.uv_inexus.controller.concise.measurementController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ConciseCommonService;
import com.persee.workstation.uv_inexus.vo.concise.common.FMethodListWithPageVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/concise/")
@RestController
@Api("简洁模式: 通用接口")
public class ConciseCommonController {

    @Autowired
    private ConciseCommonService conciseCommonService;


    @GetMapping("queryDefaultParameters")
    @ApiOperation("查询默认参数")
    public Result queryDefaultParameters(@RequestParam String measureType) {
        return conciseCommonService.queryDefaultParameters(measureType);
    }

    @PostMapping("searchMethods")
    @ApiOperation("查找方法按钮")
    public Result<FMethodListWithPageVO> searchMethodButton(@RequestBody FQueryMethodWithPageDto dto) {
        return conciseCommonService.searchMethodButton(dto);
    }

    @PostMapping("queryHistoryMethod")
    @ApiOperation("历史方法")
    public Result<Page<FMeasureMethodParam>> queryHistoryMethod(@RequestBody QueryMethodDto queryMethodDto) {
       return conciseCommonService.queryHistoryMethod(queryMethodDto);
    }
    @PostMapping("updateMethod")
    @ApiOperation("修改方法名称")
    public Result<String> updateMethod(@RequestBody UpdateMethodDto updateMethodDto) {
        return conciseCommonService.updateMethod(updateMethodDto);
    }
    @PostMapping("queryHistoryTask")
    @ApiOperation("历史草稿")
    public Result<Page<FMeasureTaskDTO>> queryHistoryTask(@RequestBody SelectTaskDto selectTaskDto) {
        return conciseCommonService.queryHistoryTask(selectTaskDto);
    }

}
