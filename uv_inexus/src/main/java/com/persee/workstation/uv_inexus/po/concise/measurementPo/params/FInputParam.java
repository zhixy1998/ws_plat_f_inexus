package com.persee.workstation.uv_inexus.po.concise.measurementPo.params;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FInputParam {

    @ApiModelProperty("标题")
    private String paramName;

    @ApiModelProperty("标识")
    private String resultIdentity;

    @ApiModelProperty("单位")
    private String unit;
}
