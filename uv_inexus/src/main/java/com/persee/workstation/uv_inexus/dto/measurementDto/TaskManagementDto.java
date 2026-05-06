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
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskManagementDto {
    @ApiModelProperty("任务id")
    private String id;
    @ApiModelProperty("任务名称")
    private String taskName;
    @ApiModelProperty("所在项目")
    private String projectName;
    @ApiModelProperty("起始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;
    @ApiModelProperty("样品个数")
    private Integer sampleKey;
    @ApiModelProperty("方法id")
    private String methodId;
    @ApiModelProperty("方法名称")
    private String methodName;
    @ApiModelProperty("测量模式")
    private String measureType;


}
