package com.persee.workstation.uv_inexus.po.concise.measurementPo.params;

import lombok.Data;

/**
 * 光谱扫描方法参数
 */
@Data
public class FSpectrumParameter extends FMeasurePublicParameters {
    // 光度模式(1.abs 2.T% 3.Es 4.Er)
    private String luminosity;
    // 开始波长
    private String startWaveLength;
    // 结束波长
    private String endWaveLength;
    // 波长间隔
    private String waveLengthInterval;
    // 重复模式(0.不重复 1.单池重复)
    private String repetitionPattern;
    // 重复次数
    private String repetitionTime;
    // 时间间隔
    private String timeInterval;
    // 阶梯校正(false.不启用 true.启用)
    private Boolean ladderCorrection;
    // 扫描速度(0.慢速 1.中速 2.快速 3.极快)
    private String scanSpeed;
}
