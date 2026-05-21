package com.persee.workstation.uv_inexus.po.factory_inspection;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("提醒功能开关配置")
@TableName("t_reminder_switch_config")
public class ReminderSwitchConfigPO extends BasePO {

    @ApiModelProperty("提醒类型：0-总开关，1-检定提醒，2-校准提醒，3-期间核查提醒，4-标准物质效期提醒")
    @TableField("reminder_type")
    private Integer reminderType;

    @ApiModelProperty("提醒名称")
    @TableField("reminder_name")
    private String reminderName;

    @ApiModelProperty("是否启用：0-关闭，1-开启")
    @TableField("enabled")
    private Integer enabled;

    @ApiModelProperty("排序号")
    @TableField("sort_order")
    private Integer sortOrder;

    @ApiModelProperty("描述说明")
    @TableField("description")
    private String description;
}
