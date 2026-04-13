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
import com.persee.workstation.uv_inexus.dto.measurementDto.AddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricMethodParameterDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.QueryDataDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.TaskHeadSnapshotDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.service.guide.measurementService.PhotometricService;
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
import java.util.Map;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/photometricMeasurement/")
@Api("向导模式-光度测量")
public class PhotometricController {

    @Autowired
    PhotometricService photometricService;

    @PostMapping("updateMethodParameter")
    @ApiOperation("方法参数修改接口")
    public Result updateMethodParameter(@RequestBody PhotometricMethodParameterDto methodDto) {
        return photometricService.updateMethodParameter(methodDto);
    }

    @ButtonPermission("wizardModeLumneModifyMethodParams")
    @OperateLog(module = "实验测试-向导模式-定量测量-修改方法参数", description = "执行实验测试-向导模式-定量测量-修改方法参数", recordParams = true)
    @PostMapping("checkParam")
    @ApiOperation("检查参数接口")
    public Result<MethodChangeCheckVO> checkParam(@RequestBody PhotometricMethodParameterDto methodParam) {
        return photometricService.checkMethodParam(methodParam);
    }

    @ButtonPermission("wizardModeLumneApplyMethod")
    @OperateLog(module = "实验测试-向导模式-定量测量-应用方法", description = "执行实验测试-向导模式-定量测量-应用方法操作", recordParams = true)
    @PostMapping("updateMethodParam")
    @ApiOperation("保存方法参数接口")
    public Result<MeasurementCurrentInfoVO> applyMethod(@RequestBody PhotometricMethodParameterDto methodParam) {
        return photometricService.updateMethodParam(methodParam);
    }

    @GetMapping("setUpParametersPort")
    @ApiOperation("设置参数接口")
    public Result setUpParametersPort(@RequestParam String methodId, @RequestParam String deviceSerialNumber) {
        return photometricService.setUpParametersPort(methodId, deviceSerialNumber);
    }


    @PostMapping
    @ApiOperation("新增样品输入参数")
    public Result<InputParam> insertMethodParameter(@RequestBody InputParam methodParameterDto) {
        return photometricService.insertMethodParameter(methodParameterDto);

    }

    @GetMapping("createSample")
    @ApiOperation("创建样本接口")
    public Result<Boolean> createSample(@RequestParam String taskId,
                                        @RequestParam String updateBy,
                                        @RequestParam String deviceSerialNumber) {
        R<Boolean> r =photometricService.createSample(taskId, updateBy, deviceSerialNumber);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        } else {
            return Result.ok();
        }
    }

    @GetMapping("queryMethodParam")
    @ApiOperation("查询方法参数接口")
    public Result<MethodParam> queryMethodParam(@RequestParam String methodId) {
        return photometricService.queryMethodParam(methodId);
    }

    @GetMapping("getSampleList")
    @ApiOperation("获取样本列表接口")
    public Result getSampleList(@RequestParam String taskId) {
        return photometricService.newQuerySampleInfo(taskId);
    }

    @GetMapping("querySampleHeads")
    @ApiOperation("获取样本输入表头接口")
    public Result<List<HeadVO>> querySampleHeads(@RequestParam(required = false) String taskId) {
        return photometricService.querySampleHeads(taskId);
    }

    @GetMapping("getCurrentInfo")
    @ApiOperation("获取当前信息")
    public Result<MeasurementCurrentInfoVO> getCurrentInfo(@RequestParam String updateBy,
                                                           @RequestParam String measureType) {
        return photometricService.getCurrentInfo(updateBy, measureType);
    }

//    @ButtonPermission(
//            description = "实验测试-向导模式-光度测量-设置样品", // 操作描述
//            permissionMapping = "add=wizardModeLumneSetSample,edit=wizardModeLumneModifySampleInformation,delete=lumnDelSample" // 自定义操作类型→驼峰权限标识
//    )
//    @OperateLog(
//            module = "实验测试-向导模式-光度测量-设置样品",
//            description = "执行实验测试-经典模式-光度测量-{operateType}样品操作",
//            recordParams = true
//    )
    @ButtonPermission("wizardModeLumneSetSample")
    @OperateLog(module = "实验测试-向导模式-光度测量-设置样品", description = "执行实验测试-经典模式-光度测量-设置样品操作", recordParams = true, ignore = true)
    @PostMapping("addOrDeleteSample")
    @ApiOperation("添加或删除样本接口")
    public Result addOrDeleteSample(@RequestBody AddOrDeleteSampleDTO dto) {
        return photometricService.addOrDeleteSample(dto);
    }

    @GetMapping("deleteOneSample")
    @ApiOperation("删除单个样本接口")
    public Result deleteOneSample(String taskId, String sampleId) {
        return photometricService.deleteSample(taskId, sampleId);
    }

    @GetMapping("revokeDel")
    @ApiOperation("撤销删除接口")
    public Result revokeDel(@RequestParam String taskId,@RequestParam String sampleId) {
        return photometricService.revokeDel(taskId, sampleId);
    }

    @GetMapping("getTableHead")
    @ApiOperation("获取表头信息接口")
    public Result getTableHead(@RequestParam String taskId,
                               @RequestParam String methodId,
                               @RequestParam Boolean allDisplay,
                               @RequestParam String deviceSerialNumber) {
        return photometricService.tableHead(taskId, methodId, allDisplay, deviceSerialNumber);
    }

    @PostMapping("queryDataInfo")
    @ApiOperation("获取数据信息接口")
    public Result<List<Map<String, Object>>> queryDataInfo(@RequestBody QueryDataDTO queryDataDTO) {
        return photometricService.queryData(queryDataDTO);
    }

    @PostMapping("saveDataInfo")
    @ApiOperation("保存数据接口,备注等")
    public Result saveData(@RequestBody SaveDataVO vo) {
        return photometricService.saveDataInfo(vo);
    }

    @GetMapping("zero")
    @ApiOperation("校零接口")
    public Result photoZero(@RequestParam String deviceSerialNumber,
                       @RequestParam String methodId,
                            @RequestParam String taskId,
                            @RequestParam String updateBy) {
        return photometricService.zero(deviceSerialNumber,methodId, taskId,updateBy);
    }

    @GetMapping("saveTask")
    @ApiOperation("保存任务接口")
    public Result<Boolean> saveTask(@RequestParam String taskId, @RequestParam String updateBy){
        return photometricService.saveButton(taskId, updateBy);
    }

    @GetMapping("downloadDoc")
    @ApiOperation("多格式文件下载接口（支持word/excel/txt）- 集成children和option数据")
    public ResponseEntity<?> downloadDoc(
            @ApiParam("任务ID") @RequestParam String taskId,
            @ApiParam("是否显示全部数据") @RequestParam Boolean allDisplay,
            @ApiParam("导出格式（word/excel/txt）") @RequestParam(required = false) String fileType,
            @ApiParam(value = "数字参数（说明digit的业务含义，如：XX编号）", required = false)
            @RequestParam(value = "digit", required = false) Integer digit,
            @ApiParam(value = "选项参数（对应queryData的option）", required = false)
            @RequestParam(value = "otherDigit") Integer otherDigit,
            @RequestParam(value = "option", required = false) java.util.List<String> option) throws Exception {

        R<File> result = photometricService.downloadFileWithChildren(taskId, allDisplay, fileType, digit, otherDigit,option);

        if (result.getSuccess()) {
            File file = result.getData();
            FileInputStream fis = new FileInputStream(file);
            InputStreamResource resource = new InputStreamResource(fis);

            String fileName = URLEncoder.encode(file.getName(), "UTF-8");

            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                    boolean deleteSuccess = file.delete();
                    System.out.println("临时文件删除结果：" + deleteSuccess);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }

        return ResponseEntity.badRequest().body("导出失败：" + result.getMessage());
    }

    @GetMapping("queryNewMethodTaskInfo")
    @ApiOperation("查询新方法任务信息接口")
    public Result queryNewMethodTask(@RequestParam String taskId) {
        return photometricService.getInfoByTaskId(taskId);
    }

    @ApiOperation("查询可绑定的方法参数")
    @GetMapping("queryInputParam")
    public Result<List<InputParam>> queryInputParam(@RequestParam String methodId) {
        R<List<InputParam>> r = photometricService.queryInputParam(methodId);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ApiOperation("保存映射关系及表头信息")
    @PostMapping("saveInputParamMapTableHead")
    public Result<Boolean> saveInputParamMapTableHead(@RequestBody TaskHeadSnapshotDTO taskHeadSnapshot) {
        R<Boolean> r = photometricService.saveInputParamMapTableHead(taskHeadSnapshot);
        if (r.getSuccess()) {
            return Result.ok();
        } else {
            return Result.fail(r.getMessage());
        }
    }
    @ApiOperation("查询已绑定的映射信息")
    @GetMapping("queryInputParamMapTableHead")
    public Result<List<InputParamMapTableHead>> queryInputParamMapTableHead(@RequestParam String taskId) {
        R<List<InputParamMapTableHead>> r = photometricService.queryInputParamMapTableHead(taskId);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }
}
