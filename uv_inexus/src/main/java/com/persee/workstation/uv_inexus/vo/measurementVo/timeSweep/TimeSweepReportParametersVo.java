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

import lombok.Data;

// 时间报告参数Vo类
@Data
public class TimeSweepReportParametersVo {
    // 重叠图片
    private byte[] overlapImage;
    // 平铺图片
    private byte[] mosaicImage;
    // 3D 图片
    private byte[] threeDImage;
    // 光谱数据
    private TimeSweepReport data;
}
