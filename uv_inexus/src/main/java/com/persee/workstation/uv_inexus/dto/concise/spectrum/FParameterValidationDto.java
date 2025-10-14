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

package com.persee.workstation.uv_inexus.dto.concise.spectrum;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * 方法参数
 */
@Data
public class FParameterValidationDto {
    // 方法id
    private String methodId;
    // 测量类型，光度测量,定量测量,光谱扫描,时间扫描
    private String measureType;
    // 前端传的参数，先用通用 JSON 接收
    private JSONObject parameter;
}
