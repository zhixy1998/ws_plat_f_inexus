package com.persee.workstation.uv_inexus.dto.concise.common;

import lombok.Data;

@Data
public class ProjectDataNumDto {
    // 项目名称
    private String projectName;
    // 任务数量
    private Long taskNum;
    // 方法数量
    private Long methodNum;
}
