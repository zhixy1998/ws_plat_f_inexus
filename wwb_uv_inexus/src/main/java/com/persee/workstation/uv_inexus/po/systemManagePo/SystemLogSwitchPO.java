package com.persee.workstation.uv_inexus.po.systemManagePo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_system_log_switch")
public class SystemLogSwitchPO extends BasePO {

    @ApiModelProperty("设备使用统计总开关")
    @TableField(value = "dev_usage_total_switch")
    private Boolean statUsageTotalSwitch;

    @ApiModelProperty("仪器使用率统计开关")
    @TableField("stat_usage_enabled")
    private Boolean statUsageEnabled;

    @ApiModelProperty("用户使用率统计开关")
    @TableField("stat_user_enabled")
    private Boolean statUserEnabled;

    @ApiModelProperty("检测类型分项统计开关")
    @TableField("stat_type_enabled")
    private Boolean statTypeEnabled;

    @ApiModelProperty("检测项目分项统计开关")
    @TableField("stat_item_enabled")
    private Boolean statItemEnabled;

    @ApiModelProperty("检测任务详细信息开关")
    @TableField("stat_detail_enabled")
    private Boolean statDetailEnabled;

    @ApiModelProperty("仪器使用记录开关")
    @TableField("dev_usage_record_switch")
    private Boolean statUsageRecordEnabled;

}
