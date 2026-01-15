package com.persee.workstation.uv_inexus.vo.measurementVo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("样品信息")
public class SampleInfoVO {

    @ApiModelProperty("样品ID")
    private String sampleId;

    @ApiModelProperty("样品编号")
    private String sampleNumber;

    @ApiModelProperty("样品名称")
    private String sampleName;

    @ApiModelProperty("样品位置")
    private String samplePosition;
}