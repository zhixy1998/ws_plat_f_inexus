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

@Data
public class DeviceComponentMonitorVO {

    @ApiModelProperty("器件类型")
    private Integer componentType;

    @ApiModelProperty("使用时长，单位 h ")
    private Integer usedTime;

    @ApiModelProperty("总时长，单位 h")
    private String totalTime;

    @ApiModelProperty("仪器健康状态，暂定，后续看前端还是后端计算状态")
    private Integer componentStatus;
}
