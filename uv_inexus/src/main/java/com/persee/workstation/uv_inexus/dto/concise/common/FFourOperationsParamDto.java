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

package com.persee.workstation.uv_inexus.dto.concise.common;

import lombok.Data;

@Data
public class FFourOperationsParamDto {
    // 测量数据id1
    private String measureDataId1;
    // 测量数据id2
    private String measureDataId2;
    // 四则运算符(0.加,1.减,2.乘,3.除)
    private String arithmeticOperators;
    // 数据类型(0.数值,1.谱图)
    private String dataType;
    // 分析数据名称
    private String analysisDataName;
    // 任务id
    private String taskId;

}
