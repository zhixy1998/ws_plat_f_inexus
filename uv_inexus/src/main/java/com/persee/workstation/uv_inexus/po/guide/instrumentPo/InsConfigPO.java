package com.persee.workstation.uv_inexus.po.guide.instrumentPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName(value = "t_ins_config_setting")
public class InsConfigPO extends BasePO {

    @TableField("device_serial_number")
    @ApiModelProperty("序列号")
    private String deviceSerialNumber;

    @TableField("is_time_startup")
    @ApiModelProperty(value = "是否定时开机",example = "0-否，1-是")
    private Integer isTimeStartUp;

    @TableField("startup_time")
    @ApiModelProperty("定时开机时间")
    private String startUpTime;

    @TableField("message_port")
    @ApiModelProperty("通讯端口")
    private Integer messagePort;

    @TableField("annex_setting")
    @ApiModelProperty("附件设置")
    private Integer annexSetting;

    @TableField("band_width")
    @ApiModelProperty("光谱带宽设置")
    private Double bandWidth;

//    @TableField(value = "restoreFactorySetting",exist = false)
//    @ApiModelProperty("是否恢复出厂设置")
//    private Boolean restoreFactorySetting;
}
