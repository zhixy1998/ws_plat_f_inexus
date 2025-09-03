package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import lombok.Data;

import java.util.List;

@Data
public class RationSaveCountDataInfo {

    private String taskId;

    private String sampleId;

    private String measureDataId;

    private String analyzeDataId;

    private List<RationCountDataInfo> countData;


}
