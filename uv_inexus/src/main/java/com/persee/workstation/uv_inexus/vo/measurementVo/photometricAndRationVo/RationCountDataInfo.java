package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class RationCountDataInfo {

    @ApiModelProperty("表头,公式的话需要标题+单位拼接")
    private String headName;

    @ApiModelProperty("标识")
    private String identity;

    @ApiModelProperty("值")
    private Double colValue;

}
