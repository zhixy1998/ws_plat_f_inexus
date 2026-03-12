package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 删除任务的请求DTO
 * 适配@RequestParam转对象的场景
 */
@Data
public class PermLogOccupyDTO {
    @ApiModelProperty("日志占位字段")
    private String operateTypeExt;

    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("方法id")
    private String methodId;

    @NotBlank(message = "operateType不能为空")
    private String operateType;

}