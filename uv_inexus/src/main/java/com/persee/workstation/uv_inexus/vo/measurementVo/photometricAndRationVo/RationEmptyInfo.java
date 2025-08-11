package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import lombok.Data;

import java.util.List;

@Data
public class RationEmptyInfo {

    private String sampleId;

    private String sampleKind;

    private String sampleType;

    private List<RationWavelengthDataInfo> wavelengthData;

    private List<RationCountDataInfo> countData;
}
