package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.persee.workstation.uv_inexus.dto.concise.spectrum.FUseMethodDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ConciseCommonService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SpectrumConciseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
("ConciseCommonService")
public class ConciseCommonServiceImpl implements ConciseCommonService {

    @Autowired
    private SpectrumConciseService spectrumConciseService;

    @Override
    public Result applyMethod(FUseMethodDto paramDto) {
        // 根据测量类型进行区分
        switch (paramDto.getMeasureType()) {
            case "光度测量":
                return Result.ok();
            case "定量测量":
                return Result.ok();
            case "光谱扫描":
                return spectrumConciseService.use(paramDto);
            case "时间扫描":
                return Result.ok();
            default:
                return Result.fail("请选择正确的光度模式");
        }
    }
}
