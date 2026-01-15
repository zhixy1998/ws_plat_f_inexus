package com.persee.workstation.uv_inexus.dto.concise.common;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MethodSumListDto {
    // 方法id
    private String methodId;
    // 方法名称
    private String methodName;
    // 创建人
    private String createBy;
    // 创建时间
    private LocalDateTime createTime;
    // 所属项目
    private String projectName;
    // 最近使用时间
    private LocalDateTime lastUseTime;
}
