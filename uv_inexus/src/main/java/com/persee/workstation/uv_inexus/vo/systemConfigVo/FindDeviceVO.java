package com.persee.workstation.uv_inexus.vo.systemConfigVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FindDeviceVO {

    @ApiModelProperty("序列号")
    private String deviceSerialNumber;

    @ApiModelProperty(value = "自检状态值",example = "0-未自检， 1-自检中，2-自检成功，3-自检失败")
    private Integer initStatus;
}
