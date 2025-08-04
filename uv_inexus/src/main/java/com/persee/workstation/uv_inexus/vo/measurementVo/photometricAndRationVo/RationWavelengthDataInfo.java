package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class RationWavelengthDataInfo {

    @ApiModelProperty("波段")
    private String wavelength;

    @ApiModelProperty("数据标识")
    private String identity;

    @ApiModelProperty("abs的数据值")
    private String absValue;

    @ApiModelProperty("T%的值")
    private String tPercentValue;

}
