package com.persee.workstation.uv_inexus.vo.sysUserPermissionVo;

import java.time.LocalDateTime;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 用户登录响应VO
 */
@Data
@ApiModel("用户登录响应信息")
public class LoginUserInfoVO {

    @ApiModelProperty("用户ID")
    private String id;

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

    @ApiModelProperty("最后登录时间")
    private LocalDateTime lastLoginTime;

    @ApiModelProperty("登录令牌（JWT）")
    private String token;
}