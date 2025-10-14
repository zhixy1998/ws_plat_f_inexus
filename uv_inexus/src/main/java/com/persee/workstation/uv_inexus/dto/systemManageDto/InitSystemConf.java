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

package com.persee.workstation.uv_inexus.dto.systemManageDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class InitSystemConf {

    @ApiModelProperty("自动进入实验测试开关")
    private Boolean autoMeasure;

    @ApiModelProperty("快捷登录开关")
    private Boolean fastLoginSwitch;

    @ApiModelProperty("自动开机开关")
    private Boolean autoStart;

    @ApiModelProperty("经典模式或者向导模式开关  1：经典模式 2：向导模式")
    private Integer modeType;


}
