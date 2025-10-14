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

package com.persee.workstation.uv_inexus.vo.measurementVo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SampleListVo {
    // 标题
    private String title;
    // 测量id
    private String analysisId;
    // 样品id
    private String sampleId;
    // 分析编号
    private Integer analysisNumber;
    // 颜色
    private String color;
    // 数据
    private List<Map<String, Object>> data;
    // 类型(0.测量数据 1.分析数据)
    private String type;
    // deleteType
    private String deleteType;
}
