package com.persee.workstation.uv_inexus.dto.guide;

import lombok.Data;
import javax.validation.constraints.NotEmpty;

/**
 * 项目导入请求DTO
 */
@Data
public class ProjectImportDto {
    // 导入负责人（当前登录用户）
    @NotEmpty(message = "导入负责人不能为空")
    private String updateBy;
    
    // 是否覆盖已存在的项目
    private Boolean overwrite = false;
}