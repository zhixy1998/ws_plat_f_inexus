package com.persee.workstation.uv_inexus.vo.measurementVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class PhotometricTableHeadVO {
    @ApiModelProperty("波长")
    private List<String> wavelength;

    @ApiModelProperty("光度模式")
    private String luminosity;

    @ApiModelProperty("待计算结果")
    private List<PreCountResultParam> resultParams;
}
