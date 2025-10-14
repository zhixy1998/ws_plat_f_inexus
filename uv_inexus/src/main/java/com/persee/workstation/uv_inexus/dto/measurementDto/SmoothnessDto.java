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
 * 平滑的参数
 */
@Data
public class SmoothnessDto {
    // 待处理数据id
    private String measureDataId;
    // 点数
    private String pointNum;
    // 系数
    private String coefficient;
    // 分析数据名称
    private String analysisDataName;
    // 任务id
    private String taskId;
}
