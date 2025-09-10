package com.persee.workstation.uv_inexus.po.concise.measurementPo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class TaskCount {
    @ApiModelProperty("最近一周")
    private TaskCountByTime lastWeek;
    @ApiModelProperty("最近一月")
    private TaskCountByTime lastMonth;
    @ApiModelProperty("半年")
    private TaskCountByTime halfYear;
    @ApiModelProperty("最近一年")
    private TaskCountByTime lastYear;
    @ApiModelProperty("最近三年")
    private TaskCountByTime threeYears;
    @ApiModelProperty("所有")
    private TaskCountByTime all;


}
