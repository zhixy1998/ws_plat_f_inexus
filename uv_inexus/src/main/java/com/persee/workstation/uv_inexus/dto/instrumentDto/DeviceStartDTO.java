package com.persee.workstation.uv_inexus.dto.instrumentDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class DeviceStartDTO {

    @ApiModelProperty(value = "用户名", example = "admin")
    private String userName;

    @ApiModelProperty(value = "密码", example = "123456")
    private String password;

    @ApiModelProperty(value = "是否快捷登录", example = "0-普通登录，1-快捷登录")
    private String isFastLogin;

    @ApiModelProperty(value = "序列号")
    private String deviceSerialNumber;
}
