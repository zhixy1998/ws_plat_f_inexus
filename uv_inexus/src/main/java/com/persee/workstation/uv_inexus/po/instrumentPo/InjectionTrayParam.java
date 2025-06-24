package com.persee.workstation.uv_inexus.po.instrumentPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "t_int_injection_tray_param")
@ApiModel("进样盘参数设置")
public class InjectionTrayParam extends BasePO {

    @TableField(value = "device_serial_number")
    @ApiModelProperty("序列号")
    private String deviceSerialNumber;

    @TableField(value = "injection_id")
    @ApiModelProperty("进样器配置id")
    private String injectionId;

    @ApiModelProperty("盘区设置")
    @TableField(value = "panel")
    private Integer panel;

    @ApiModelProperty("盘架设定")
    @TableField(value = "plate_rail")
    private Integer plateRail;

    @ApiModelProperty("编号信息")
    @TableField(value = "num_info")
    private String numInfo;


}
