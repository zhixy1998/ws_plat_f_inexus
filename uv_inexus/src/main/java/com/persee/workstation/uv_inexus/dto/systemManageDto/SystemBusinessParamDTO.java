/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.systemManageDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemBusinessParamDTO {

    @ApiModelProperty("标准曲线配置")
    private String standardCurveConf;

    @ApiModelProperty("显示已删除")
    private Boolean showDeleted;

    @ApiModelProperty("显示样品测量时间")
    private Boolean showMeasureTime;

    @ApiModelProperty("自动保存开关")
    private Boolean autoSaveSwitch;

    @ApiModelProperty("自动保存间隔")
    private String autoSaveTime;

    @ApiModelProperty("自动登出开关")
    private Boolean autoLeaveSwitch;

    @ApiModelProperty("自动登出时间")
    private String autoLeaveTime;

    private String filePath;

    @ApiModelProperty("用户")
    private String updateBy;

    @ApiModelProperty("Abs显示位数")
    private Integer absDigits;

    @ApiModelProperty("T%、R%显示位数")
    @JsonProperty("tDigits")
    private Integer tDigits;

    @ApiModelProperty("浓度显示位数")
    private Integer concentrationDigits;

    @ApiModelProperty("E显示位数")
    @JsonProperty("eDigits")
    private Integer eDigits;

    @ApiModelProperty("SD和RSD显示位数")
    private Integer sdDigits;

    @ApiModelProperty("K显示位数")
    @JsonProperty("kDigits")
    private Integer kDigits;

    @ApiModelProperty("R/R平方显示位数")
    @JsonProperty("rSquareDigits")
    private Integer rSquareDigits;

    @ApiModelProperty("浓度负值是否显示未检出")
    private Boolean isNegativeShow;
}
