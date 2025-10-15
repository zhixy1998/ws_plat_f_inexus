/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.po.guide.instrumentPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName("t_ins_info_temp")
public class DeviceInfoPO extends BasePO {

    @ApiModelProperty("机器ip")
    @TableField("device_ip")
    private String ip;

    @ApiModelProperty("仪器名称")
    @TableField("device_name")
    private String deviceName;

    @ApiModelProperty("仪器型号")
    @TableField("device_model")
    private String deviceModel;

    @ApiModelProperty("仪器型号名称")
    @TableField("device_model_name")
    private String deviceModelName;

    @ApiModelProperty("连接仪器的ip")
    @TableField("machine_ip")
    private String machineIp;

    @ApiModelProperty("初始化状态, 0：未完成；1：自检成功； 2：自检失败； 3：自检进行中")
    @TableField("is_inited")
    private Integer isInited;

    @ApiModelProperty("是否开机,0-未开机,1-已开机,2-软关机")
    @TableField("is_dev_opened")
    private Integer isDevOpened;

    @ApiModelProperty("联机状态,0-未连接，1-联机中，2-联机成功，3-联机失败")
    @TableField("connect_status")
    private Integer connectStatus;

    @ApiModelProperty("仪器当前状态")
    @TableField("device_current_status")
    private Integer deviceCurrentStatus;

    @ApiModelProperty("序列号")
    @TableField("device_serial_number")
    private String deviceSerialNumber;

    @ApiModelProperty("端口号")
    @TableField("device_port")
    private Integer devicePort;

    @ApiModelProperty("附件类型")
    @TableField("attachment_type")
    private Integer attachmentType;

    @ApiModelProperty("附件名称")
    @TableField("attachment_name")
    private String attachmentName;
}
