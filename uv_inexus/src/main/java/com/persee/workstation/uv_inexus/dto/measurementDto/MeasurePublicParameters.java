package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

@Data
public class MeasurePublicParameters {
    // 光谱带宽
    private String spectralBandwidth;
    // 响应时间
    private String responseTime;
    // 换灯波长
    private String changeLampWaveLength;
    // 能量灯
    private String energyLamp;
    // 增益
    private Integer gain;
}
