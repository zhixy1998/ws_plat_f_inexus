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

package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class FQueryDataDTO {
    private String taskId;
    @ApiModelProperty("是否显示被删除的数据")
    private Boolean allDisplay;
    @ApiModelProperty("小数位数")
    private Integer digit;
    @ApiModelProperty("要显示的数据类型：平均值/sd/rsd等")
    private List<String> option;

    @ApiModelProperty("仪器序列号")
    private String deviceSerialNumber;
 }
