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

import com.persee.workstation.uv_inexus.vo.concise.spectrum.FSpectrumParameterVo;
import lombok.Data;

@Data
public class FUseMethodDto {
    // 方法id
    private String methodId;
    // 方法id2
    private String methodId2;
    // 用户名
    private String updateBy;
    // 测量类型
    private String measureType;
    // 测量方式 (0. 向导数据库,1. 简洁数据库)
    private String type;
    // 方法参数
    private FSpectrumParameterVo parameter;
    // 序列号
    private String deviceSerialNumber;
}
