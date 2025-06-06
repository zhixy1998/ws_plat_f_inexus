package com.persee.workstation.uv_inexus.dto.instrumentDto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("用户登录数据类")
public class UserLoginDTO {

    @ApiModelProperty(value = "用户名", example = "admin")
    private String userName;

    @ApiModelProperty(value = "密码", example = "123456")
    private String password;

    @ApiModelProperty(value = "是否快捷登录, 0-普通登录，1-快捷登录")
    private Integer isFastLogin;

    @ApiModelProperty(value = "是否自动开机", example = "0-非自动开机，1-自动开机")
    private String isOnlyData;

}

