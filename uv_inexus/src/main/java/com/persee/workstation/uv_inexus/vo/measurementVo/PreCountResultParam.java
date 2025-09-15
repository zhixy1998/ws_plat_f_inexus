package com.persee.workstation.uv_inexus.vo.measurementVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PreCountResultParam {

    @ApiModelProperty("标题")
    private String resultName;

    @ApiModelProperty("标识")
    private String resultIdentity;

    @ApiModelProperty("单位")
    private String unit;

    @ApiModelProperty("公式")
    private String formulaText;

    @ApiModelProperty("参数名")
    private String param;
}
