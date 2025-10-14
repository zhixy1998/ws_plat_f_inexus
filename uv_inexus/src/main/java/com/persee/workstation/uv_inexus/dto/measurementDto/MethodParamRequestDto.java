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

import java.util.List;
import java.util.Map;

@Data
public class MethodParamRequestDto {
    private String methodName;  // 测量方法名称
    private String methodType; // 测量类型
    private String projectId; // 项目id
    private List<String> pretreatmentIds; // 前处理的多个id
    private String updateBy; // 修改人
    private Map<String, Object> params; // 不定参数
}