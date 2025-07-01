package com.persee.workstation.uv_inexus.vo.systemManageVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemAlgorithmConfVO {

    @ApiModelProperty("透过率门限系数k")
    private String Transmittance;

    @ApiModelProperty("积分球门限系数k")
    private String IntegratingSphere;

    @ApiModelProperty("Abs门限系数k")
    private String Abs;
}
