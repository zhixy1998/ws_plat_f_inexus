package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import java.util.List;

@Data
public class AddGaugePointDto {
    // 标记点数组
    private List<String> gaugePoints;
    // 数据id
    private String dataId;
}
