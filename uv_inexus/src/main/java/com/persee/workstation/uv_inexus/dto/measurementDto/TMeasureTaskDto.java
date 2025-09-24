package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TMeasureTaskDto {
    @ApiModelProperty("任务id")
    private String id;

    @ApiModelProperty("测量类型")
    private String measureType;
    @ApiModelProperty("任务名称")
    private String taskName;
    @ApiModelProperty("任务状态")

    private String status;
    @ApiModelProperty("所属方法名")

    private String methodName;
    @ApiModelProperty("所属方法id")

    private String methodId;
    @ApiModelProperty("所属项目id")

    private String projectId;
    @ApiModelProperty("所属项目名")

    private String projectName;
    @ApiModelProperty("样品数量")

    private Integer sampleNumber;
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("创建时间--未完成任务")

    private LocalDateTime createTime;
    @ApiModelProperty("开始时间--已完成任务")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")

    private LocalDateTime startTime;
    @ApiModelProperty("结束时间--已完成任务")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")

    private LocalDateTime endTime;


}
