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

package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 用户登录请求DTO
 */
@Data
@ApiModel("用户登录请求参数")
public class UserLoginDTO {

    @ApiModelProperty(value = "用户名", example = "admin")
    @NotBlank(message = "用户名不能为空")
    private String userName;

    @ApiModelProperty(value = "密码",  example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @ApiModelProperty(value = "是否快捷登录（1-是，0-否）", example = "0")
    private Integer isFastLogin;

    /**
     * 快捷登录参数补全（默认用户）
     * 参照原有逻辑：快捷登录时自动填充默认用户名/密码
     */
    public void completeFastLogin() {
        if (this.isFastLogin != null && this.isFastLogin == 1) {
            this.userName = "admin"; // 建议在Constants类定义默认值，此处先硬编码示例
            this.password = "123456";
        }
    }
}