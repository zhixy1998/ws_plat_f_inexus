package com.persee.workstation.uv_inexus.controller.concise.measurementController;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.persee.workstation.uv_inexus.dto.concise.common.FAddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.dto.concise.photometricDto.NewPhotometricMethodParamCheckDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FTaskHeadSnapshotDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParam;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParamMapTableHead;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FMethodParam;
import com.persee.workstation.uv_inexus.service.concise.measurementService.PhotometricConciseService;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FMeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FMethodChangeCheckVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FSaveDataVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/photometricMeasurement/concise/")
@Api("简洁模式-光度测量")
public class PhotometricConciseController {

    @Autowired
    PhotometricConciseService fPhotometricService;

    @GetMapping("queryMethodParam")
    @ApiOperation("查询方法参数接口")
    public Result<FMethodParam> queryMethodParam(@RequestParam String methodId) {
        return fPhotometricService.queryMethodParam(methodId);
    }


    @GetMapping("newDraft")
    @ApiOperation("新建草稿")
    public Result<FMeasurementCurrentInfoVO> newDraft(@RequestParam String methodId, @RequestParam String updateBy) {
        return fPhotometricService.newDraft(methodId, updateBy);
    }

    @PostMapping("checkParam")
    @ApiOperation("检查参数接口")
    public Result<FMethodChangeCheckVO> checkParam(@RequestBody NewPhotometricMethodParamCheckDTO methodParam) {
        return fPhotometricService.checkMethodParam(methodParam);
    }

    @PostMapping("applyMethod")
    @ApiOperation("应用方法接口")
    public Result<FMeasurementCurrentInfoVO> applyMethod(@RequestBody NewPhotometricMethodParamCheckDTO methodParam) {
        return fPhotometricService.applyMethod(methodParam);
    }

    @GetMapping("getSampleList")
    @ApiOperation("获取样本列表接口")
    public Result getSampleList(@RequestParam String taskId) {
        return fPhotometricService.newQuerySampleInfo(taskId);
    }

    @GetMapping("querySampleHeads")
    @ApiOperation("获取样本输入表头接口")
    public Result<List<FHeadVO>> querySampleHeads(@RequestParam String taskId) {
        return fPhotometricService.querySampleHeads(taskId);
    }

    @GetMapping("getCurrentInfo")
    @ApiOperation("获取当前信息")
    public Result<FMeasurementCurrentInfoVO> getCurrentInfo(@RequestParam String updateBy,
                                                            @RequestParam String measureType) {
        return fPhotometricService.getCurrentInfo(updateBy, measureType);
    }

    @PostMapping("addOrDeleteSample")
    @ApiOperation("添加或删除样本接口")
    public Result addOrDeleteSample(@RequestBody FAddOrDeleteSampleDTO dto) {
        return fPhotometricService.addOrDeleteSample(dto);
    }

    @GetMapping("deleteOneSample")
    @ApiOperation("删除单个样本接口")
    public Result deleteOneSample(String taskId, String sampleId) {
        return fPhotometricService.deleteSample(taskId, sampleId);
    }

    @GetMapping("revokeDel")
    @ApiOperation("撤销删除接口")
    public Result revokeDel(@RequestParam String taskId,@RequestParam String sampleId) {
        return fPhotometricService.revokeDel(taskId, sampleId);
    }

    @GetMapping("getTableHead")
    @ApiOperation("获取表头信息接口")
    public Result getTableHead(@RequestParam String taskId,
                               @RequestParam String methodId,
                               @RequestParam Boolean allDisplay) {
        return fPhotometricService.tableHead(taskId, methodId, allDisplay);
    }

    @PostMapping("queryDataInfo")
    @ApiOperation("获取数据信息接口")
    public Result<List<Map<String, Object>>> queryDataInfo(@RequestBody FQueryDataDTO queryDataDTO) {
        return fPhotometricService.queryData(queryDataDTO);
    }

    @PostMapping("saveDataInfo")
    @ApiOperation("保存数据接口，用户自定义数据")
    public Result saveData(@RequestBody FSaveDataVO vo) {
        return fPhotometricService.saveDataInfo(vo);
    }

    @GetMapping("zero")
    @ApiOperation("校零接口")
    public Result photoZero(@RequestParam String deviceSerialNumber,
                       @RequestParam String methodId,
                            @RequestParam String taskId,
                            @RequestParam String updateBy) {
        return fPhotometricService.zero(deviceSerialNumber,methodId, taskId,updateBy);
    }

    @GetMapping("saveTask")
    @ApiOperation("保存任务接口")
    public Result<Boolean> saveTask(@RequestParam String taskId, @RequestParam String updateBy){
        return fPhotometricService.saveButton(taskId, updateBy);
    }

    @GetMapping("downloadDoc")
    @ApiOperation("多格式文件下载接口（支持word/excel/txt）")
    public ResponseEntity<?> downloadDoc(
            @ApiParam("任务ID") @RequestParam String taskId,
            @ApiParam("是否显示全部数据") @RequestParam Boolean allDisplay,
            @ApiParam("导出格式（word/excel/txt）") @RequestParam(required = false) String fileType) throws Exception {
        
        // 调用 Service 层的多格式导出方法
        R<File> result = fPhotometricService.downloadFile(taskId, allDisplay, fileType);
        System.out.println("111111111111111111");
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
        return ResponseEntity.badRequest().body("导出失败：" + result.getMessage());
    }

    @GetMapping("queryNewMethodTaskInfo")
    @ApiOperation("查询新方法任务信息接口")
    public Result queryNewMethodTask(@RequestParam String taskId) {
        return fPhotometricService.getInfoByTaskId(taskId);
    }

    @ApiOperation("查询可绑定的方法参数")
    @GetMapping("queryInputParam")
    public Result<List<FInputParam>> queryInputParam(@RequestParam String methodId) {
        R<List<FInputParam>> r = fPhotometricService.queryInputParam(methodId);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ApiOperation("保存映射关系及表头信息")
    @PostMapping("saveInputParamMapTableHead")
    public Result<Boolean> saveInputParamMapTableHead(@RequestBody FTaskHeadSnapshotDTO fTaskHeadSnapshot) {
        R<Boolean> r = fPhotometricService.saveInputParamMapTableHead(fTaskHeadSnapshot);
        if (r.getSuccess()) {
            return Result.ok();
        } else {
            return Result.fail(r.getMessage());
        }
    }
    @ApiOperation("查询已绑定的映射信息")
    @GetMapping("queryInputParamMapTableHead")
    public Result<List<FInputParamMapTableHead>> queryInputParamMapTableHead(@RequestParam String taskId) {
        R<List<FInputParamMapTableHead>> r = fPhotometricService.queryInputParamMapTableHead(taskId);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }


}
