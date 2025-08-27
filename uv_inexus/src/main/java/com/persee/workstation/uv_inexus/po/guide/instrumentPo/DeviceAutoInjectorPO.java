package com.persee.workstation.uv_inexus.po.guide.instrumentPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("自动进样器配置")
@TableName("t_ins_auto_injector")
public class DeviceAutoInjectorPO extends BasePO {

    @ApiModelProperty(value = "仪器序列号",required = true)
    @TableField("device_serial_number")
    private String deviceSerialNumber;

    @ApiModelProperty("通信端口配置")
    @TableField("message_port")
    private Integer messagePort;

    @ApiModelProperty("连接状态，0-未连接，1-已连接")
    @TableField("connect_status")
    private Integer connectStatus;

    @ApiModelProperty("是否开机自动对进样器初始化， 0-否，1-是")
    @TableField("is_start_init")
    private Integer isStartInit;

    @ApiModelProperty("进样速度")
    @TableField("enter_speed")
    private Integer enterSpeed;

    @ApiModelProperty("进样时间")
    @TableField("enter_time")
    private String enterTime;

    @ApiModelProperty("清洗速度")
    @TableField("wash_speed")
    private Integer washSpeed;

    @ApiModelProperty("清洗时间")
    @TableField("wash_time")
    private String washTime;

    @ApiModelProperty("停泵等待时间")
    @TableField("waiting_time")
    private String waitingTime;

    @ApiModelProperty("进样之前是否清洗，0-否，1-是")
    @TableField("is_wash_before_enter")
    private Integer isWashBeforeEnter;

    @ApiModelProperty("进样盘参数列表")
    @TableField(value = "param_list",exist = false)
    List<InjectionTrayParamPO> paramList;

}
