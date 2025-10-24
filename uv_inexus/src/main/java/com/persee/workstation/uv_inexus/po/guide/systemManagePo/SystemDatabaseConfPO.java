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
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "t_system_database")
public class SystemDatabaseConfPO extends BasePO {

    @ApiModelProperty("全局端口")
    @TableField(value = "global_port")
    private String globalPort;

    @ApiModelProperty("默认进入经典模式")
    @TableField(value = "default_enter_classic")
    private Boolean defaultEnterClassic;

    @ApiModelProperty("主数据库ip")
    @TableField(value = "main_database_ip")
    private String mainDatabaseIp;

    @ApiModelProperty("主数据库端口")
    @TableField(value = "main_database_port")
    private String mainDatabasePort;

    @ApiModelProperty("主数据库用户名")
    @TableField(value = "main_database_username")
    private String mainDatabaseUsername;

    @ApiModelProperty("主数据库密码")
    @TableField(value = "main_database_password")
    private String mainDatabasePassword;

    @ApiModelProperty("备数据库ip")
    @TableField(value = "spare_database_ip")
    private String spareDatabaseIp;

    @ApiModelProperty("备数据库端口")
    @TableField(value = "spare_database_port")
    private String spareDatabasePort;

    @ApiModelProperty("备数据库用户名")
    @TableField(value = "spare_database_username")
    private String spareDatabaseUsername;

    @ApiModelProperty("备数据库密码")
    @TableField(value = "spare_database_password")
    private String spareDatabasePassword;


}
