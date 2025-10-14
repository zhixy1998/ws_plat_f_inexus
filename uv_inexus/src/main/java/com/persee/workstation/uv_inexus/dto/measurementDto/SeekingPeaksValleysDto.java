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
 * 寻找峰谷差参数
 */
@Data
public class SeekingPeaksValleysDto {
    // 参数id(测量id和分析id共用一个)
    private String dataId;
    // 峰谷差阈值
    private Double height;
    // 峰间距
    private String distance;
}
