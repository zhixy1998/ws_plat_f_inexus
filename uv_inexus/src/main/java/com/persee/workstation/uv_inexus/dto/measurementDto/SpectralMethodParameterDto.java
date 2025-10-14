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
public class SpectralMethodParameterDto extends SpectrumParameter{
    // 项目ID
    private String projectId;
    // 测量类型
    private String measureType;
    // 方法ID
    private String methodId;
    // 方法名称
    private String methodName;
    // 用户名
    private String updateBy;
    // 是否共享
    private String isShare;
}