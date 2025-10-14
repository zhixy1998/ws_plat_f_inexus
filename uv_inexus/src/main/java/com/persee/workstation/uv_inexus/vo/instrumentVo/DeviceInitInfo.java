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
public class DeviceInitInfo {

    @ApiModelProperty("自检任务名")
    private String initProject;

    @ApiModelProperty("初始化条目序号")
    private Integer initTaskNum;

    @ApiModelProperty(value = "自检任务状态",example = "0-进行中，1-成功，2-失败")
    private Integer initStatus;
}
