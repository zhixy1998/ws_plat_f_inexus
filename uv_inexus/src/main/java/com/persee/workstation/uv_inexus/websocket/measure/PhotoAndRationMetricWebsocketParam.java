/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket.measure;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PhotoAndRationMetricWebsocketParam {


    @ApiModelProperty(value = "任务ID")
    private String taskId;
    @ApiModelProperty(value = "样本ID")
    private String sampleId;
    @ApiModelProperty(value = "设备序列号")
    private String deviceSerialNumber;
    @ApiModelProperty(value = "小数位数")
    private String digit;
    @ApiModelProperty(value = "当前用户")
    private String updateBy;
    @ApiModelProperty(value = "是否平均值")
    private Boolean isAvg;
    @ApiModelProperty(value = "是否标准差")
    private Boolean isSd;
    @ApiModelProperty(value = "是否RSD")
    private Boolean isRsd;
    @ApiModelProperty(value = "样品类型-定量用")
    private String sampleKind;

}
