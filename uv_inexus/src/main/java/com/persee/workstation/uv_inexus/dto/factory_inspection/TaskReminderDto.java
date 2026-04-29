package com.persee.workstation.uv_inexus.dto.factory_inspection;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Date;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Data
public class TaskReminderDto {
    @ApiModelProperty(value = "任务ID", required = true)
    private String taskId;

    @ApiModelProperty(value = "是否启用提醒：0-否，1-是")
    @Min(0)
    @Max(1)
    private Integer reminderEnabled;

    @ApiModelProperty(value = "提醒间隔天数")
    private Integer reminderIntervalDays;

    @ApiModelProperty(value = "首次提醒日期")
    private Date firstReminderDate;
}
