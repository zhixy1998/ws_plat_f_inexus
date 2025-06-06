package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class DeviceInfoVO {

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("序列号")
    private String deviceSerialNumber;

    @ApiModelProperty(value = "开机状态", example = "0-未开机，2-已开机")
    private String isDevOpened;

    @ApiModelProperty(value = "自检状态", example = "0：未完成；1：自检成功； 2：自检失败； 3：自检进行中")
    private String isInited;

    @ApiModelProperty(value = "联机信息", example = "connect to device：ip=1.1.1.2 port=30500，连接中")
    private String connectMessage;

    @ApiModelProperty("附加信息")
    private String appendMessage;

    @ApiModelProperty("初始化信息表")
    private List<DeviceInitInfo> intMessage;


}
