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

package com.persee.workstation.uv_inexus.po.guide.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 样品模板表头实体类
 */
@Data
@TableName("t_measure_sample_head")
public class TMeasureSampleHead {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id; // 主键id

    @TableField("template_name")
    private String templateName; // 样品模板名称

    @TableField("template_desc")
    private String templateDesc; // 样品模板描述

    @TableField("measure_type")
    private String measureType; // 测量类型

    @TableField("heads")
    private String heads; // 表头信息(JSON)

    @TableField("create_time")
    private LocalDateTime createTime; // 创建时间

    @TableField("update_time")
    private LocalDateTime updateTime; // 修改时间

    @TableField("create_user")
    private String createUser; // 创建者

    @TableField("update_user")
    private String updateUser; // 修改者
}
