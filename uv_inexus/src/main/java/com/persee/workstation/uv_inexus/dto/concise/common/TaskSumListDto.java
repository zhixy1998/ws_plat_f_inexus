package com.persee.workstation.uv_inexus.dto.concise.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskSumListDto {
    // 任务ID
    private String taskId;
    // 方法名称
    private String methodName;
    // 所在项目
    private String projectName;
    // 测量模式
    private String measureMode;
    // 任务状态
    private String taskStatus;
    // 样品个数
    private Long sampleNum;
    // 测量起始时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime measureStartTime;
}
