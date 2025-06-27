package com.persee.workstation.uv_inexus.vo.measurementVo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectStatisticsVo {
    private String projectId;
    private String projectName;
    private String creator;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private String methodCount;
    private String taskCount;
}
