/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.controller.concise.measurementController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRoundingVO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ConciseCommonService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FSamplePoolConfigService;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FMethodListWithPageVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FSaveDataVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

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

    @GetMapping("queryDeviceStatus")
    @ApiOperation("查询设备状态")
    public Result queryDeviceStatus(@RequestParam String deviceSerialNumber) {
        return conciseCommonService.queryDeviceStatus(deviceSerialNumber);
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

    @GetMapping("queryMethodParameter")
    @ApiOperation("查询方法参数")
    public Result queryMethodParameter(@RequestParam String methodId) {
        return conciseCommonService.queryMethodParameter(methodId);
    }

    @PostMapping("updateMethod")
    @ApiOperation("修改方法名称")
    public Result<String> updateMethod(@RequestBody UpdateMethodDto updateMethodDto) {
        return conciseCommonService.updateMethod(updateMethodDto);
    }

    @GetMapping("delMethod")
    @ApiOperation("删除方法")
    public Result<String> delMethod(@RequestParam("methodId") String methodId) {
        return conciseCommonService.delMethod(methodId);
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

    @GetMapping("delTask")
    @ApiOperation("删除历史文件")
    public Result delTask(@RequestParam("taskId") String taskId) {
        return conciseCommonService.delTask(taskId);
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


    @ApiOperation("设置为当前样品池")
    @GetMapping("setCurrentPool")
    public Result<Boolean> setCurrentPool(@RequestParam String deviceSerialNumber, @RequestParam Integer poolNum, @RequestParam String updateBy) {
        R<Boolean> r = fSamplePoolConfigService.setCurrentPool(deviceSerialNumber, poolNum, updateBy);
        if (r.getSuccess()) {
            return Result.ok();
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

    @GetMapping("obtainReportParameters")
    @ApiOperation("获取报告通用数据接口")
    public Result obtainReportParameters(@RequestParam String taskId,
                                         @RequestParam(required = false) String analysisId) {
        return conciseCommonService.obtainReportParameters(taskId,analysisId);
    }

    @GetMapping("queryReportAllParameters")
    @ApiOperation("获取报告所有参数接口")
    public Result queryReportAllParameters(@RequestParam String taskId,
                                           @RequestParam String sampleId,
                                           @RequestParam(required = false) String serialNumber) {
        return conciseCommonService.queryReportAllParameters(taskId, sampleId, serialNumber);
    }

    @GetMapping("queryReportTableData")
    @ApiOperation("查询报告表格数据")
    public Result reportTableDataQuery(@RequestParam String taskId,
                                       @RequestParam(required = false) String analysisId,
                                       @RequestParam(required = false) Boolean allDisplay,
                                       @RequestParam(required = false) Integer digit){
        return conciseCommonService.newReportTableDataQuery(taskId, analysisId,allDisplay,digit);
    }

    @GetMapping("isAddSample")
    @ApiOperation("是否添加样品")
    public Result isAddSample(@RequestParam String taskId,@RequestParam String deviceSerialNumber) {
        return conciseCommonService.isAddSample(taskId,deviceSerialNumber);
    }

    @GetMapping("queryDeviceTypeInfo")
    @ApiOperation("查询设备类型信息")
    public Result<DeviceMeasureVO> queryDeviceTypeInfo(@RequestParam(required = false) String deviceSerialNumber) {
        return conciseCommonService.queryDeviceTypeInfo(deviceSerialNumber);
    }

    @PostMapping("/copy")
    @ApiOperation("另存为")
    public Result<String> copyProject(@Valid @RequestBody CopyParam copyParam) {
        return  conciseCommonService.copy(copyParam);
    }

    @PostMapping("saveDataInfo")
    @ApiOperation("保存数据接口，用户自定义数据")
    public Result saveData(@RequestBody FSaveDataVO vo) {
        return conciseCommonService.saveDataInfo(vo);
    }


    @PostMapping("checkApproximateRange")
    @ApiOperation("检查修约区间是否正确")
    public Result<Boolean> checkApproximateRange(@RequestBody FRoundingVO vo) {
        return conciseCommonService.checkApproximateRange(vo);
    }
}
