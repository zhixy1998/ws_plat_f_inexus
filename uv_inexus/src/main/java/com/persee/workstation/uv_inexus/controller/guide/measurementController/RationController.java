/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.guide.measurementController;

import com.persee.workstation.uv_inexus.config.ButtonPermission;
import com.persee.workstation.uv_inexus.dto.guide.GuideCreateSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.QueryDataDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.RationAddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.TaskHeadSnapshotDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.service.guide.measurementService.RationService;
import com.persee.workstation.uv_inexus.utils.OperateLog.OperateLog;
import com.persee.workstation.uv_inexus.vo.measurementVo.InputParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/quantitativeMeasurement/")
@Api("向导模式-定量测量")
public class RationController {
    @Autowired
    private RationService rationService;

    @GetMapping("getCurrentInfo")
    @ApiOperation("获取当前信息")
    public Result<MeasurementCurrentInfoVO> getCurrentInfo(@RequestParam String updateBy,
                                                           @RequestParam String measureType) {
        return rationService.getCurrentInfo(updateBy, measureType);
    }

//    @PostMapping("updateMethodParameter")
//    @ApiOperation("方法参数修改接口")
//    public Result updateMethodParameter(@RequestBody MethodParamsDto methodDto) {
//        return rationService.updateMethodParameter(methodDto);
//    }

    @GetMapping("queryMethodParam")
    @ApiOperation("查询方法参数接口")
    public Result<RationMethodParam> queryMethodParam(@RequestParam String methodId) {
        return rationService.queryMethodParam(methodId);
    }

    @ButtonPermission("wizardModeQuantityModifySampleInformation")
    @OperateLog(module = "实验测试-向导模式-定量测量-修改方法参数", description = "执行实验测试-向导模式-定量测量-修改方法参数操作", recordParams = true)
    @PostMapping("checkParam")
    @ApiOperation("检查参数接口")
    public Result<MethodChangeCheckVO> checkParam(@RequestBody MethodParamsDto methodParam) {
        return rationService.checkMethodParam(methodParam);
    }

    @ButtonPermission("wizardModeQuantityApplyMethod")
    @OperateLog(module = "实验测试-向导模式-定量测量-应用方法", description = "执行实验测试-向导模式-定量测量-应用方法操作", recordParams = true)
    @PostMapping("updateMethodParam")
    @ApiOperation("保存方法参数接口")
    public Result<MeasurementCurrentInfoVO> applyMethod(@RequestBody MethodParamsDto methodParam) {
        return rationService.updateMethodParam(methodParam);
    }

    @GetMapping("getSampleList")
    @ApiOperation("获取样本列表接口")
    public Result getSampleList(@RequestParam String taskId,
                                @RequestParam String sampleKind) {
        return rationService.querySampleList(taskId, sampleKind);
    }
//    @ButtonPermission(
//            description = "实验测试-向导模式-定量测量-设置样品", // 操作描述
//            permissionMapping = "add=wizardModeLumneSetSample,edit=wizardModeLumneModifySampleInformation,delete=quantityDeleteSample" // 自定义操作类型→驼峰权限标识
//    )
//    @OperateLog(
//            module = "实验测试-向导模式-定量测量-设置样品",
//            description = "执行实验测试-经典模式-定量测量-{operateType}样品操作",
//            recordParams = true
//    )
    @ButtonPermission("wizardModeLumneSetSample")
    @OperateLog(module = "实验测试-向导模式-定量测量-设置样品", description = "执行实验测试-向导模式-定量测量-设置样品操作", recordParams = true)
    @PostMapping("addOrDeleteSample")
    @ApiOperation("添加或删除样本接口")
    public Result addOrDeleteSample(@RequestBody RationAddOrDeleteSampleDTO dto) {
        return rationService.addOrDeleteSample(dto);
    }

    @GetMapping("querySampleHeads")
    @ApiOperation("查询样品表头接口")
    public Result<List<HeadVO>> querySampleHeads(@RequestParam(required = false) String taskId,
                                                 @RequestParam String sampleKind) {
        return rationService.querySampleHeads(taskId, sampleKind);
    }

    @GetMapping("deleteOneSample")
    @ApiOperation("删除单个样本接口")
    public Result deleteOneSample(String taskId, String sampleId) {
        return rationService.deleteSample(taskId, sampleId);
    }

    @GetMapping("revokeDel")
    @ApiOperation("撤销删除接口")
    public Result revokeDel(@RequestParam String taskId, @RequestParam String sampleId) {
        return rationService.revokeDel(taskId, sampleId);
    }

    @GetMapping("checkAllSampleTestStatus")
    @ApiOperation("检查标准样品是否都测量了")
    public Result<Boolean> checkCreateNewSample(@RequestParam String taskId) {
        R<Boolean> r = rationService.checkStandAllSampleTest(taskId);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        } else {
            return Result.ok(r.getData());
        }
    }

    @PostMapping("createSample")
    @ApiOperation("创建样品接口")
    public Result createSample(@RequestBody GuideCreateSampleDTO dto) {
        R<Boolean> r = rationService.createSample(dto);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        } else {
            return Result.ok();
        }
    }


    @GetMapping("getStandTableHead")
    @ApiOperation("获取标准样品表头信息接口")
    public Result<List<HeadVO>> getTableHead(@RequestParam String taskId,
                                             @RequestParam String methodId,
                                             @RequestParam Boolean allDisplay,
                                             @RequestParam String deviceSerialNumber) {
        return rationService.getStandSampleTableHead(taskId, methodId, allDisplay, deviceSerialNumber);
    }

    @GetMapping("getUnknowTableHead")
    @ApiOperation("获取未知样品表头信息接口")
    public Result<List<HeadVO>> getUnknownSampleTableHead(@RequestParam String taskId,
                                                          @RequestParam String methodId,
                                                          @RequestParam Boolean allDisplay,
                                                          @RequestParam String deviceSerialNumber) {
        return rationService.getUnknownSampleTableHead(taskId, methodId, allDisplay, deviceSerialNumber);
    }


    @PostMapping("querySampleData")
    @ApiOperation("查询样品数据接口")
    public Result<RationSampleDataVO> querySampleData(@RequestBody QueryDataDTO queryDataDTO) {
        return rationService.querySampleData(queryDataDTO);
    }


    @GetMapping("getCurveInfo")
    @ApiOperation("获取曲线信息接口")
    public Result<RationFitCurveVO> getCurveInfo(@RequestParam String taskId,
                                                 @RequestParam String methodId) {
        return rationService.queryCurveInfo(taskId, methodId);
    }

    @ButtonPermission("wizardModeLumneZero")
    @OperateLog(module = "实验测试-向导模式-定量测量-校零", description = "执行实验测试-向导模式-定量测量-校零记录", recordParams = true)
    @GetMapping("zero")
    @ApiOperation("校零接口")
    public Result photoZero(@RequestParam String deviceSerialNumber,
                            @RequestParam String methodId,
                            @RequestParam String taskId,
                            @RequestParam String updateBy) {
        return rationService.zero(deviceSerialNumber, methodId, taskId, updateBy);
    }

    @GetMapping("zero2")
    @ApiOperation("校零接口2")
    public Result photoZero2(@RequestParam String deviceSerialNumber,
                             @RequestParam String methodId,
                             @RequestParam String taskId,
                             @RequestParam String updateBy) {
        return rationService.zero2(deviceSerialNumber, methodId, taskId, updateBy);
    }

    @ApiOperation("测试")
    @GetMapping("test")
    public Result<String> test(@RequestParam String a,
                               @RequestParam String b) {

        rationService.test(a, b);

        return Result.ok("测试成功");
    }

    @ApiOperation("查询可绑定的方法参数")
    @GetMapping("queryInputParam")
    public Result<List<InputParam>> queryInputParam(@RequestParam String methodId) {
        R<List<InputParam>> r = rationService.queryInputParam(methodId);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ApiOperation("保存映射关系及表头信息")
    @PostMapping("saveInputParamMapTableHead")
    public Result<Boolean> saveInputParamMapTableHead(@RequestBody TaskHeadSnapshotDTO taskHeadSnapshot) {
        R<Boolean> r = rationService.saveInputParamMapTableHead(taskHeadSnapshot);
        if (r.getSuccess()) {
            return Result.ok();
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ApiOperation("查询已绑定的映射信息")
    @GetMapping("queryInputParamMapTableHead")
    public Result<List<InputParamMapTableHead>> queryInputParamMapTableHead(@RequestParam String taskId, @RequestParam String sampleKind) {
        R<List<InputParamMapTableHead>> r = rationService.queryInputParamMapTableHead(taskId, sampleKind);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ButtonPermission("wizardModeLumneExportData")
    @OperateLog(module = "实验测试-向导模式-定量测量-导出数据", description = "执行实验测试-向导模式-定量测量-导出数据记录", recordParams = true)
    @GetMapping("downloadDoc")
    @ApiOperation("多格式文件下载接口（支持word/excel/txt）")
    public ResponseEntity<?> downloadDoc(
            @ApiParam("任务ID") @RequestParam String taskId,
            @ApiParam("是否显示全部数据") @RequestParam Boolean allDisplay,
            @ApiParam("导出格式（word/excel/txt）") @RequestParam(required = false) String fileType,
            @RequestParam Integer digit,
            @RequestParam Integer otherDigit) throws Exception {

        // 调用 Service 层的多格式导出方法
        R<File> result = rationService.downloadFile(taskId, allDisplay, fileType, digit, otherDigit);

        if (result.getSuccess()) {
            File file = result.getData();
            FileInputStream fis = new FileInputStream(file);
            InputStreamResource resource = new InputStreamResource(fis);

            // 编码文件名（避免中文乱码）
            String fileName = URLEncoder.encode(file.getName(), "UTF-8");

            // 异步删除临时文件（10秒后）
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                    boolean deleteSuccess = file.delete();
                    System.out.println("临时文件删除结果：" + deleteSuccess);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // 构造响应头，指定下载和文件类型
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }

        // 导出失败时，返回错误信息
        return ResponseEntity.badRequest().body("导出失败：");
    }
}
