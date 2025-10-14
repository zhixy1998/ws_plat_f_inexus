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

import java.util.List;

@Data
public class SampleWithMeasuresVo {
    private String sampleId;        // 样品ID
    private String title;           // 样品名称
    private String color;           // 样品颜色
    private String deleteType;      // 删除标识
    private String type;            // 类型
    private List<MeasureVo> children; // 样品下的测量数据列表
}