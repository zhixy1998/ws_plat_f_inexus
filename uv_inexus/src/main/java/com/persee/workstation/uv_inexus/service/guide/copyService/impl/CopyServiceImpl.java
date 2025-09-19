package com.persee.workstation.uv_inexus.service.guide.copyService.impl;

import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureAnalysisDataService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureDataService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureSampleService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskService;
import com.persee.workstation.uv_inexus.service.guide.copyService.CopyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CopyServiceImpl implements CopyService {
    @Autowired
    private FMeasureTaskService fMeasureTaskService;
    @Autowired
    private FMeasureDataService fMeasureDataService;
    @Autowired
    private FMeasureAnalysisDataService fMeasureAnalysisDataService;
    @Autowired
    private FMeasureSampleService fMeasureSampleService;


    @Override
    public Result<String> copy() {
        try {
            /*
             * 复制任务
             */
            fMeasureTaskService.list().forEach(fMeasureTask -> {
                fMeasureTask.setId(null);
                fMeasureTaskService.save(fMeasureTask);
            });

            /*
             * 复制样品
             */
            fMeasureSampleService.list().forEach(sample -> {
                        sample.setId(null);
                        fMeasureSampleService.save(sample);
                    }
            );
            /*
             * 复制数据
             */
            fMeasureDataService.list().forEach(fMeasureData -> {
                fMeasureData.setId(null);
                fMeasureDataService.save(fMeasureData);
            });
            fMeasureAnalysisDataService.list().forEach(fMeasureAnalysisData -> {
                fMeasureAnalysisData.setId(null);
                fMeasureAnalysisDataService.save(fMeasureAnalysisData);
            });
            return Result.ok("拷贝成功");
        } catch (Exception e) {
            return Result.fail("拷贝失败" + e.getMessage());
        }

    }
}
