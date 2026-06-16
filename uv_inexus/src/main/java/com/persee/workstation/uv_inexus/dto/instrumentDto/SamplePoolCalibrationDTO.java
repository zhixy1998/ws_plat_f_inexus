/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.instrumentDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class SamplePoolCalibrationDTO {

    private String deviceSerialNumber;

    private String taskId;

    private String updateBy;

    private String measureType;

    @ApiModelProperty(value = "光度模式")
    private String luminosity;

    @ApiModelProperty(value = "是否是经典模式？true-是，false-否")
    private Boolean isConcise;

    @ApiModelProperty(value = "波长")
    List<String> waveLengths;

}
