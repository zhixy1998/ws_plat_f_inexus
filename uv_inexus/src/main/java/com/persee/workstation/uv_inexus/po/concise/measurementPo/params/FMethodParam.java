package com.persee.workstation.uv_inexus.po.concise.measurementPo.params;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FMethodParam extends FPhotometricParameter {

    @ApiModelProperty("方法ID")
    private String methodId;

    @ApiModelProperty("方法名称")
    private String methodName;

    @ApiModelProperty("测量类型")
    private String measurementType;


}
