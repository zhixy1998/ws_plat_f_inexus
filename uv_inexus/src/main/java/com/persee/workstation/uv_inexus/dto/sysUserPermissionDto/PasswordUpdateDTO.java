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

import javax.validation.constraints.NotBlank;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 修改密码数据传输对象
 */
@Data
@ApiModel(description = "修改密码DTO")
public class PasswordUpdateDTO {

    @ApiModelProperty(value = "用户ID（必传，关联待修改密码的用户）", required = true)
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @ApiModelProperty(value = "旧密码（必传，用于身份校验）", required = true)
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @ApiModelProperty(value = "新密码（必传，格式要求：8-16位，包含字母和数字）", required = true)
    @NotBlank(message = "新密码不能为空")
    // @Length(min = 8, max = 16, message = "新密码长度需在8-16位之间")
    // @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$", message = "新密码需同时包含字母和数字")
    private String newPassword;

    @ApiModelProperty(value = "新密码确认（必传，需与新密码一致）", required = true)
    @NotBlank(message = "请确认新密码")
    private String confirmNewPassword;
}