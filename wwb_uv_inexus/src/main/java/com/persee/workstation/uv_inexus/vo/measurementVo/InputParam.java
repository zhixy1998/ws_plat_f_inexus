package com.persee.workstation.uv_inexus.vo.measurementVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class InputParam {

    @ApiModelProperty("标题")
    private String paramName;

    @ApiModelProperty("标识")
    private String resultIdentity;

    @ApiModelProperty("单位")
    private String unit;
}
