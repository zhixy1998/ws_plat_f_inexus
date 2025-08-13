package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

@Data
public class UpdateDataColorDto {
    // 数据id
    private String dataId;
    // 数据类型(0. 测量数据,1. 分析数据)
    private String type;
    // 颜色
    private String color;
}
