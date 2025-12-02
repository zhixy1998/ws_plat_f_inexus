/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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