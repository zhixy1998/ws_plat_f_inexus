package com.persee.workstation.uv_inexus.dto.systemManageDto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("提醒开关配置DTO")
public class ReminderSwitchConfigDto {

    @ApiModelProperty(value = "配置ID")
    private String id;

    @ApiModelProperty(value = "提醒类型：0-总开关，1-检定提醒，2-校准提醒，3-期间核查提醒，4-标准物质效期提醒", required = true)
    @NotNull(message = "提醒类型不能为空")
    private Integer reminderType;

    @ApiModelProperty(value = "提醒名称")
    private String reminderName;

    @ApiModelProperty(value = "是否启用：0-关闭，1-开启")
    @Min(0)
    @Max(1)
    private Integer enabled;

    @ApiModelProperty(value = "排序号")
    private Integer sortOrder;

    @ApiModelProperty(value = "描述说明")
    private String description;
}
