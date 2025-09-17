package com.persee.workstation.uv_inexus.vo.concise.ration;

import lombok.Data;

import java.util.List;

@Data
public class FRationEmptyInfo {

    private String sampleId;

    private String sampleKind;

    private String sampleType;

    private List<FRationWavelengthDataInfo> wavelengthData;

    private List<FRationCountDataInfo> countData;
}
