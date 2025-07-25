package com.persee.workstation.uv_inexus.vo.systemManageVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemDataBaseVO {

    @ApiModelProperty("全局端口")
    private String globalPort;

    @ApiModelProperty("快捷登录开关")
    private Boolean fastLoginSwitch;

    @ApiModelProperty("主数据库ip")
    private String mainDatabaseIp;

    @ApiModelProperty("主数据库端口")
    private String mainDatabasePort;

    @ApiModelProperty("主数据库用户名")
    private String mainDatabaseUsername;

    @ApiModelProperty("主数据库密码")
    private String mainDatabasePassword;

    @ApiModelProperty("备数据库ip")
    private String spareDatabaseIp;

    @ApiModelProperty("备数据库端口")
    private String spareDatabasePort;

    @ApiModelProperty("备数据库用户名")
    private String spareDatabaseUsername;

    @ApiModelProperty("备数据库密码")
    private String spareDatabasePassword;
}
