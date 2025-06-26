package com.persee.workstation.uv_inexus.vo.userManageVo;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("用户登录后返回给前端的信息")
public class LoginUserInfoVO {

    @ApiModelProperty("登录的token")
    private String token;

    @ApiModelProperty("用户名")
    private String userName;
}
