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

@Data
public class FindDeviceVO {

    @ApiModelProperty("序列号")
    private String deviceSerialNumber;

    @ApiModelProperty(value = "自检状态值",example = "0：未完成；1：自检成功； 2：自检失败； 3：自检进行中")
    private Integer isInited;

    @ApiModelProperty(value = "开机状态",example = "0-无响应，1-已开机，2-开机中，3-软关机")
    private Integer isDevOpened;

    @ApiModelProperty("激活状态，0-未激活，1-激活")
    private Integer isActivated;

    @ApiModelProperty("机器ip地址")
    private String ip;

    @ApiModelProperty("附件类型")
    private Integer attachmentType;

    @ApiModelProperty("附件种类")
    private Integer attachmentKind;

    @ApiModelProperty("附件名称")
    private String attachmentName;

    @ApiModelProperty("当前样品池位置")
    private Integer currentPoolNum;

    @ApiModelProperty("总池数")
    private Integer totalPoolNum;

    @ApiModelProperty("仪器名称")
    private String deviceName;

    @ApiModelProperty("仪器型号名称")
    private String deviceModelName;

    @ApiModelProperty("计算公式")
    private String linearSegment;
}
