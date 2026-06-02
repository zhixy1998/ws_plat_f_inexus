package com.persee.workstation.uv_inexus.vo.factory_inspection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("提醒开关配置VO")
public class ReminderSwitchConfigVo {

    @ApiModelProperty("配置ID")
    private String id;

    @ApiModelProperty("提醒类型0总开关，1期间核查提醒，2标准物效期提醒")
    private Integer reminderType;

    @ApiModelProperty("提醒名称")
    private String reminderName;

    @ApiModelProperty("是否启用：0-关闭，1-开启")
    private Integer enabled;
    @ApiModelProperty("是否启用（布尔值）")
    private Boolean isEnabled;
    @ApiModelProperty("排序号")
    private Integer sortOrder;
    @ApiModelProperty("通知模式：0-正常提醒，1-仅通知不提醒")
    private Integer notificationMode;

    @ApiModelProperty("描述说明")
    private String description;

    @ApiModelProperty("创建时间")
    private String createTime;

    @ApiModelProperty("更新时间")
    private String updateTime;

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
        this.isEnabled = enabled != null && enabled == 1;
    }
}
