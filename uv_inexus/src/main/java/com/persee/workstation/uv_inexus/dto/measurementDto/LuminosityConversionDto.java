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

@Data
public class LuminosityConversionDto {
    // 待处理的数据id
    private String measureDataId;
    // 方法id
    private String methodId;
    // 数据类型(1.Abs<->T% 2.Abs<->R%)
    private String dataType;
    // 分析数据名称
    private String analysisDataName;
    // 任务id
    private String taskId;
}
