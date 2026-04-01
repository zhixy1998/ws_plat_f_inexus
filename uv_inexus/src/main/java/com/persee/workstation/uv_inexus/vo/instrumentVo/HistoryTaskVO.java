package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HistoryTaskVO {

    @ApiModelProperty("任务ID")
    private String taskId;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("用户名")
    private String updateBy;

    @ApiModelProperty("检测项目")
    private String projectName;

    @ApiModelProperty("检测方法")
    private String methodName;

    @ApiModelProperty("仪器编号")
    private String deviceNumber;

    @ApiModelProperty("测量类型")
    private String measureType;

    @ApiModelProperty("任务状态 (0:未开始, 1:进行中, 2:已结束)")
    private String taskStatus;

    @ApiModelProperty("格式化后的开始时间")
    private String startTimeDisplay;

    @ApiModelProperty("格式化后的结束时间")
    private String endTimeDisplay;

    @ApiModelProperty("开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("结束时间")
    private LocalDateTime endTime;

}
