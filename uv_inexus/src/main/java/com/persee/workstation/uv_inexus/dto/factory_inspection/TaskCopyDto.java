package com.persee.workstation.uv_inexus.dto.factory_inspection;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Size;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Data
public class TaskCopyDto {
    @ApiModelProperty("新任务名称")
    @Size(max = 32, message = "任务名称长度不能超过32")
    private String newTaskName;
    @ApiModelProperty("当前任务id")
    private String currentTaskId;
    @ApiModelProperty("仪器序列号")
    private String serialNumber;
    private String updateBy;
}
