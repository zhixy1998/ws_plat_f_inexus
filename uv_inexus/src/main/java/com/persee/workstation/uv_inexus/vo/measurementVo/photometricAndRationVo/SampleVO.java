package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SampleVO {

    @ApiModelProperty("样本ID")
    private String sampleId;

    @ApiModelProperty("样本编号")
    private String sampleNumber;

    @ApiModelProperty("样本名称")
    private String sampleName;

    @ApiModelProperty("样本类型")
    private String sampleType;

    @ApiModelProperty("稀释比")
    private String dilutionRatio;

    @ApiModelProperty("样品位置")
    private String sampleLocation;

    // 取样量
    @ApiModelProperty("取样量")
    private String sampleVolume;

}
