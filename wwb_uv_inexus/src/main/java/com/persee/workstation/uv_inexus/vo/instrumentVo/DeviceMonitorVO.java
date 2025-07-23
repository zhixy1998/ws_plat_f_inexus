package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class DeviceMonitorVO {

    @ApiModelProperty(value = "数据集合")
    private List<DataNameValue> data;

    @ApiModelProperty("数据类型，区分要展示的数据，0-data,1-totalStatus")
    private String type;

    @ApiModelProperty("数据集合名称，eg:使用时长")
    private String dataName;

    @ApiModelProperty("状态")
    private String totalStatus;

}
