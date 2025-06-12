package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class DeviceCalibrationParams {

    @ApiModelProperty("id")
    private String id;

    @ApiModelProperty("参数名称")
    private String paramName;

    @ApiModelProperty("参数--英文名")
    private String param;

    @ApiModelProperty("参数类型：输入框或下拉")
    private Integer paramType;

    @ApiModelProperty("上次设置的值")
    private String lastValue;

    @ApiModelProperty("下拉参数")
    private List<DataNameValue> data;
}
