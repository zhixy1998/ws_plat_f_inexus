package com.persee.workstation.uv_inexus.vo.measurementVo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecentlyAccomplishTaskVo {
    private String measureId;
    private String taskId;
    private String taskName;
    private String methodId;
    private String methodName;
    private String projectId;
    private String projectName;
    private String sampleCount;
    private LocalDateTime measurementStartTime;
    private LocalDateTime measurementEndTime;
}
