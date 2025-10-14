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

package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class DeviceCalibrationParams {

    @ApiModelProperty("id")
    private String id;

    @ApiModelProperty("参数名称")
    private String paramName;

    @ApiModelProperty("参数--英文名")
    private String param;

    @ApiModelProperty("参数类型：输入框或下拉")
    private Integer paramType;

    @ApiModelProperty("上次设置的值")
    private String lastValue;

    @ApiModelProperty("下拉参数")
    private List<DataNameValue> data;
}
