/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class DeviceInfoVO {

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("设备名称")
    private String deviceName;

    @ApiModelProperty("设备型号")
    private String deviceModelName;

    @ApiModelProperty("序列号")
    private String deviceSerialNumber;

    @ApiModelProperty(value = "开机状态", example = "0-未开机，1-已开机")
    private Integer isDevOpened;

    @ApiModelProperty(value = "自检状态", example = "0：未完成；1：自检成功； 2：自检失败； 3：自检进行中")
    private Integer isInited;

    @ApiModelProperty("激活状态")
    private Integer isActivated;

    @ApiModelProperty(value = "联机信息", example = "connect to device：ip=1.1.1.2 port=30500，连接中")
    private String connectMessage;

    @ApiModelProperty(value = "联机状态", example = "0-未连接，1-联机中，2-联机成功，3-联机失败")
    private Integer connectStatus;

    @ApiModelProperty("附加信息")
    private String appendMessage;

    @ApiModelProperty("初始化信息表")
    private List<DeviceInitInfo> intMessage;


}
