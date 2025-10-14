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

package com.persee.workstation.uv_inexus.po.concise.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 草稿数据表实体类
 */
@Data
@TableName("f_t_measure_draft")
public class FMeasureDraft {

    @TableId(value = "id",type = IdType.ASSIGN_UUID) // 主键字段
    private String id;          // 草稿ID

    @TableField("draft_name")
    private String draftName;   // 草稿名称

    @TableField("method_id")
    private String methodId;    // 方法ID

}
