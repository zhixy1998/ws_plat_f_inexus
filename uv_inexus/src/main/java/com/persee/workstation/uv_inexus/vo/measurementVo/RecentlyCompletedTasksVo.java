package com.persee.workstation.uv_inexus.vo.measurementVo;

import lombok.Data;

@Data
public class RecentlyCompletedTasksVo {
    // 任务名称
    private String taskName;
    // 任务id
    private String taskId;
    // 方法id
    private String methodId;
    // 方法名称
    private String methodName;
    // 项目名称
    private String projectName;
    // 测量样品
    private String measurementSample;
    // 测量开始时间
    private String measurementStartTime;
    // 测量结束时间
    private String measurementEndTime;
}
