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

package com.persee.workstation.uv_inexus.controller.factory_inspection;

import com.persee.workstation.uv_inexus.dto.factory_inspection.CalArtifactsSaveDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.factory_inspection.CalArtifactsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/factory-inspection/cal-artifacts")
@Api("标准实体物质管理")
public class CalArtifactsController {
    @Autowired
    private CalArtifactsService calArtifactsService;
//    /**
//     * 查询标准物质
//     * @param queryCalArtifactsDto
//     * @return
//     */
//    @ApiOperation("查询标准物质")
//    @PostMapping("/listCalArtifacts")
//    public Result<Page<CalArtifacts>> queryCalArtifacts(@RequestBody QueryCalArtifactsDto queryCalArtifactsDto) {
//        return calArtifactsService.queryCalArtifacts(queryCalArtifactsDto);
//    }
    @ApiOperation("增加/编辑标准物质")
    @PostMapping("/saveCalArtifacts")
    public Result addCalArtifacts(@Valid @RequestBody CalArtifactsSaveDto calArtifacts) {
        return calArtifactsService.addCalArtifacts(calArtifacts);
    }



}
