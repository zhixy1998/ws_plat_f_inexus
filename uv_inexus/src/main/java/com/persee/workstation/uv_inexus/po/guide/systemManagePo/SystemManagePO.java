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

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("系统管理")
@TableName("t_system_manage")
public class SystemManagePO extends BasePO {

    @ApiModelProperty("语言")
    @TableField(value = "language")
    private String language;

    @ApiModelProperty("远程控制")
    @TableField(value = "remote_control")
    private Boolean remoteControl;

    @ApiModelProperty("远程控制数量上限")
    @TableField(value = "remote_control_num")
    private Integer remoteControlNum;

    @ApiModelProperty("远程数据访问")
    @TableField(value = "remote_data_access")
    private Boolean remoteDataAccess;

    @ApiModelProperty("远程数据访问用户上限")
    @TableField(value = "remote_data_access_num")
    private Integer remoteDataAccessNum;

    @ApiModelProperty("仪器使用模式, 1-独占，2-共享")
    @TableField(value = "device_use_mode")
    private String deviceUseMode;

    @ApiModelProperty("仪器资源自动释放时间")
    @TableField(value = "device_resource_auto_release_time")
    private Integer deviceResourceAutoReleaseTime;

    @ApiModelProperty("物联代理IP配置")
    @TableField(value = "ip_configuration")
    private String ipConfiguration;

    @ApiModelProperty("物联代理端口配置")
    @TableField(value = "port_configuration")
    private String portConfiguration;
}
