package com.persee.workstation.uv_inexus.po.concise.measurementPo.params;

import lombok.Data;

/**
 * 时间扫描方法参数
 */
@Data
public class FTimeSweepParamter extends FMeasurePublicParameters {
    // 光度模式(1.abs 2.T% 3.Es 4.Er)
    private String luminosity;
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
