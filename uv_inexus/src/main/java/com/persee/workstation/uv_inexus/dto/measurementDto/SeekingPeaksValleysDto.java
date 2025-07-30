package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

@Data
public class SeekingPeaksValleysDto {
    // 参数id(测量id和分析id共用一个)
    private String DataId;
    // 类型(0. 测量 1. 分析)
    private String type;
    // 峰谷差阈值
    private Double height;
    // 峰间距
    private String distance;
}
