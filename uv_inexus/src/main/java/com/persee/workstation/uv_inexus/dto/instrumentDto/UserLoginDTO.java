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

package com.persee.workstation.uv_inexus.dto.instrumentDto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("用户登录数据类")
public class UserLoginDTO {

    @ApiModelProperty(value = "用户名", example = "admin")
    private String userName;

    @ApiModelProperty(value = "密码", example = "px123456")
    private String password;

    @ApiModelProperty(value = "是否快捷登录, 0-普通登录，1-快捷登录")
    private Integer isFastLogin;

    @ApiModelProperty(value = "是否自动开机", example = "0-非自动开机，1-自动开机")
    private String isOnlyData;

}

