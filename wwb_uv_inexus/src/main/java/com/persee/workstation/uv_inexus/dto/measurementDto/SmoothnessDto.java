package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

/**
 * 平滑的参数
 */
@Data
public class SmoothnessDto {
    // 待处理数据id
    private String measureDataId;
    // 点数
    private String pointNum;
    // 系数
    private String coefficient;
    // 分析数据名称
    private String analysisDataName;
}
