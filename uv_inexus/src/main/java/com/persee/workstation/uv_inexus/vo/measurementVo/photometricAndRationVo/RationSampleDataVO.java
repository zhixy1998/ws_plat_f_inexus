package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RationSampleDataVO {

    private List<Map<String, Object>> standSampleData;

    private List<Map<String, Object>> unknowSampleData;
}
