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
public class DeviceMonitorVO {

    @ApiModelProperty(value = "数据集合")
    private List<DataNameValue> data;

    @ApiModelProperty("数据类型，区分要展示的数据，0-data,1-totalStatus")
    private String type;

    @ApiModelProperty("数据集合名称，eg:使用时长")
    private String dataName;

    @ApiModelProperty("状态")
    private String totalStatus;

}
