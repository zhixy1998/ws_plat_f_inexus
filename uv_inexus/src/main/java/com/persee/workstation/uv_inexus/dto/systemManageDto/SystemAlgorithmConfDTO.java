/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.systemManageDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemAlgorithmConfDTO {

    @ApiModelProperty("透过率门限系数k")
    private String Transmittance;

    @ApiModelProperty("积分球门限系数k")
    private String IntegratingSphere;

    @ApiModelProperty("Abs门限系数k")
    private String Abs;

    @ApiModelProperty("峰间距")
    private String peakInterval;

    @ApiModelProperty("峰谷差")
    private String peakValleyDiff;

    @ApiModelProperty(value = "用户")
    private String updateBy;
}
