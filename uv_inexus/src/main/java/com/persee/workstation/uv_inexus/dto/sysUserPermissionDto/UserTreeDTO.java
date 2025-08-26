package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 用户树形结构DTO
 */
@Data
@ApiModel("用户树形结构")
public class UserTreeDTO {
    @ApiModelProperty("用户ID")
    private String id;

    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("真实姓名")
    private String realName;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("用户状态（1-启用，0-禁用）")
    private Integer status;
}
