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

package com.persee.workstation.uv_inexus.po.concise.measurementPo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class TaskCountByTime {
    @ApiModelProperty("完成数")
    Long finishedCount;
    @ApiModelProperty("未完成数")
    Long unFinishedCount;

}
