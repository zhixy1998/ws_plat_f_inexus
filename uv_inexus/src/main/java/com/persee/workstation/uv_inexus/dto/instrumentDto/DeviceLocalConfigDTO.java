package com.persee.workstation.uv_inexus.dto.instrumentDto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("仪器基础配置类")
public class DeviceLocalConfigDTO {

    @ApiModelProperty("通讯端口")
    private Integer messagePort;

    @ApiModelProperty("附件设置")
    private Integer annexSetting;

    @ApiModelProperty("光谱带宽设置")
    private Double bandWidth;

    @ApiModelProperty("是否恢复出厂设置")
    private Boolean restoreFactorySetting;

    private String updateBy;
}
