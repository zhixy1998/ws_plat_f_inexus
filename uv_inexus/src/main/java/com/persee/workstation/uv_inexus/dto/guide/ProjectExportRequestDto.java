package com.persee.workstation.uv_inexus.dto.guide;

import lombok.Data;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 项目导出请求DTO
 */
@Data
public class ProjectExportRequestDto {
    @NotEmpty(message = "项目ID列表不能为空")
    private List<String> projectIds;
}