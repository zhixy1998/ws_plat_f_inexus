package com.persee.workstation.uv_inexus.vo.systemManageVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ConfigVO {
    @ApiModelProperty("代理IP配置")
    private String ip;
    @ApiModelProperty("代理端口配置")
    private String port;
}
