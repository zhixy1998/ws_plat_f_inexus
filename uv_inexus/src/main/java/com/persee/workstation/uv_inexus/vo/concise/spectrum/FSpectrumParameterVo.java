package com.persee.workstation.uv_inexus.vo.concise.spectrum;

import lombok.Data;

import java.util.List;

@Data
public class FSpectrumParameterVo {
    // 光度模式(1.abs 2.T% 3.Es 4.Er)
    private String luminosity;
    // 光度模式名称
    private String luminosityName;
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
    // 阶梯校正名称
    private String ladderCorrectionName;
    // 扫描速度(0.慢速 1.中速 2.快速 3.极快)
    private String scanSpeed;
    // 扫描速度名称
    private String scanSpeedName;
    // 参考标准名称
    private String referenceStandardName;
    // 前处理参数
    private List<String> preProcessParameters;
    // 光谱带宽
    private String spectralBandwidth;
    // 响应时间
    private String responseTime;
    // 换灯波长
    private String changeLampWaveLength;
    // 能量灯(0.钨灯 1.氘灯 2.汞灯)
    private String energyLamp;
    //能量灯的名称
    private String energyLampName;
    // 增益
    private Integer gain;
}
