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
public class SystemPanelConfDTO {
    @ApiModelProperty("方法列表面板开关")
    private Boolean methodListSwitch;

    @ApiModelProperty("项目统计面板开关")
    private Boolean projectStatisticsSwitch;

    @ApiModelProperty("任务列表面板开关")
    private Boolean taskListSwitch;

    @ApiModelProperty(value = "用户")
    private String updateBy;

    @ApiModelProperty("启用快捷登录")
    private Boolean quickLoginSwitch;

    @ApiModelProperty("默认选中快捷登录，true-选中")
    private Boolean defaultQuickLogin;

    @ApiModelProperty("自动开机")
    private Boolean isAutoBoot;

    @ApiModelProperty("自动进入实验测试")
    private Boolean isAutoEnterExperiment;
}
