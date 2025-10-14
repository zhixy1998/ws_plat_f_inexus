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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("仪器基础配置类")
public class DeviceLocalConfigDTO {

    @ApiModelProperty("通讯端口")
    private Integer messagePort;

    @ApiModelProperty("附件设置")
    private Integer annexSetting;

    @ApiModelProperty("光谱带宽设置")
    private Double bandWidth;

    @ApiModelProperty("是否恢复出厂设置")
    private Boolean restoreFactorySetting;

    private String updateBy;
}
