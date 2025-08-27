package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

/**
 * 寻找峰谷差参数
 */
@Data
public class SeekingPeaksValleysDto {
    // 参数id(测量id和分析id共用一个)
    private String dataId;
    // 峰谷差阈值
    private Double height;
    // 峰间距
    private String distance;
}
