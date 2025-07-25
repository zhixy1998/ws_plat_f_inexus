package com.persee.workstation.uv_inexus.vo.measurementVo;

import lombok.Data;

@Data
public class ProjectStatisticsVo {
    private String projectId;
    private String projectName;
    private String creator;
    private String createTime;
    private String updateBy;
    private String updateTime;
    private String methodCount;
    private String taskCount;
}
