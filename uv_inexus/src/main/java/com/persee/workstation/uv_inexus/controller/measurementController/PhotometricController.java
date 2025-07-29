package com.persee.workstation.uv_inexus.controller.measurementController;

import com.persee.workstation.uv_inexus.dto.measurementDto.AddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricMethodParameterDto;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.PhotometricService;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MethodParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RemarkChangeSaveVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/photometricMeasurement/")
@Api("光度测量")
public class PhotometricController {

    @Autowired
    private PhotometricService photometricService;

    @PostMapping("updateMethodParameter")
    @ApiOperation("方法参数修改接口")
    public Result updateMethodParameter(@RequestBody PhotometricMethodParameterDto methodDto) {
        return photometricService.updateMethodParameter(methodDto);
    }

    @GetMapping("queryMethodParam")
    @ApiOperation("查询方法参数接口")
    public Result<MethodParam> queryMethodParam(@RequestParam String methodId) {
        return photometricService.queryMethodParam(methodId);
    }

    @GetMapping("getSampleList")
    @ApiOperation("获取样本列表接口")
    public Result getSampleList(@RequestParam String taskId) {
//        return photometricService.querySampleInfo(taskId);
        return photometricService.newQuerySampleInfo(taskId);
    }

    @GetMapping("querySampleHeads")
    @ApiOperation("获取样本输入表头接口")
    public Result<List<HeadVO>> querySampleHeads(@RequestParam String taskId) {
        return photometricService.querySampleHeads(taskId);
    }

    @GetMapping("getCurrentInfo")
    @ApiOperation("获取当前信息")
    public Result<MeasurementCurrentInfoVO> getCurrentInfo(@RequestParam String updateBy,
                                                           @RequestParam String measureType) {
        return photometricService.getCurrentInfo(updateBy, measureType);
    }

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
                               @RequestParam Boolean allDisplay) {
        return photometricService.tableHead(taskId, methodId, allDisplay);
    }

    @GetMapping("queryDataInfo")
    @ApiOperation("获取数据信息接口")
    public Result<List<Map<String, String>>> queryDataInfo(@RequestParam String taskId,
                                                           @RequestParam Boolean allDisplay) {
        return photometricService.queryData(taskId, allDisplay);
    }

    @PostMapping("saveDataInfo")
    @ApiOperation("保存数据接口,备注等")
    public Result saveData(@RequestBody RemarkChangeSaveVO vo) {
        return photometricService.saveDataInfo(vo);
    }

    @GetMapping("zero")
    @ApiOperation("校零接口")
    public Result photoZero(@RequestParam String serialNumber,
                       @RequestParam String methodId) {
        return photometricService.zero(serialNumber,methodId);
    }

    @GetMapping("saveTask")
    @ApiOperation("保存任务接口")
    public Result<Boolean> saveTask(@RequestParam String taskId, @RequestParam String updateBy){
        return photometricService.saveButton(taskId, updateBy);
    }

    @GetMapping("downloadDoc")
    @ApiOperation("下载文档接口")
    public ResponseEntity downloadDoc(@RequestParam String taskId) throws Exception {
        R<File> r = photometricService.downloadWord(taskId);
        if (r.getSuccess()) {
            File file = r.getData();
            FileInputStream fis = new FileInputStream(file);
            InputStreamResource resource = new InputStreamResource(fis);

            String fileName = URLEncoder.encode(file.getName(), "UTF-8");
            //5.删除临时文件
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                    System.out.println("删除文件");
                    boolean delete = file.delete();
                    System.out.println("删除结果：" + delete);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }
        return null;
    }

    @GetMapping("queryNewMethodTaskInfo")
    @ApiOperation("查询新方法任务信息接口")
    public Result queryNewMethodTask(@RequestParam String taskId) {
        return photometricService.getInfoByTaskId(taskId);
    }

}
