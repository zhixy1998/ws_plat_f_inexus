package com.persee.workstation.uv_inexus.vo.concise.ration;

import lombok.Data;

import java.util.List;

@Data
public class FRationSaveCountDataInfo {

    private String taskId;

    private String sampleId;

    private String measureDataId;

    private String analyzeDataId;

    private List<FRationCountDataInfo> countData;


}
