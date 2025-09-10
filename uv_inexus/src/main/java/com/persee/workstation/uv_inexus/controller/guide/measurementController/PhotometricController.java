package com.persee.workstation.uv_inexus.controller.guide.measurementController;

import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskHeadSnapshot;
import com.persee.workstation.uv_inexus.service.guide.measurementService.PhotometricService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.SampleHeadTemplateService;
import com.persee.workstation.uv_inexus.vo.measurementVo.InputParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.SampleTemplateVO;
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

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/photometricMeasurement/")
@Api("光度测量")
public class PhotometricController {

    @Autowired
    PhotometricService photometricService;

    @Autowired
    SampleHeadTemplateService sampleHeadTemplateService;


    @PostMapping("updateMethodParameter")
    @ApiOperation("方法参数修改接口")
    public Result<String> updateMethodParameter(@RequestBody PhotometricMethodParameterDto methodDto) {
        return photometricService.updateMethodParameter(methodDto);
    }
    @PostMapping
    @ApiOperation("新增样品输入参数")
    public Result<InputParam> insertMethodParameter(@RequestBody InputParam methodParameterDto) {
        return photometricService.insertMethodParameter(methodParameterDto);

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

    @PostMapping("queryDataInfo")
    @ApiOperation("获取数据信息接口")
    public Result<List<Map<String, Object>>> queryDataInfo(@RequestBody QueryDataDTO queryDataDTO) {
        return photometricService.queryData(queryDataDTO);
    }

    @PostMapping("saveDataInfo")
    @ApiOperation("保存数据接口,备注等")
    public Result saveData(@RequestBody RemarkChangeSaveVO vo) {
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
    @ApiOperation("下载文档接口")
    public ResponseEntity downloadDoc(@RequestParam String taskId, @RequestParam Boolean allDisplay) throws Exception {
        R<File> r = photometricService.downloadWord(taskId, allDisplay);
        if (r.getSuccess()) {
            File file = r.getData();
            FileInputStream fis = new FileInputStream(file);
            InputStreamResource resource = new InputStreamResource(fis);

            String fileName = URLEncoder.encode(file.getName(), "UTF-8");
            //删除临时文件
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

    @ApiOperation("保存样品模板")
    @PostMapping("saveSampleTemplate")
    public Result saveSampleTemplate(@RequestBody SampleHeadTemplateDTO templateDto) {
        R<Boolean> r = sampleHeadTemplateService.createTemplate(templateDto);
        if (r.getSuccess()) {
            return Result.ok();
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ApiOperation("查询样品模板")
    @GetMapping("querySampleTemplate")
    public Result<List<SampleTemplateVO>> querySampleTemplate(@RequestParam String measureType, @RequestParam String search) {
        R<List<SampleTemplateVO>> r = sampleHeadTemplateService.queryTemplate(measureType, search);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @ApiOperation("删除样品模板")
    @GetMapping("deleteSampleTemplate")
    public Result<Boolean> deleteSampleTemplate(@RequestParam String templateId, @RequestParam String updateBy) {
        R<Boolean> r = sampleHeadTemplateService.deleteTemplate(templateId, updateBy);
        if (r.getSuccess()) {
            return Result.ok();
        } else {
            return Result.fail(r.getMessage());
        }
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
    public Result<Boolean> saveInputParamMapTableHead(@RequestBody TaskHeadSnapshot taskHeadSnapshot) {
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
