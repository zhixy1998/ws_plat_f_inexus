/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.vo.measurementVo.timeSweep;
// 时间报告数据

import lombok.Data;

@Data
public class TimeSweepReport {
// 测量
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
    // 检测开始时间
    private String detectionStartTime;
    // 检测结束时间
    private String detectionEndTime;
    // 多联池模式(0.顺序单池连续采样,1.循环多池交替采样)
    private String multiPoolMode;

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
    // 时间扫描图片
    private byte[] timeSweepScanImage;
    // 方法名称
    private String methodName;
    // 任务名称(数据文件)
    private String taskName;
}
