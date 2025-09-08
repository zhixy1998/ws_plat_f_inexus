package com.persee.workstation.uv_inexus.po.guide.systemManagePo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("系统面板数据类")
@TableName(value = "t_system_panel_conf")
public class SystemPanelConfPO extends BasePO {

    @ApiModelProperty("方法列表面板开关")
    @TableField(value = "method_list_switch")
    private Boolean methodListSwitch;

    @ApiModelProperty("项目统计面板开关")
    @TableField(value = "project_statistics_switch")
    private Boolean projectStatisticsSwitch;
}
