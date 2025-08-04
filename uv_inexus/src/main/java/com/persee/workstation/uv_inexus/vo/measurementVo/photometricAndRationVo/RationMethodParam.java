package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import com.persee.workstation.uv_inexus.dto.measurementDto.RationParameter;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class RationMethodParam extends RationParameter {
    @ApiModelProperty("方法ID")
    private String methodId;

    @ApiModelProperty("方法名称")
    private String methodName;

    @ApiModelProperty("测量类型")
    private String measurementType;
}
