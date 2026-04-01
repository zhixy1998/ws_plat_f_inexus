/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DeviceMeasureVO {

    @ApiModelProperty("光度模式")
    private List<DeviceConfParam> luminosity;

    @ApiModelProperty("灯类型，钨灯，氘灯")
    private List<DeviceConfParam> lampType;

    @ApiModelProperty("连接方式，0-串口，1-网口")
    private Integer connectMode;

    @ApiModelProperty("增益")
    private List<Integer> gain;

    @ApiModelProperty("响应时间")
    private List<String> responseTime;

    @ApiModelProperty("扫描速度")
    private List<DeviceConfParam> scanSpeed;

    @ApiModelProperty("最小波长")
    private String minWavelength;

    @ApiModelProperty("最小换灯波长")
    private String minLampWavelength;

    @ApiModelProperty("最大波长")
    private String maxWavelength;

    @ApiModelProperty("最大换灯波长")
    private String maxLampWavelength;

    @ApiModelProperty("扫描间隔")
    private List<String> scanInterval;

    @ApiModelProperty("光谱带宽")
    private List<String> bandWidth;

    @ApiModelProperty("滤色片波长点")
    private List<Integer> filterWavelength;
}
