package com.persee.workstation.uv_inexus.vo.concise.ration;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FRationSampleDataVO {

    private List<Map<String, Object>> standSampleData;

    private List<Map<String, Object>> unknowSampleData;
}
