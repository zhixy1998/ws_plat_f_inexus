package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class MethodChangeVO {

    @ApiModelProperty("当前方法id")
    private String currentMethodId;

    @ApiModelProperty("参数是否有改变，true-改变，false-未改变")
    private Boolean paramChange;

    @ApiModelProperty("提示信息")
    private String message;

    @ApiModelProperty("清除数据范围")
    private String clearDataRange;
}
