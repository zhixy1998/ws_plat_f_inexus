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

package com.persee.workstation.uv_inexus.dto.instrumentDto;

import com.persee.workstation.uv_inexus.vo.instrumentVo.DataNameValue;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 仪器校准传参
 */
@Data
public class DeviceCalibrationDTO {

    @ApiModelProperty("校准参数")
    private List<DataNameValue> data;

    @ApiModelProperty("校准类型")
    private Integer calibrationType;

    @ApiModelProperty("校准名称")
    private String calibrationName;

    @ApiModelProperty("仪器序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("操作人")
    private String updateBy;
}
