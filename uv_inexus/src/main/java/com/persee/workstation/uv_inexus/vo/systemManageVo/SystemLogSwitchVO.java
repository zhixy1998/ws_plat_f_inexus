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

package com.persee.workstation.uv_inexus.vo.systemManageVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemLogSwitchVO {
    @ApiModelProperty("设备使用统计总开关")
    private Boolean statUsageTotalSwitch;

    @ApiModelProperty("仪器使用率统计开关")
    private Boolean statUsageEnabled;

    @ApiModelProperty("用户使用率统计开关")
    private Boolean statUserEnabled;

    @ApiModelProperty("检测类型分项统计开关")
    private Boolean statTypeEnabled;

    @ApiModelProperty("检测项目分项统计开关")
    private Boolean statItemEnabled;

    @ApiModelProperty("检测任务详细信息开关")
    private Boolean statDetailEnabled;

    @ApiModelProperty("仪器使用记录开关")
    private Boolean statUsageRecordEnabled;
}
