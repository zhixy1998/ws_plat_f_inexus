package com.persee.workstation.uv_inexus.controller.measurementController;

import com.persee.workstation.uv_inexus.dto.measurementDto.AddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricMethodParameterDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.PhotometricService;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricSampleAndDataVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricTableHeadVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/photometricMeasurement/")
@ApiModel("光度测量")
public class PhotometricController {

    @Autowired
    private PhotometricService photometricService;

    @PostMapping("updateMethodParameter")
    @ApiOperation("方法参数修改接口")
    public Result updateMethodParameter(@RequestBody PhotometricMethodParameterDto methodDto) {
        return photometricService.updateMethodParameter(methodDto);
    }

    @PostMapping("addOrDeleteSample")
    @ApiOperation("添加或删除样本接口")
    public Result addOrDeleteSample(@RequestBody AddOrDeleteSampleDTO dto) {
        return photometricService.addOrDeleteSample(dto);
    }

    @GetMapping("revokeDel")
    @ApiOperation("撤销删除接口")
    public Result revokeDel(@RequestParam String taskId,@RequestParam String sampleId) {
        return photometricService.revokeDel(taskId, sampleId);
    }

    @GetMapping("getTableHead")
    @ApiOperation("获取表头信息接口")
    public Result<PhotometricTableHeadVO> getTableHead(@RequestParam String taskId,@RequestParam String sampleId) {
        return photometricService.getTableHead(taskId, sampleId);
    }

    @GetMapping("queryDataInfo")
    @ApiOperation("获取数据信息接口")
    public Result<List<PhotometricSampleAndDataVO>> queryDataInfo(@RequestParam String taskId) {
        return photometricService.queryDataInfo(taskId);
    }

    @PostMapping("saveData")
    @ApiOperation("保存数据接口")
    public Result saveData(@RequestBody PhotometricSampleAndDataVO vo) {
        return photometricService.saveDataInfo(vo);
    }

}
