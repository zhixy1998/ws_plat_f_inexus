/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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
