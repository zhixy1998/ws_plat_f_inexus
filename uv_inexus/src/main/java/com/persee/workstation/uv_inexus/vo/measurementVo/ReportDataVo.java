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

@Data
public class ReportDataVo {
    // 名称
    private String name;
    // 波峰
    private List<String> peak;
    // 波谷
    private List<String> valley;
    // 标记点
    private List<String> markPoint;
    // 参数数据
    private List<String> parameterData;
    // 数据类型
    private String dataType;


}
