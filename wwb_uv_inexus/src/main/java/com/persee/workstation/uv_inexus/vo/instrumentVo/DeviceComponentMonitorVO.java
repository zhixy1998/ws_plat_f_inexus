package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class DeviceComponentMonitorVO {

    @ApiModelProperty("器件类型")
    private Integer componentType;

    @ApiModelProperty("使用时长，单位 h ")
    private Integer usedTime;

    @ApiModelProperty("总时长，单位 h")
    private String totalTime;

    @ApiModelProperty("仪器健康状态，暂定，后续看前端还是后端计算状态")
    private Integer componentStatus;
}
