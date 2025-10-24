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

package com.persee.workstation.uv_inexus.po.guide.systemManagePo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("系统面板数据类")
@TableName(value = "t_system_panel_conf")
public class SystemPanelConfPO extends BasePO {

    @ApiModelProperty("方法列表面板开关")
    @TableField(value = "method_list_switch")
    private Boolean methodListSwitch;

    @ApiModelProperty("项目统计面板开关")
    @TableField(value = "project_statistics_switch")
    private Boolean projectStatisticsSwitch;

    @ApiModelProperty("任务列表面板开关")
    @TableField(value = "task_list_switch")
    private Boolean taskListSwitch;

    @ApiModelProperty("面板顺序")
    @TableField(value = "panel_order")
    private String panelOrder;

    @ApiModelProperty("启用快捷登录")
    @TableField(value = "quick_login_switch")
    private Boolean quickLoginSwitch;

    @ApiModelProperty("默认选中快捷登录，true-选中")
    @TableField(value = "default_quick_login")
    private Boolean defaultQuickLogin;

    @ApiModelProperty("自动开机")
    @TableField(value = "is_auto_boot")
    private Boolean isAutoBoot;

    @ApiModelProperty("自动进入实验测试")
    @TableField(value = "is_auto_enter_experiment")
    private Boolean isAutoEnterExperiment;

}
