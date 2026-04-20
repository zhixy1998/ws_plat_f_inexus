package com.persee.workstation.uv_inexus.dto.concise.common;

import lombok.Data;

@Data
public class TaskDataNumDto {
    // 日份
    private String daily;
    // 待完成
    private Long taskNum;
    // 已完成任务数
    private Long completeNum;
}
