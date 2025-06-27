package com.persee.workstation.uv_inexus.controller.measurementController;

import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricMethodParameterDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.PhotometricService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/photometricMeasurement/")
public class PhotometricController {

    @Autowired
    private PhotometricService photometricService;


    @GetMapping("queryMethodProt")
    @ApiOperation("查找方法接口")
    public Result queryMethodProt(@RequestParam String projectId,@RequestParam String measureType) {
        return photometricService.queryMethodProt(projectId,measureType);
    }

    @PostMapping("saveMethodParameter")
    @ApiOperation("方法参数修改接口")
    public Result saveMethodParameter(@RequestBody PhotometricMethodParameterDto methodDto) {
        return photometricService.saveMethodParameter(methodDto);
    }
}
