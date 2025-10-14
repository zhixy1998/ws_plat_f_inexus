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

package com.persee.workstation.uv_inexus.vo;

import lombok.Data;

@Data
public class ScanParamInfo {

    // 光度模式
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
    // 光谱带宽
    private String spectralBandwidth;
    // 响应时间
    private String responseTime;
    // 换灯波长
    private String changeLampWaveLength;
    // 能量灯
    private String energyLamp;
    // 增益
    private String gain;

}
