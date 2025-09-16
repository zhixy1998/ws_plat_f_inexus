package com.persee.workstation.uv_inexus.po.concise.measurementPo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class TaskCountByTime {
    @ApiModelProperty("完成数")
    Long finishedCount;
    @ApiModelProperty("未完成数")
    Long unFinishedCount;

}
