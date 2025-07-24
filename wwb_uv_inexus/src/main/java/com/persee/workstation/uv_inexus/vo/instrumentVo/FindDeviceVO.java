package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FindDeviceVO {

    @ApiModelProperty("序列号")
    private String deviceSerialNumber;

    @ApiModelProperty(value = "自检状态值",example = "0：未完成；1：自检成功； 2：自检失败； 3：自检进行中")
    private Integer isInited;

    @ApiModelProperty(value = "开机状态",example = "0-无响应，1-已开机，2-开机中，3-软关机")
    private Integer isDevOpened;

    @ApiModelProperty("机器ip地址")
    private String ip;
}
