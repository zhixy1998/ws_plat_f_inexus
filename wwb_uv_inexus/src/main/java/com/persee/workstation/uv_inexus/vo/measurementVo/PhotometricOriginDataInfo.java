package com.persee.workstation.uv_inexus.vo.measurementVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PhotometricOriginDataInfo {

    @ApiModelProperty("光度模式")
    private String mode;

    @ApiModelProperty("波段")
    private String wavelength;

    @ApiModelProperty("abs的数据值")
    private String absValue;

    @ApiModelProperty("T%的值")
    private String tPercentValue;

}
