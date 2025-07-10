package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

@Data
public class SpectrumParameter extends MeasurePublicParameters {
    // 光度模式(1.abs 2.T% 3.R%)
    private String luminosity;
    // 开始波长
    private String startWaveLength;
    // 结束波长
    private String endWaveLength;
    // 波长间隔
    private String waveLengthInterval;
    // 重复模式
    private String repetitionPattern;
    // 重复次数
    private String repetitionTime;
    // 时间间隔
    private String timeInterval;
    // 阶梯校正(0.不启用 1.启用)
    private String ladderCorrection;
}
