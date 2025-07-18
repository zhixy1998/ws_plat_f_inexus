package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricParameter;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class MethodParam extends PhotometricParameter {

    @ApiModelProperty("方法ID")
    private String methodId;

    @ApiModelProperty("方法名称")
    private String methodName;

    @ApiModelProperty("测量类型")
    private String measurementType;

//    @ApiModelProperty("光度测量参数")
//    @JsonInclude(JsonInclude.Include.NON_NULL)
//    private PhotometricParameter photometricParameter;
//
//    @ApiModelProperty("定量测量参数")
//    @JsonInclude(JsonInclude.Include.NON_NULL)
//    private RationParameter rationParam;
}
