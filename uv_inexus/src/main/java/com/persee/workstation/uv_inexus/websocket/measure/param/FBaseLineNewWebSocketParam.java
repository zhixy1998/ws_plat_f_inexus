package com.persee.workstation.uv_inexus.websocket.measure.param;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FBaseLineNewWebSocketParam {
    @ApiModelProperty(value = "任务ID")
    private String taskId;
    @ApiModelProperty(value = "方法ID")
    private String methodId;
    @ApiModelProperty(value = "设备序列号")
    private String deviceSerialNumber;
}
