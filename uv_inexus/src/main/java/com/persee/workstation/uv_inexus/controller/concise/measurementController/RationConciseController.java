/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.concise.measurementController;

import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.SaveDataDto;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FRationAddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FTaskHeadSnapshotDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.NewRationMethodParamCheckDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParam;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParamMapTableHead;
import com.persee.workstation.uv_inexus.service.concise.measurementService.RationConciseService;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FMeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FMethodChangeCheckVO;
import com.persee.workstation.uv_inexus.vo.concise.ration.FRationFitCurveVO;
import com.persee.workstation.uv_inexus.vo.concise.ration.FRationMethodParam;
import com.persee.workstation.uv_inexus.vo.concise.ration.FRationSampleDataVO;
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
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/quantitativeMeasurement/concise")
@Api("简洁模式--定量测量")
public class RationConciseController {
    @Autowired
    private RationConciseService rationService;

    @GetMapping("getCurrentInfo")
    @ApiOperation("获取当前信息")
    public Result<FMeasurementCurrentInfoVO> getCurrentInfo(@RequestParam String updateBy,
                                                            @RequestParam String measureType) {
        return rationService.getCurrentInfo(updateBy, measureType);
    }

    @PostMapping("newDraft")
    @ApiOperation("新建草稿")
    public Result<FMeasurementCurrentInfoVO> newDraft(@RequestBody SaveDataDto dto) {
        return rationService.newDraft(dto);
    }

    @PostMapping("checkParam")
    @ApiOperation("检查参数接口")
    public Result<FMethodChangeCheckVO> checkParam(@RequestBody NewRationMethodParamCheckDTO methodParam) {
        return rationService.checkMethodParam(methodParam);
    }

    @PostMapping("applyMethod")
    @ApiOperation("应用方法接口")
    public Result<FMeasurementCurrentInfoVO> applyMethod(@RequestBody NewRationMethodParamCheckDTO methodParam) {
        return rationService.applyMethod(methodParam);
    }


    @GetMapping("queryMethodParam")
    @ApiOperation("查询方法参数接口")
    public Result<FRationMethodParam> queryMethodParam(@RequestParam String methodId) {
        return rationService.queryMethodParam(methodId);
    }
    @GetMapping("getSampleList")
    @ApiOperation("获取样本列表接口")
    public Result getSampleList(@RequestParam String taskId,
                                @RequestParam String sampleKind) {
        return rationService.querySampleList(taskId, sampleKind);
    }

    @PostMapping("addOrDeleteSample")
    @ApiOperation("添加或删除样本接口")
    public Result addOrDeleteSample(@RequestBody FRationAddOrDeleteSampleDTO dto) {
        return rationService.addOrDeleteSample(dto);
    }

    @GetMapping("querySampleHeads")
    @ApiOperation("查询样品表头接口")
    public Result<List<FHeadVO>> querySampleHeads(@RequestParam String taskId,
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
    public Result revokeDel(@RequestParam String taskId,@RequestParam String sampleId) {
        return rationService.revokeDel(taskId, sampleId);
    }

    @GetMapping("saveTask")
    @ApiOperation("保存任务接口")
    public Result<Boolean> saveTask(@RequestParam String taskId, @RequestParam String updateBy){
        return rationService.saveButton(taskId, updateBy);
    }

    @GetMapping("getStandTableHead")
    @ApiOperation("获取标准样品表头信息接口")
    public Result<List<FHeadVO>> getTableHead(@RequestParam String taskId,
                                             @RequestParam String methodId,
                                             @RequestParam Boolean allDisplay,
                                              @RequestParam String deviceSerialNumber) {
        return rationService.getStandSampleTableHead(taskId, methodId, allDisplay, deviceSerialNumber);
    }

    @GetMapping("getUnknowTableHead")
    @ApiOperation("获取未知样品表头信息接口")
    public Result<List<FHeadVO>> getUnknownSampleTableHead(@RequestParam String taskId,
                                                          @RequestParam String methodId,
                                                          @RequestParam Boolean allDisplay,
                                                           @RequestParam String deviceSerialNumber) {
        return rationService.getUnknownSampleTableHead(taskId, methodId, allDisplay, deviceSerialNumber);
    }

    @PostMapping("querySampleData")
    @ApiOperation("查询样品数据接口")
    public Result<FRationSampleDataVO> querySampleData(@RequestBody FQueryDataDTO queryDataDTO) {
        return rationService.querySampleData(queryDataDTO);
    }

    @GetMapping("getCurveInfo")
    @ApiOperation("获取曲线信息接口")
    public Result<FRationFitCurveVO> getCurveInfo(@RequestParam String taskId,
                                                  @RequestParam String methodId) {
        return rationService.queryCurveInfo(taskId, methodId);
    }

    @GetMapping("checkAllSampleTestStatus")
    @ApiOperation("检查样品是否都测量了")
    public Result<Boolean> checkCreateNewSample(@RequestParam String taskId){
        R<Boolean> r = rationService.checkAllSampleTest(taskId);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        } else {
            return Result.ok(r.getData());
        }
    }

    @GetMapping("createSample")
    @ApiOperation("创建样品接口")
    public Result createSample(@RequestParam String taskId,
                               @RequestParam String sampleKind,
                               @RequestParam(required = false) String normalConcentration,
                               @RequestParam String updateBy,
                               @RequestParam String deviceSerialNumber) {
        R<Boolean> r = rationService.createSample(taskId, sampleKind, normalConcentration, updateBy, deviceSerialNumber);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        } else {
            return Result.ok();
        }
    }

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

    @ApiOperation("查询可绑定的方法参数")
    @GetMapping("queryInputParam")
    public Result<List<FInputParam>> queryInputParam(@RequestParam String methodId) {
        R<List<FInputParam>> r = rationService.queryInputParam(methodId);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ApiOperation("保存映射关系及表头信息")
    @PostMapping("saveInputParamMapTableHead")
    public Result<Boolean> saveInputParamMapTableHead(@RequestBody FTaskHeadSnapshotDTO taskHeadSnapshot) {
        R<Boolean> r = rationService.saveInputParamMapTableHead(taskHeadSnapshot);
        if (r.getSuccess()) {
            return Result.ok();
        } else {
            return Result.fail(r.getMessage());
        }
    }
    @ApiOperation("查询已绑定的映射信息")
    @GetMapping("queryInputParamMapTableHead")
    public Result<List<FInputParamMapTableHead>> queryInputParamMapTableHead(@RequestParam String taskId, @RequestParam String sampleKind) {
        R<List<FInputParamMapTableHead>> r = rationService.queryInputParamMapTableHead(taskId, sampleKind);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    /*
    @GetMapping("downloadDoc")
    @ApiOperation("多格式文件下载接口（支持word/excel/txt）")
    public ResponseEntity<?> downloadDoc(
            @ApiParam("任务ID") @RequestParam String taskId,
            @ApiParam("是否显示全部数据") @RequestParam Boolean allDisplay,
            @ApiParam("导出格式（word/excel/txt）") 
            @RequestParam(required = false) String fileType,
            // 新增 digit 参数（根据实际需求调整 required 和描述）
            @ApiParam(value = "修约", required = false) 
            @RequestParam(value = "digit", required = false) Integer digit) throws Exception {
        
        // 调用 Service 层的多格式导出方法
        R<File> result = rationService.downloadFile(taskId, allDisplay, fileType, digit);
        System.out.println("2222222222222");
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
        return ResponseEntity.badRequest().body("导出失败："  + result.getMessage());
    }
    */

    @GetMapping("downloadDoc")
    @ApiOperation("多格式文件下载接口（支持word/excel/txt）- 集成children和option数据")
    public ResponseEntity<?> downloadDoc(
            @ApiParam("任务ID") @RequestParam String taskId,
            @ApiParam("是否显示全部数据") @RequestParam Boolean allDisplay,
            @ApiParam("导出格式（word/excel/txt）") 
            @RequestParam(required = false) String fileType,
            @ApiParam(value = "修约", required = false) 
            @RequestParam(value = "digit", required = false) Integer digit,
            @ApiParam(value = "选项参数列表", required = false)
            @RequestParam(value = "option", required = false) List<String> option) throws Exception {
        
        // 调用 Service 层的多格式导出方法（集成children和option数据）
        R<File> result = rationService.downloadFileWithChildren(taskId, allDisplay, fileType, digit, option);
        
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
        return ResponseEntity.badRequest().body("导出失败："  + result.getMessage());
    }
}
