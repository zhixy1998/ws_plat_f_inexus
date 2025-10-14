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
public class SystemDataBaseVO {

    @ApiModelProperty("全局端口")
    private String globalPort;

    @ApiModelProperty("快捷登录开关")
    private Boolean fastLoginSwitch;

    @ApiModelProperty("主数据库ip")
    private String mainDatabaseIp;

    @ApiModelProperty("主数据库端口")
    private String mainDatabasePort;

    @ApiModelProperty("主数据库用户名")
    private String mainDatabaseUsername;

    @ApiModelProperty("主数据库密码")
    private String mainDatabasePassword;

    @ApiModelProperty("备数据库ip")
    private String spareDatabaseIp;

    @ApiModelProperty("备数据库端口")
    private String spareDatabasePort;

    @ApiModelProperty("备数据库用户名")
    private String spareDatabaseUsername;

    @ApiModelProperty("备数据库密码")
    private String spareDatabasePassword;
}
