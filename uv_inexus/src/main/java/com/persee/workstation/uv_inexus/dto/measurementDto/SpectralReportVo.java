package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.po.measurementPo.*;
import lombok.Data;

import java.util.List;

@Data
public class SpectralReportVo {
    private TMeasureTask task;
    private List<TMeasureData> data;
    private List<TMeasureAnalysisData> analysisData;
    private List<TMeasureSample> sample;
    private TMeasureMethod method;
    private TMeasureProject project;
}
