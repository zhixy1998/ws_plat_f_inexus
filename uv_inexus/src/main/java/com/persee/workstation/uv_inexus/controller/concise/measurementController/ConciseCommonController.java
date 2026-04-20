/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.concise.measurementController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.config.ButtonPermission;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.dto.measurementDto.AddSampleDTO;
import com.persee.workstation.uv_inexus.enums.measurementEnums.DatabaseEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRoundingVO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ConciseCommonService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SamplePoolConfigService;
import com.persee.workstation.uv_inexus.utils.OperateLog.OperateLog;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FMethodListWithPageVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FSaveDataVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.stream.Collectors;

@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/concise/")
@RestController
@Api("简洁模式: 通用接口")
public class ConciseCommonController {

    @Autowired
    private ConciseCommonService conciseCommonService;

    @Autowired
    SamplePoolConfigService fSamplePoolConfigService;


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
    @ButtonPermission(
            description = "实验测试-经典模式-重命名方法名称", // 操作描述
            permissionMapping = "lumne=lumneRenameMethodName,quantity=quantityRenameMethodName,spectralScan=spectralScanRenameMethodName,timeScan=timeScanRenameMethodName" // 自定义操作类型→驼峰权限标识
    )
    @OperateLog(
            module = "实验测试-经典模式",
            description = "执行实验测试-经典模式-{operateType}-重命名方法名称操作",
            recordParams = true
    )
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
    @ButtonPermission(
            description = "实验测试-经典模式-重命名历史文件名称", // 操作描述
            permissionMapping = "lumne=lumneRenameMethodName,quantity=quantityRenameDataName,spectralScan=spectralScanRenameDataName,timeScan=timeScanRenameDataName" // 自定义操作类型→驼峰权限标识
    )
    @OperateLog(
            module = "实验测试-经典模式",
            description = "执行实验测试-经典模式-{operateType}-重命名历史文件名称操作",
            recordParams = true
    )
    @PostMapping("updateTask")
    @ApiOperation("修改任务名称")
    public Result updateTask(@RequestBody UpdateTaskDto updateTaskDto) {
        return conciseCommonService.updateTask(updateTaskDto);
    }

    @ButtonPermission(
            description = "实验测试-经典模式-光度测量-设置样品", // 操作描述
            permissionMapping = "lumne=lumneModifyAttachmentParams,quantity=quantityModifyMethodParams,spectralScan=spectralScanModifyMethodParams,timeScan=timeScanModifyAttachmentParams" // 自定义操作类型→驼峰权限标识
    )
    @OperateLog(
            module = "实验测试-经典模式",
            description = "执行实验测试-经典模式-{operateType}-修改方法参数操作",
            recordParams = true
    )
    @PostMapping("changeSampleAttachmentInfo")
    @ApiOperation("修改样品池信息")
    public Result<Boolean> changeSampleAttachmentInfo(@RequestBody FSamplePoolInfoChangeDto dto) {
        dto.setUpdateDatabase(DatabaseEnum.CONCISE.getCode());
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

//    @GetMapping("queryReportAllParameters")
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


    @PostMapping("isAddSample")
    @ApiOperation("是否添加样品")
    public Result isAddSample(@RequestBody @Valid AddSampleDTO addSampleDTO,
                              BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            return Result.fail("参数校验失败: " + errorMsg);
        }
        return conciseCommonService.isAddSample(addSampleDTO);
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

    @GetMapping("setUpParametersPort")
    @ApiOperation("通用: 设置参数接口")
    public Result setUpParametersPort(@RequestParam String taskId,
                                      @RequestParam String deviceSerialNumber,
                                      @RequestParam String measureType) {
        return conciseCommonService.setUpParametersPort(taskId,deviceSerialNumber,measureType);
    }

    @ButtonPermission(
            description = "通用websocket前置判断接口",
            permissionMapping =
                    "lumne=lumneMeasurement," +
                    "quantity=quantityMeasurement," +
                    "spectralScan=spectralScanScan," +
                    "timeScan=timeScanScan," +
                    "spectralScanBaseline=spectralScanBaseline," +
                    "wmLumne=wizardModeLumneMeasurement," +
                    "wmQuantity=wizardModeQuantityMeasurement," +
                    "wmSpectralScan=wizardModeSpectralScanScan," +
                    "wmTimeScan=wizardModeTimeScanScan," +
                    "spectralScanBaseline=spectralScanBaseline," +
                    "timeScanBaseline=timeScanBaseline," +
                    "wmSpectralScanBaseline=wizardModeSpectralScanBaseline," +
                    "wmTimeScanBaseline=wizardModeTimeScanBaseline," +
                    "lumnePreviewReport=lumnePreviewReport," +
                    "quantityPreviewReport=quantityPreviewReport," +
                    "spectralPreviewReport=spectralPreviewReport," +
                    "timeScanPreviewReport=timeScanPreviewReport," +
                    "wmlumneGenerationReport=wmlumneGenerationReport," +
                    "wmquantityGenerationReport=wmquantityGenerationReport," +
                    "wmspectralGenerationReport=wmspectralGenerationReport," +
                    "wmtimeScanGenerationReport=wmtimeScanGenerationReport,"
    )
    @OperateLog(
            module = "实验测试-经典模式或向导模式",
            description = "执行实验测试-{operateTypeExt}-{operateType}-操作",
            recordParams = true
    )
    @GetMapping("checkOperatePermission")
    @ApiOperation("校验操作类型权限（返回true/false）")
    public Result<Boolean> checkOperatePermission(PermLogOccupyDTO requestDTO) {
        return Result.ok(true);
    }

    @PostMapping("queryMultipleTaskSample")
    @ApiOperation("查询多个任务样品")
    public Result queryMultipleTaskSample(@RequestBody MultipTaskSample multipTaskSample) {
        return conciseCommonService.queryMultipleTaskSample(multipTaskSample);
    }
}
