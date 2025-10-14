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
public class TaskCount {
    @ApiModelProperty("最近一周")
    private TaskCountByTime lastWeek;
    @ApiModelProperty("最近一月")
    private TaskCountByTime lastMonth;
    @ApiModelProperty("半年")
    private TaskCountByTime halfYear;
    @ApiModelProperty("最近一年")
    private TaskCountByTime lastYear;
    @ApiModelProperty("最近三年")
    private TaskCountByTime threeYears;
    @ApiModelProperty("所有")
    private TaskCountByTime all;
}
