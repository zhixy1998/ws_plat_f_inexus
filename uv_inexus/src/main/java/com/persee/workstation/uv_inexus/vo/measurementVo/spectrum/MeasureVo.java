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

package com.persee.workstation.uv_inexus.vo.measurementVo.spectrum;

import lombok.Data;

@Data
public class MeasureVo {
    private String measureId;       // 测量数据ID
    private String title;           // 测量名称(序号)
    private Object data;            // 测量结果
    private String serialNumber;    // 序列号
    private String type;            // 测量类型
}
