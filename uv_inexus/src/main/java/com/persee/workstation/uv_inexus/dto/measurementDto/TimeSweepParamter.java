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

package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

/**
 * 时间扫描方法参数
 */
@Data
public class TimeSweepParamter extends MeasurePublicParameters{
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
