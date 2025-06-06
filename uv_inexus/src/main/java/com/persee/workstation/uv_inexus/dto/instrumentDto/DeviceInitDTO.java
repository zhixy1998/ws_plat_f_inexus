package com.persee.workstation.uv_inexus.dto.instrumentDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class DeviceInitDTO {

    @ApiModelProperty(value = "用户名", example = "admin")
    private String userName;

    @ApiModelProperty(value = "密码", example = "123456")
    private String password;

    @ApiModelProperty(value = "是否自动开机", example = "0-非自动开机，1-自动开机")
    private String isOnlyData;

    @ApiModelProperty(value = "序列号")
    private String deviceSerialNumber;
}
