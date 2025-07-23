package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class DeviceCalibrationVO {

    @ApiModelProperty("校准方法名")
    private String calibrationName;

    @ApiModelProperty("校准方法类型")
    private Integer calibrationType;

    @ApiModelProperty("校准方法参数")
    private List<DeviceCalibrationParams> params;
}
