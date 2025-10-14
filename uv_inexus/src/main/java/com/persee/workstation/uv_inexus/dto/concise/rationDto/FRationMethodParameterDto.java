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

package com.persee.workstation.uv_inexus.dto.concise.rationDto;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRationParameter;
import lombok.Data;

@Data
public class FRationMethodParameterDto extends FRationParameter {
    // 项目ID
    private String projectId;
    // 方法id
    private String methodId;
    // 测量类型
    private String measureType;
    // 方法名称
    private String methodName;
    // 是否共享
    private String isShare;
}
