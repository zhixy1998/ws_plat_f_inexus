package com.persee.workstation.uv_inexus.dto.instrumentDto;

import com.persee.workstation.uv_inexus.vo.instrumentVo.DataNameValue;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 仪器校准传参
 */
@Data
public class DeviceCalibrationDTO {

    @ApiModelProperty("校准参数")
    private List<DataNameValue> data;

    @ApiModelProperty("校准类型")
    private Integer calibrationType;

    @ApiModelProperty("校准名称")
    private String calibrationName;

    @ApiModelProperty("仪器序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("操作人")
    private String updateBy;
}
