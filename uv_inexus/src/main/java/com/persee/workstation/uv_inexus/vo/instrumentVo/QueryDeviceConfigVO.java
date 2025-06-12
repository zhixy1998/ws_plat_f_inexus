package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("仪器管理配置")
public class QueryDeviceConfigVO {
    @ApiModelProperty("通讯端口")
    private Integer messagePort;

    @ApiModelProperty("附件设置")
    private Integer annexSetting;

    @ApiModelProperty("光谱带宽设置")
    private Double bandWidth;

}
