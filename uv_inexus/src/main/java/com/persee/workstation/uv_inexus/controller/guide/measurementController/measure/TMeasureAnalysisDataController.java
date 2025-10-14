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

package com.persee.workstation.uv_inexus.controller.guide.measurementController.measure;


import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureAnalysisDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/measure/")
public class TMeasureAnalysisDataController {

    @Autowired
    private TMeasureAnalysisDataService tMeasureAnalysisDataService;
    @GetMapping("queryMeasureAnalysisData")
    public Result queryMeasureAnalysisData() {
        return Result.ok(tMeasureAnalysisDataService.list());
    }
}
