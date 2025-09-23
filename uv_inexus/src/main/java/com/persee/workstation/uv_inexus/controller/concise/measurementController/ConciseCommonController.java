package com.persee.workstation.uv_inexus.controller.concise.measurementController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ConciseCommonService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FSamplePoolConfigService;
import com.persee.workstation.uv_inexus.vo.concise.common.FMethodListWithPageVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
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

    @Autowired
    FSamplePoolConfigService fSamplePoolConfigService;


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


    @GetMapping("querySaveState")
    @ApiOperation("查询保存状态")
    public Result querySaveState(@RequestParam String taskId) {
        return conciseCommonService.querySaveState(taskId);
    }


    @GetMapping("saveDraftButton")
    @ApiOperation("保存草稿按钮接口")
    public Result saveDraftButton(@RequestParam String taskId) {
        return conciseCommonService.saveDraftButton(taskId);
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

    @PostMapping("updateTask")
    @ApiOperation("修改任务名称")
    public Result updateTask(@RequestBody UpdateTaskDto updateTaskDto) {
        return conciseCommonService.updateTask(updateTaskDto);
    }

    @PostMapping("changeSampleAttachmentInfo")
    @ApiOperation("修改样品池信息")
    public Result<Boolean> changeSampleAttachmentInfo(@RequestBody FSamplePoolInfoChangeDto dto) {
        R<Boolean> r = fSamplePoolConfigService.updateSamplePoolInfo(dto);
        if (r.getSuccess()) {
            return Result.ok();
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @GetMapping("querySampleAttachmentInfo")
    @ApiOperation("查询附件信息")
    public Result<FSamplePoolInfoVO> querySampleAttachmentInfo(@RequestParam String deviceSerialNumber) {
        R<FSamplePoolInfoVO> r = fSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @PostMapping("checkSampleInputParam")
    @ApiOperation("检查样品输入参数")
    public Result<Boolean> checkSampleInputParam(@RequestBody FCheckSampleInputParamDto checkSampleInputParamDto) {
        return conciseCommonService.checkSampleInputParam(checkSampleInputParamDto);
    }

    @PostMapping("checkFormula")
    @ApiOperation("检查公式")
    public Result<Boolean> checkFormula(@RequestBody FCheckFormulaDto checkFormulaDto) {
        return conciseCommonService.checkFormula(checkFormulaDto);
    }

}
