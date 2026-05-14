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
public class CalibrationTableData {

    @ApiModelProperty("序号")
    private String num;

    @ApiModelProperty("物质名称")
    private String name;

    @ApiModelProperty("光度模式")
    private String luminosity;

    @ApiModelProperty("标准波长")
    private String standLength;

    @ApiModelProperty("实际波长")
    private String realLength;

    @ApiModelProperty("增益")
    private String gain;

    @ApiModelProperty("灯类型")
    private String lamp;

    @ApiModelProperty("是否已经发送给设备")
    private Boolean isSendToDevice;

    @ApiModelProperty("物质标识")
    private String identifier;
}
