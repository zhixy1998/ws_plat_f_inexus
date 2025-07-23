package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class DeviceInitInfo {

    @ApiModelProperty("自检任务名")
    private String initProject;

    @ApiModelProperty("初始化条目序号")
    private Integer initTaskNum;

    @ApiModelProperty(value = "自检任务状态",example = "0-进行中，1-成功，2-失败")
    private Integer initStatus;
}
