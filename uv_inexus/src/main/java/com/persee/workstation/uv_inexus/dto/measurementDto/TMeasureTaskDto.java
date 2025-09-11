package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.enums.measurementEnums.TaskEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TMeasureTaskDto {

    private String taskName;
    private String status;
    private String methodName;
    private String projectName;
    private Integer sampleNumber;
    private LocalDateTime startTime;



}
