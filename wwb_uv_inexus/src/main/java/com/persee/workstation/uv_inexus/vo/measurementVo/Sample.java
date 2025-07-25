package com.persee.workstation.uv_inexus.vo.measurementVo;

import lombok.Data;

@Data
public class Sample {
    // 样品id
    private String id;
    // 样品编号
    private String sampleNumber;
    // 样品名称
    private String sampleName;
    // 样品位置
    private String sampleLocation;
}
