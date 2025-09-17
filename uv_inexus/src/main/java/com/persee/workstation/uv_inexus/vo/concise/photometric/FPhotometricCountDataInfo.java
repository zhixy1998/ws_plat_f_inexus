package com.persee.workstation.uv_inexus.vo.concise.photometric;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FPhotometricCountDataInfo {

    @ApiModelProperty("标题")
    private String resultName;

    @ApiModelProperty("标识")
    private String resultIdentity;

    @ApiModelProperty("单位")
    private String unit;

    @ApiModelProperty("公式")
    private String formulaText;

    @ApiModelProperty("数据")
    private String countValue;
}
