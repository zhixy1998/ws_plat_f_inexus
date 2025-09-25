package com.persee.workstation.uv_inexus.vo.measurementVo.spectrum;

import lombok.Data;

// 光谱报告
@Data
public class SpectralReport {
// 测量
    // 起始波长
    private String startWaveLength;
    // 结束波长
    private String endWaveLength;
    // 扫描速度
    private String scanSpeed;
    // 时间间隔
    private String timeInterval;
    // 检测开始时间
    private String detectionStartTime;
    // 检测结束时间
    private String detectionEndTime;
    // 阶梯校正
    private String ladderCorrection;
    // 重复模式(0.不重复 1.单池重复)
    private String repetitionPattern;
    // 重复次数(如果为不重复则该字段没有值)
    private String repetitionTime;
    // 波长间隔
    private String waveLengthInterval;

// 仪器
    // 仪器编号
    private String deviceSerialNumber;
    // 光度模式
    private String LuminosityMode;
    // 响应时间
    private String responseTime;
    // 光谱带宽
    private String spectralBandwidth;
    // 换灯波长
    private String changeLampWaveLength;
    // 能量灯(0.钨灯 1.氘灯 2.汞灯)
    private String energyLamp;
    // 增益
    private Integer gain;

// 其他
    // 项目名称
    private String projectName;
    // 用户名
    private String userBy;
    // 样品名称
    private String sampleName;
    // 样品编号
    private String sampleNumber;
    // 光谱扫描图片
    private byte[] spectrumScanImage;
    // 方法名称
    private String methodName;
    // 任务名称(数据文件)
    private String taskName;
}
