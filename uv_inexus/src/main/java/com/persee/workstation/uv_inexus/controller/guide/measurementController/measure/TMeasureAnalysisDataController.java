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
