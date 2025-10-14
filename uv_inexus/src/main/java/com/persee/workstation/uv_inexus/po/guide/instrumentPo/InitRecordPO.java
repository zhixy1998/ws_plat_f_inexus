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
@TableName("t_ins_init_temp")
public class InitRecordPO extends BasePO {

    @ApiModelProperty("仪器信息id")
    @TableField("device_info_id")
    private String deviceInfoId;

    @ApiModelProperty("序列号")
    @TableField("device_serial_number")
    private String deviceSerialNumber;

    @ApiModelProperty("初始化条目")
    @TableField("init_task_num")
    private Integer initTaskNum;

    @ApiModelProperty("初始化状态,0-进行中，1-成功，2-失败")
    @TableField("init_status")
    private Integer initStatus;
}
