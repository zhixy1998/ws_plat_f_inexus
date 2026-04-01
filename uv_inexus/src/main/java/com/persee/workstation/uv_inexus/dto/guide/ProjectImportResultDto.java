package com.persee.workstation.uv_inexus.dto.guide;

import lombok.Data;
import java.util.List;

/**
 * 项目导入结果DTO
 */
@Data
public class ProjectImportResultDto {
    private Integer successCount;      // 成功导入数量
    private Integer failCount;         // 失败数量
    private Integer skipCount;         // 跳过数量
    private List<String> successIds;   // 成功导入的项目ID
    private List<String> failedNames;  // 导入失败的项目名称
    private List<String> skippedNames; // 跳过的项目名称
}