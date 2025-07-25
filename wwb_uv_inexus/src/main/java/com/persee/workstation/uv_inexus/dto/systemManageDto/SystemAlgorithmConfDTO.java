package com.persee.workstation.uv_inexus.dto.systemManageDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemAlgorithmConfDTO {

    @ApiModelProperty("透过率门限系数k")
    private String Transmittance;

    @ApiModelProperty("积分球门限系数k")
    private String IntegratingSphere;

    @ApiModelProperty("Abs门限系数k")
    private String Abs;

    @ApiModelProperty(value = "用户")
    private String updateBy;
}
