package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

@Data
public class TimeSweepParamter extends MeasurePublicParameters{
    // 光度模式
    private String spectralBandwidth;
    // 波长值
    private String waveLength;
    // 扫描总时长
    private String scanTime;
    // 扫描单位(0.分 1.秒)
    private String scanUnit;
    // 时间间隔
    private String intervalTime;
    // 扫描总个数
    private String totalNumber;
    // 多联池模式(0.顺序单池连续采样,1.循环多池交替采样)
    private String multiPoolMode;
}
