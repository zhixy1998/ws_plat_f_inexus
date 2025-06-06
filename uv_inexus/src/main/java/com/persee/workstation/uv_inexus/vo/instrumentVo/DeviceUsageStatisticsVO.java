package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class DeviceUsageStatisticsVO {

    @ApiModelProperty("具体数据信息")
    private List<DataNameValue> data;

    @ApiModelProperty("数据类型，eg.用户使用率统计")
    private String type;

    @ApiModelProperty("数据名称，eg:使用时长")
    private String dataName;

    @ApiModelProperty("字段待用，后续需要再加")
    private String demo1;
}
