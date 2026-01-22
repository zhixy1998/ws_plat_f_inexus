package com.persee.workstation.uv_inexus.websocket.measure.param;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FTimeNewWebSocketParam {
    @ApiModelProperty(value = "任务ID")
    private String taskId;
    @ApiModelProperty(value = "样品id")
    private String sampleId;
    @ApiModelProperty(value = "用户名")
    private String updateBy;
    @ApiModelProperty(value = "设备序列号")
    private String deviceSerialNumber;
    @ApiModelProperty(value = "小数位数")
    private Integer digit;
}
