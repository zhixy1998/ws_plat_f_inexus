package com.persee.workstation.uv_inexus.dto.instrumentDto;


import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DeviceHistoryTaskDTO {

    @ApiModelProperty(value = "页码", example = "1")
    private Integer pageNum = 1;

    @ApiModelProperty(value = "每页数量", example = "10")
    private Integer pageSize = 20;

    @ApiModelProperty("仪器编号")
    private String deviceSerialNumber;

    @ApiModelProperty("搜索条件")
    private String search;

    @ApiModelProperty("开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm:ss.SSS'Z']", timezone = "GMT+8")
    private LocalDate startTime;

    @ApiModelProperty("结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm:ss.SSS'Z']", timezone = "GMT+8")
    private LocalDate endTime;

    //sql
    @ApiModelProperty(hidden = true)
    private LocalDateTime startDateTime;
    @ApiModelProperty(hidden = true)
    private LocalDateTime endDateTime;
}
